import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Users, Package, Wallet, TrendingUp, AlertCircle, Activity } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface AdminStats {
  totalCustomers: number;
  totalProducts: number;
  totalAccounts: number;
  totalValue: number;
  pendingApprovals: number;
  recentActivity: number;
}

export default function AdminDashboard() {
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Admin';
  const [stats, setStats] = useState<AdminStats>({
    totalCustomers: 0,
    totalProducts: 0,
    totalAccounts: 0,
    totalValue: 0,
    pendingApprovals: 0,
    recentActivity: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAdminStats();
  }, []);

  const fetchAdminStats = async () => {
    try {
      // TODO: Fetch admin stats from APIs
      // For now, showing placeholder data
      setStats({
        totalCustomers: 0,
        totalProducts: 0,
        totalAccounts: 0,
        totalValue: 0,
        pendingApprovals: 0,
        recentActivity: 0,
      });
    } catch (error) {
      console.error('Error fetching admin stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const statCards = [
    {
      title: 'Total Customers',
      value: stats.totalCustomers,
      icon: Users,
      color: 'text-blue-600 dark:text-blue-400',
      bgColor: 'bg-blue-100 dark:bg-blue-950',
      onClick: () => navigate('/customers'),
    },
    {
      title: 'Active Products',
      value: stats.totalProducts,
      icon: Package,
      color: 'text-green-600 dark:text-green-400',
      bgColor: 'bg-green-100 dark:bg-green-950',
      onClick: () => navigate('/products'),
    },
    {
      title: 'FD Accounts',
      value: stats.totalAccounts,
      icon: Wallet,
      color: 'text-purple-600 dark:text-purple-400',
      bgColor: 'bg-purple-100 dark:bg-purple-950',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Total Value',
      value: `₹${stats.totalValue.toLocaleString()}`,
      icon: TrendingUp,
      color: 'text-orange-600 dark:text-orange-400',
      bgColor: 'bg-orange-100 dark:bg-orange-950',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Pending Approvals',
      value: stats.pendingApprovals,
      icon: AlertCircle,
      color: 'text-red-600 dark:text-red-400',
      bgColor: 'bg-red-100 dark:bg-red-950',
      onClick: () => {},
    },
    {
      title: 'Recent Activity',
      value: stats.recentActivity,
      icon: Activity,
      color: 'text-indigo-600 dark:text-indigo-400',
      bgColor: 'bg-indigo-100 dark:bg-indigo-950',
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
        <h1 className="text-3xl font-bold text-foreground">Admin Dashboard</h1>
        <p className="text-muted-foreground mt-2">Welcome, {username} - Manage the entire Fixed Deposit system</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
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
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>Latest transactions and account updates</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-center py-8 text-muted-foreground">
              No recent activity
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Upcoming Maturities</CardTitle>
            <CardDescription>FD accounts maturing in the next 30 days</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-center py-8 text-muted-foreground">
              No upcoming maturities
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
            <CardDescription>Common administrative tasks</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <button
                onClick={() => navigate('/customers')}
                className="w-full text-left p-3 bg-blue-50 dark:bg-blue-950 hover:bg-blue-100 dark:hover:bg-blue-900 rounded-lg transition-colors border border-blue-200 dark:border-blue-800"
              >
                <h3 className="font-semibold text-blue-900 dark:text-blue-100">Manage Customers</h3>
                <p className="text-sm text-blue-700 dark:text-blue-300">View and edit customer data</p>
              </button>
              <button
                onClick={() => navigate('/products')}
                className="w-full text-left p-3 bg-green-50 dark:bg-green-950 hover:bg-green-100 dark:hover:bg-green-900 rounded-lg transition-colors border border-green-200 dark:border-green-800"
              >
                <h3 className="font-semibold text-green-900 dark:text-green-100">Manage Products</h3>
                <p className="text-sm text-green-700 dark:text-green-300">Configure FD products</p>
              </button>
              <button
                onClick={() => navigate('/accounts')}
                className="w-full text-left p-3 bg-purple-50 dark:bg-purple-950 hover:bg-purple-100 dark:hover:bg-purple-900 rounded-lg transition-colors border border-purple-200 dark:border-purple-800"
              >
                <h3 className="font-semibold text-purple-900 dark:text-purple-100">View All Accounts</h3>
                <p className="text-sm text-purple-700 dark:text-purple-300">Monitor FD accounts</p>
              </button>
            </div>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>System Overview</CardTitle>
            <CardDescription>Key system metrics and status</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <span className="text-sm font-medium text-foreground">Customer Growth (30 days)</span>
                <span className="text-lg font-bold text-green-600">+0%</span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <span className="text-sm font-medium text-foreground">Total Deposits (30 days)</span>
                <span className="text-lg font-bold text-blue-600">₹0</span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <span className="text-sm font-medium text-foreground">Active FD Accounts</span>
                <span className="text-lg font-bold text-purple-600">0</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

