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
      color: 'text-purple-600 dark:text-purple-400',
      bgColor: 'bg-purple-100 dark:bg-purple-950',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Total Investment',
      value: `₹${stats.totalInvestment.toLocaleString()}`,
      icon: TrendingUp,
      color: 'text-green-600 dark:text-green-400',
      bgColor: 'bg-green-100 dark:bg-green-950',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Maturing Soon',
      value: stats.upcomingMaturity,
      icon: Calendar,
      color: 'text-orange-600 dark:text-orange-400',
      bgColor: 'bg-orange-100 dark:bg-orange-950',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Notifications',
      value: stats.activeNotifications,
      icon: Bell,
      color: 'text-blue-600 dark:text-blue-400',
      bgColor: 'bg-blue-100 dark:bg-blue-950',
      onClick: () => {},
    },
  ];

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-lg text-muted-foreground">Loading dashboard...</div>
      </div>
    );
  }

  return (
    <div className="p-8 space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-foreground">Welcome, {username}!</h1>
        <p className="text-muted-foreground mt-2">Manage your Fixed Deposits and track your investments</p>
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
                <CardTitle className="text-sm font-medium text-foreground">{stat.title}</CardTitle>
                <div className={`p-2 rounded-lg ${stat.bgColor}`}>
                  <Icon className={`h-4 w-4 ${stat.color}`} />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold text-foreground">{stat.value}</div>
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
            <div className="text-center py-8 text-muted-foreground">
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
                className="w-full text-left p-4 bg-indigo-50 dark:bg-indigo-950 hover:bg-indigo-100 dark:hover:bg-indigo-900 rounded-lg transition-colors border border-indigo-200 dark:border-indigo-800"
              >
                <h3 className="font-semibold text-indigo-900 dark:text-indigo-100">My 360° View</h3>
                <p className="text-sm text-indigo-700 dark:text-indigo-300">Complete profile overview</p>
              </button>
              <button
                onClick={() => navigate('/calculator')}
                className="w-full text-left p-4 bg-blue-50 dark:bg-blue-950 hover:bg-blue-100 dark:hover:bg-blue-900 rounded-lg transition-colors border border-blue-200 dark:border-blue-800"
              >
                <h3 className="font-semibold text-blue-900 dark:text-blue-100">Calculate FD Returns</h3>
                <p className="text-sm text-blue-700 dark:text-blue-300">Use our FD calculator</p>
              </button>
              <button
                onClick={() => navigate('/products')}
                className="w-full text-left p-4 bg-green-50 dark:bg-green-950 hover:bg-green-100 dark:hover:bg-green-900 rounded-lg transition-colors border border-green-200 dark:border-green-800"
              >
                <h3 className="font-semibold text-green-900 dark:text-green-100">Browse Products</h3>
                <p className="text-sm text-green-700 dark:text-green-300">Explore FD products</p>
              </button>
              <button
                onClick={() => navigate('/profile')}
                className="w-full text-left p-4 bg-purple-50 dark:bg-purple-950 hover:bg-purple-100 dark:hover:bg-purple-900 rounded-lg transition-colors border border-purple-200 dark:border-purple-800"
              >
                <h3 className="font-semibold text-purple-900 dark:text-purple-100">Update Profile</h3>
                <p className="text-sm text-purple-700 dark:text-purple-300">Manage your information</p>
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

