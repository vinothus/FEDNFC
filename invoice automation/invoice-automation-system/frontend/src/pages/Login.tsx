import React from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import AuthLayout from '../components/auth/AuthLayout';
import LoginForm from '../components/auth/LoginForm';

/**
 * Login page component following React UI Cursor Rules
 * - Redirects authenticated users to dashboard
 * - Uses semantic layout and accessible form
 * - Handles return URL after successful login
 * - Responsive design
 */
const Login: React.FC = () => {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  
  // Get the return URL from navigation state, but always redirect to dashboard for root path
  const from = (location.state as any)?.from === '/' ? '/dashboard' : ((location.state as any)?.from || '/dashboard');

  // Show loading while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-gray-600">Checking authentication...</p>
        </div>
      </div>
    );
  }

  // Redirect if already authenticated - ensure we have both token and user data
  // Only redirect if we're definitely authenticated and not still loading
  if (isAuthenticated && user && !isLoading) {
    console.log('🔄 Login: User already authenticated, redirecting to:', from);
    return <Navigate to={from} replace />;
  }

  const handleLoginSuccess = () => {
    // Force navigation to dashboard after successful login
    console.log('Login successful, navigating to:', from);
    navigate(from, { replace: true });
  };

  return (
    <AuthLayout
      title="Sign in to Invoice Automation"
      subtitle="Welcome back! Please enter your credentials to continue."
    >
      <LoginForm onSuccess={handleLoginSuccess} />
    </AuthLayout>
  );
};

export default Login;
