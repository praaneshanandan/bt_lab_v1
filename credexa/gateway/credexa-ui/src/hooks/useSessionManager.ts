import { useEffect, useState, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '@/services/api';
import { toast } from 'sonner';
import { isTokenExpired } from '@/utils/auth';

const IDLE_TIMEOUT = 5 * 60 * 1000; // 5 minutes in milliseconds
const WARNING_TIME = 30 * 1000; // Show warning 30 seconds before logout
const CHECK_INTERVAL = 1000; // Check every second

export const useSessionManager = () => {
  const navigate = useNavigate();
  const [showWarning, setShowWarning] = useState(false);
  const [secondsRemaining, setSecondsRemaining] = useState(30);
  const lastActivityRef = useRef<number>(Date.now());
  const warningTimeoutRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const logoutTimeoutRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const checkIntervalRef = useRef<ReturnType<typeof setInterval> | undefined>(undefined);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('isAuthenticated');
      localStorage.removeItem('authToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('userRoles');
      toast.info('You have been logged out due to inactivity');
      navigate('/login');
    }
  }, [navigate]);

  const resetTimers = useCallback(() => {
    lastActivityRef.current = Date.now();
    setShowWarning(false);
    
    // Clear existing timers
    if (warningTimeoutRef.current) clearTimeout(warningTimeoutRef.current);
    if (logoutTimeoutRef.current) clearTimeout(logoutTimeoutRef.current);

    // Set warning timer
    warningTimeoutRef.current = setTimeout(() => {
      setShowWarning(true);
      setSecondsRemaining(30);
    }, IDLE_TIMEOUT - WARNING_TIME);

    // Set logout timer
    logoutTimeoutRef.current = setTimeout(() => {
      logout();
    }, IDLE_TIMEOUT);
  }, [logout]);

  const handleActivity = useCallback(() => {
    // Check if token is expired first
    if (isTokenExpired()) {
      logout();
      return;
    }
    
    lastActivityRef.current = Date.now();
    if (showWarning) {
      resetTimers();
    }
  }, [showWarning, resetTimers, logout]);

  const continueSession = useCallback(() => {
    resetTimers();
    toast.success('Session extended');
  }, [resetTimers]);

  useEffect(() => {
    // Check if user is authenticated
    const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
    if (!isAuthenticated) return;

    // Check token expiration on mount
    if (isTokenExpired()) {
      logout();
      return;
    }

    // Initialize timers
    resetTimers();

    // Activity event listeners
    const events = ['mousedown', 'keydown', 'scroll', 'touchstart', 'click'];
    events.forEach(event => {
      document.addEventListener(event, handleActivity);
    });

    // Check remaining time every second when warning is shown
    checkIntervalRef.current = setInterval(() => {
      if (showWarning) {
        const elapsed = Date.now() - lastActivityRef.current;
        const remaining = Math.max(0, Math.ceil((IDLE_TIMEOUT - elapsed) / 1000));
        setSecondsRemaining(remaining);
        
        if (remaining === 0) {
          logout();
        }
      }
    }, CHECK_INTERVAL);

    // Cleanup
    return () => {
      events.forEach(event => {
        document.removeEventListener(event, handleActivity);
      });
      if (warningTimeoutRef.current) clearTimeout(warningTimeoutRef.current);
      if (logoutTimeoutRef.current) clearTimeout(logoutTimeoutRef.current);
      if (checkIntervalRef.current) clearInterval(checkIntervalRef.current);
    };
  }, [handleActivity, resetTimers, showWarning, logout]);

  return { showWarning, secondsRemaining, continueSession, logout };
};
