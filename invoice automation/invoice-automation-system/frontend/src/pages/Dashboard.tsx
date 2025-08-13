import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import DashboardLayout from '../components/dashboard/DashboardLayout';
import AdminDashboard from '../components/admin/AdminDashboard';

/**
 * Dashboard page component following React UI Cursor Rules
 * - Uses the new enhanced dashboard layout
 * - Shows admin dashboard for administrators
 * - Responsive design with mobile-first approach
 * - Real-time data updates
 * - Accessible dashboard structure
 */
const Dashboard: React.FC = () => {
  const { hasRole } = useAuth();

  // Show admin dashboard for administrators
  if (hasRole('ADMIN')) {
    return (
      <div className="space-y-8">
        <AdminDashboard />
        <div className="border-t border-gray-200 pt-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Invoice Overview</h2>
          <DashboardLayout />
        </div>
      </div>
    );
  }

  // Regular dashboard for non-admin users
  return <DashboardLayout />;
};

export default Dashboard;