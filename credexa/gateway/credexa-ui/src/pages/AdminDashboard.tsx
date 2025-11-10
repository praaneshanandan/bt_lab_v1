import { useEffect, useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Users, Package, Wallet, TrendingUp } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

interface AdminStats {
  totalCustomers: number;
  totalProducts: number;
  totalAccounts: number;
  totalValue: number;
}

export default function AdminDashboard() {
  const navigate = useNavigate();
  const username = localStorage.getItem('username') || 'Admin';
  const [stats, setStats] = useState<AdminStats>({
    totalCustomers: 0,
    totalProducts: 0,
    totalAccounts: 0,
    totalValue: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchAdminStats();
  }, []);

  const fetchAdminStats = async () => {
    try {
      // Import API services
      const { customerApi, productApi, accountServiceApi } = await import('../services/api');
      
      // Fetch all data in parallel
      const [customersRes, productsRes, accountsRes] = await Promise.allSettled([
        customerApi.getAllCustomers(),
        productApi.getAllProducts(),
        accountServiceApi.getAllAccounts(),
      ]);
      
      console.log('Customers response:', customersRes);
      console.log('Products response:', productsRes);
      console.log('Accounts response:', accountsRes);
      
      // Extract customers count
      const customersData = customersRes.status === 'fulfilled' ? customersRes.value.data : null;
      console.log('Customers data:', customersData);
      const totalCustomers = Array.isArray(customersData?.data) 
        ? customersData.data.length 
        : Array.isArray(customersData) 
          ? customersData.length 
          : 0;
      
      // Extract products count
      const productsData = productsRes.status === 'fulfilled' ? productsRes.value.data : null;
      console.log('Products data:', productsData);
      // Backend returns { success, message, data: { content: [], totalElements, ...pagination } }
      const productsArray = productsData?.data?.content || productsData?.data || [];
      const totalProducts = productsData?.data?.totalElements || 
                           (Array.isArray(productsArray) ? productsArray.length : 0);
      console.log('Products array:', productsArray);
      console.log('Total products:', totalProducts);
      
      // Extract accounts data
      const accountsData = accountsRes.status === 'fulfilled' ? accountsRes.value.data : null;
      console.log('Accounts data:', accountsData);
      // Backend returns { success, message, data: { content: [], ...pagination } }
      const accounts = accountsData?.data?.content || accountsData?.data || [];
      console.log('Accounts array:', accounts);
      
      const totalAccounts = accounts.length;
      
      // Calculate total value (sum of all account balances)
      const totalValue = accounts.reduce((sum: number, acc: {currentBalance?: number, principalAmount?: number}) => {
        return sum + (acc.currentBalance || acc.principalAmount || 0);
      }, 0);
      
      setStats({
        totalCustomers,
        totalProducts,
        totalAccounts,
        totalValue,
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

      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
          <CardDescription>Common administrative tasks</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <button
              onClick={() => navigate('/customers')}
              className="text-left p-4 bg-blue-50 dark:bg-blue-950 hover:bg-blue-100 dark:hover:bg-blue-900 rounded-lg transition-colors border border-blue-200 dark:border-blue-800"
            >
              <h3 className="font-semibold text-blue-900 dark:text-blue-100">Manage Customers</h3>
              <p className="text-sm text-blue-700 dark:text-blue-300 mt-1">View and edit customer data</p>
            </button>
            <button
              onClick={() => navigate('/products')}
              className="text-left p-4 bg-green-50 dark:bg-green-950 hover:bg-green-100 dark:hover:bg-green-900 rounded-lg transition-colors border border-green-200 dark:border-green-800"
            >
              <h3 className="font-semibold text-green-900 dark:text-green-100">Manage Products</h3>
              <p className="text-sm text-green-700 dark:text-green-300 mt-1">Configure FD products</p>
            </button>
            <button
              onClick={() => navigate('/accounts')}
              className="text-left p-4 bg-purple-50 dark:bg-purple-950 hover:bg-purple-100 dark:hover:bg-purple-900 rounded-lg transition-colors border border-purple-200 dark:border-purple-800"
            >
              <h3 className="font-semibold text-purple-900 dark:text-purple-100">View All Accounts</h3>
              <p className="text-sm text-purple-700 dark:text-purple-300 mt-1">Monitor FD accounts</p>
            </button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

