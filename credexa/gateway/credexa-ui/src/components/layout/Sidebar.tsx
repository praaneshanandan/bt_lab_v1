import { Link, useLocation, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Users, 
  Package, 
  Calculator, 
  Menu,
  X,
  LogOut,
  User,
  Landmark,
  Receipt,
  Settings,
  Coins
} from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/ThemeToggle';
import { authApi } from '@/services/api';
import { isManagerOrAdmin } from '@/utils/auth';

const navigation = [
  { name: 'Dashboard', href: '/', icon: LayoutDashboard, requiresManagerOrAdmin: false },
  { name: 'My Profile', href: '/profile', icon: User, requiresManagerOrAdmin: false },
  { name: 'Customers', href: '/customers', icon: Users, requiresManagerOrAdmin: true },
  { name: 'Products', href: '/products', icon: Package, requiresManagerOrAdmin: false },
  { name: 'FD Calculator', href: '/calculator', icon: Calculator, requiresManagerOrAdmin: false },
  { name: 'FD Accounts', href: '/fd-accounts', icon: Landmark, requiresManagerOrAdmin: false },
  { name: 'Transactions', href: '/transactions', icon: Receipt, requiresManagerOrAdmin: false },
  { name: 'Redemptions', href: '/redemptions', icon: Coins, requiresManagerOrAdmin: false },
  { name: 'Batch Management', href: '/batch-management', icon: Settings, requiresManagerOrAdmin: true },
];

export function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const [isOpen, setIsOpen] = useState(true);
  const username = localStorage.getItem('username') || 'User';
  const hasAdminAccess = isManagerOrAdmin();
  
  // Filter navigation based on user role
  const filteredNavigation = navigation.filter(item => 
    !item.requiresManagerOrAdmin || hasAdminAccess
  );

  const handleLogout = async () => {
    try {
      // Call logout API
      await authApi.logout();
    } catch (error) {
      console.error('Logout API error:', error);
    } finally {
      // Clear local storage regardless of API call result
      localStorage.removeItem('isAuthenticated');
      localStorage.removeItem('authToken');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      localStorage.removeItem('userRoles');
      navigate('/login');
    }
  };

  return (
    <>
      {/* Mobile menu button */}
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="lg:hidden fixed top-4 left-4 z-50 p-2 rounded-md bg-sidebar text-sidebar-foreground border border-sidebar-border"
      >
        {isOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      {/* Sidebar */}
      <div
        className={`${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        } lg:translate-x-0 fixed lg:sticky top-0 left-0 z-40 h-screen w-64 bg-sidebar text-sidebar-foreground border-r border-sidebar-border transition-transform duration-300 ease-in-out`}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center justify-center h-16 border-b border-sidebar-border px-4">
            <img 
              src="/bank-logo.png" 
              alt="Bank Logo" 
              className="h-12 w-auto object-contain"
            />
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
            {filteredNavigation.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname === item.href;
              
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-sidebar-accent text-sidebar-accent-foreground'
                      : 'text-sidebar-foreground hover:bg-sidebar-accent/50'
                  }`}
                  onClick={() => window.innerWidth < 1024 && setIsOpen(false)}
                >
                  <Icon size={20} />
                  <span className="font-medium">{item.name}</span>
                </Link>
              );
            })}
          </nav>

          {/* Footer */}
          <div className="p-4 border-t border-sidebar-border space-y-3">
            {/* User Info */}
            <div className="flex items-center gap-3 px-4 py-2 bg-sidebar-accent rounded-lg">
              <div className="p-2 bg-sidebar-primary rounded-full">
                <User size={16} className="text-sidebar-primary-foreground" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-sidebar-foreground truncate">{username}</p>
                <p className="text-xs text-sidebar-foreground/60">Logged in</p>
              </div>
              {/* Theme Toggle */}
              <ThemeToggle />
            </div>
            
            {/* Logout Button */}
            <Button
              variant="ghost"
              className="w-full justify-start text-sidebar-foreground hover:bg-destructive/20 hover:text-destructive"
              onClick={handleLogout}
            >
              <LogOut size={20} className="mr-3" />
              <span className="font-medium">Logout</span>
            </Button>
            
            <p className="text-sm text-sidebar-foreground/60 text-center pt-2">
              Banking Portal v1.0
            </p>
          </div>
        </div>
      </div>

      {/* Overlay for mobile */}
      {isOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-black bg-opacity-50 z-30"
          onClick={() => setIsOpen(false)}
        />
      )}
    </>
  );
}

