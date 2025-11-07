import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Wallet, TrendingUp, Calendar, Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface CustomerStats {
  totalAccounts: number;
  totalInvestment: number;
  upcomingMaturity: number;
  activeNotifications: number;
}

export default function CustomerDashboard() {
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Customer';
  const [stats, setStats] = useState<CustomerStats>({
    totalAccounts: 0,
    totalInvestment: 0,
    upcomingMaturity: 0,
    activeNotifications: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchCustomerStats();
  }, []);

  const fetchCustomerStats = async () => {
    try {
      // TODO: Fetch customer-specific stats from API
      // For now, showing placeholder data
      setStats({
        totalAccounts: 0,
        totalInvestment: 0,
        upcomingMaturity: 0,
        activeNotifications: 0,
      });
    } catch (error) {
      console.error('Error fetching customer stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const statCards = [
    {
      title: 'My FD Accounts',
      value: stats.totalAccounts,
      icon: Wallet,
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Total Investment',
      value: `₹${stats.totalInvestment.toLocaleString()}`,
      icon: TrendingUp,
      color: 'text-green-600',
      bgColor: 'bg-green-100',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Maturing Soon',
      value: stats.upcomingMaturity,
      icon: Calendar,
      color: 'text-orange-600',
      bgColor: 'bg-orange-100',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Notifications',
      value: stats.activeNotifications,
      icon: Bell,
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
      onClick: () => {},
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-gray-600">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="p-8 space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Welcome, {username}!</h1>
        <p className="text-gray-600 mt-2">Manage your Fixed Deposits and track your investments</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((stat) => {
          const Icon = stat.icon;
          return (
            <Card 
              key={stat.title} 
              className="cursor-pointer hover:shadow-lg transition-shadow"
              onClick={stat.onClick}
            >
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
                <div className={`p-2 rounded-lg ${stat.bgColor}`}>
                  <Icon className={`h-4 w-4 ${stat.color}`} />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{stat.value}</div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>My Recent FD Accounts</CardTitle>
            <CardDescription>Your latest fixed deposit accounts</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-center py-8 text-gray-500">
              No FD accounts yet. Create your first FD account!
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
            <CardDescription>Common tasks and operations</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <button
                onClick={() => navigate('/my-360-view')}
                className="w-full text-left p-4 bg-indigo-50 hover:bg-indigo-100 rounded-lg transition-colors"
              >
                <h3 className="font-semibold text-indigo-900">My 360° View</h3>
                <p className="text-sm text-indigo-700">Complete profile overview</p>
              </button>
              <button
                onClick={() => navigate('/calculator')}
                className="w-full text-left p-4 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors"
              >
                <h3 className="font-semibold text-blue-900">Calculate FD Returns</h3>
                <p className="text-sm text-blue-700">Use our FD calculator</p>
              </button>
              <button
                onClick={() => navigate('/products')}
                className="w-full text-left p-4 bg-green-50 hover:bg-green-100 rounded-lg transition-colors"
              >
                <h3 className="font-semibold text-green-900">Browse Products</h3>
                <p className="text-sm text-green-700">Explore FD products</p>
              </button>
              <button
                onClick={() => navigate('/profile')}
                className="w-full text-left p-4 bg-purple-50 hover:bg-purple-100 rounded-lg transition-colors"
              >
                <h3 className="font-semibold text-purple-900">Update Profile</h3>
                <p className="text-sm text-purple-700">Manage your information</p>
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
