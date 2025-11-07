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
      color: 'text-blue-600',
      bgColor: 'bg-blue-100',
      onClick: () => navigate('/customers'),
    },
    {
      title: 'Active Products',
      value: stats.totalProducts,
      icon: Package,
      color: 'text-green-600',
      bgColor: 'bg-green-100',
      onClick: () => navigate('/products'),
    },
    {
      title: 'FD Accounts',
      value: stats.totalAccounts,
      icon: Wallet,
      color: 'text-purple-600',
      bgColor: 'bg-purple-100',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Total Value',
      value: `₹${stats.totalValue.toLocaleString()}`,
      icon: TrendingUp,
      color: 'text-orange-600',
      bgColor: 'bg-orange-100',
      onClick: () => navigate('/accounts'),
    },
    {
      title: 'Pending Approvals',
      value: stats.pendingApprovals,
      icon: AlertCircle,
      color: 'text-red-600',
      bgColor: 'bg-red-100',
      onClick: () => {},
    },
    {
      title: 'Recent Activity',
      value: stats.recentActivity,
      icon: Activity,
      color: 'text-indigo-600',
      bgColor: 'bg-indigo-100',
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
        <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
        <p className="text-gray-600 mt-2">Welcome, {username} - Manage the entire Fixed Deposit system</p>
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
            <CardTitle>Recent Activity</CardTitle>
            <CardDescription>Latest transactions and account updates</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-center py-8 text-gray-500">
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
            <div className="text-center py-8 text-gray-500">
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
                className="w-full text-left p-3 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors"
              >
                <h3 className="font-semibold text-blue-900">Manage Customers</h3>
                <p className="text-sm text-blue-700">View and edit customer data</p>
              </button>
              <button
                onClick={() => navigate('/products')}
                className="w-full text-left p-3 bg-green-50 hover:bg-green-100 rounded-lg transition-colors"
              >
                <h3 className="font-semibold text-green-900">Manage Products</h3>
                <p className="text-sm text-green-700">Configure FD products</p>
              </button>
              <button
                onClick={() => navigate('/accounts')}
                className="w-full text-left p-3 bg-purple-50 hover:bg-purple-100 rounded-lg transition-colors"
              >
                <h3 className="font-semibold text-purple-900">View All Accounts</h3>
                <p className="text-sm text-purple-700">Monitor FD accounts</p>
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
                <span className="text-sm font-medium text-gray-700">Customer Growth (30 days)</span>
                <span className="text-lg font-bold text-green-600">+0%</span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <span className="text-sm font-medium text-gray-700">Total Deposits (30 days)</span>
                <span className="text-lg font-bold text-blue-600">₹0</span>
              </div>
              <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <span className="text-sm font-medium text-gray-700">Active FD Accounts</span>
                <span className="text-lg font-bold text-purple-600">0</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
