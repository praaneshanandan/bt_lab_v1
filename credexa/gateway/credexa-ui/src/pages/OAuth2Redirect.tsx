import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';

/**
 * OAuth2 Redirect Handler
 * Receives token and user info from OAuth2 provider and stores in localStorage
 */
export default function OAuth2Redirect() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    const userId = searchParams.get('userId');
    const username = searchParams.get('username');
    const role = searchParams.get('role');
    const email = searchParams.get('email');

    if (token && userId && username && role) {
      // Store authentication data
      localStorage.setItem('token', token);
      localStorage.setItem('userId', userId);
      localStorage.setItem('username', username);
      localStorage.setItem('role', role);
      if (email) {
        localStorage.setItem('email', email);
      }

      toast.success(`Welcome ${username}! You've been logged in via Google.`);
      
      // Redirect to dashboard
      navigate('/', { replace: true });
    } else {
      toast.error('OAuth2 login failed. Missing required parameters.');
      navigate('/login', { replace: true });
    }
  }, [searchParams, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <Loader2 className="h-12 w-12 animate-spin text-blue-600 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-gray-900 mb-2">
          Completing your login...
        </h2>
        <p className="text-gray-600">
          Please wait while we set up your session
        </p>
      </div>
    </div>
  );
}
