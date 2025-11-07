import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { isManagerOrAdmin } from '@/utils/auth';
import AdminDashboard from './AdminDashboard';
import CustomerDashboard from './CustomerDashboard';

export default function Dashboard() {
  const navigate = useNavigate();
  const hasAdminAccess = isManagerOrAdmin();

  useEffect(() => {
    // Verify authentication
    const isAuthenticated = localStorage.getItem('isAuthenticated');
    if (!isAuthenticated) {
      navigate('/login');
    }
  }, [navigate]);

  // Route to appropriate dashboard based on role
  return hasAdminAccess ? <AdminDashboard /> : <CustomerDashboard />;
}
