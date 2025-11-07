import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'sonner';
import { Sidebar } from './components/layout/Sidebar';
import { ProtectedRoute } from './components/auth/ProtectedRoute';
import { SessionWarningModal } from './components/SessionWarningModal';
import { useSessionManager } from './hooks/useSessionManager';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Customers from './pages/Customers';
import MyProfile from './pages/MyProfile';
import Customer360View from './pages/Customer360View';
import Products from './pages/Products';
import Calculator from './pages/Calculator';
import FDAccounts from './pages/FDAccounts';
import CreateFDAccount from './pages/CreateFDAccount';
import './App.css';

// Session manager wrapper that must be inside Router
function AppContent() {
  const { showWarning, secondsRemaining, continueSession, logout } = useSessionManager();

  return (
    <>
      <SessionWarningModal
        open={showWarning}
        secondsRemaining={secondsRemaining}
        onContinue={continueSession}
        onLogout={logout}
      />
      <Routes>
        {/* Public route */}
        <Route path="/login" element={<Login />} />
        
        {/* Protected routes */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <div className="flex h-screen bg-gray-50">
                <Sidebar />
                <main className="flex-1 overflow-y-auto">
                  <Routes>
                    <Route path="/" element={<Dashboard />} />
                    <Route path="/customers" element={<Customers />} />
                    <Route path="/customers/:id/360-view" element={<Customer360View />} />
                    <Route path="/profile" element={<MyProfile />} />
                    <Route path="/products" element={<Products />} />
                    <Route path="/calculator" element={<Calculator />} />
                    <Route path="/accounts" element={<FDAccounts />} />
                    <Route path="/accounts/create" element={<CreateFDAccount />} />
                    <Route path="*" element={<Navigate to="/" replace />} />
                  </Routes>
                </main>
              </div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </>
  );
}

function App() {
  return (
    <>
      <Toaster position="top-right" richColors expand={false} closeButton />
      <BrowserRouter>
        <AppContent />
      </BrowserRouter>
    </>
  );
}

export default App;
