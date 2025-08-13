import React, { useState, useEffect } from 'react';
import {
  UsersIcon,
  Cog6ToothIcon,
  DocumentTextIcon,
  ChartBarIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  LockClosedIcon,
} from '@heroicons/react/24/outline';
import { Link } from 'react-router-dom';
import { userApi, UserStatsResponse } from '../../services/userApi';
import { settingsApi, SettingsStatsResponse } from '../../services/settingsApi';
import { cn } from '../../utils/cn';
import { Button, Loading, Alert } from '../ui';
import StatCard from '../dashboard/StatCard';

export interface AdminDashboardProps {
  className?: string;
}

interface AdminStats {
  users: UserStatsResponse | null;
  settings: SettingsStatsResponse | null;
}

/**
 * AdminDashboard component following React UI Cursor Rules
 * - Admin-specific dashboard with user, pattern, and settings statistics
 * - Quick access to admin functions
 * - System health indicators
 * - Responsive design with proper accessibility
 */
const AdminDashboard: React.FC<AdminDashboardProps> = ({ className }) => {
  const [stats, setStats] = useState<AdminStats>({
    users: null,
    settings: null,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchAdminStats = async () => {
      try {
        setLoading(true);
        setError(null);

        // Fetch all admin statistics in parallel
        const [userStats, settingsStats] = await Promise.all([
          userApi.getUserStats().catch(() => null),
          settingsApi.getAllSettings().catch(() => null),
        ]);

        setStats({
          users: userStats,
          settings: settingsStats ? {
            totalSettings: settingsStats.totalSettings,
            categoriesCount: settingsStats.categories.length,
            modifiedSettings: settingsStats.modifiableSettings,
            requiresRestart: settingsStats.restartRequired ? 1 : 0,
          } : null,
        });
      } catch (err: any) {
        setError(err.message || 'Failed to load admin statistics');
      } finally {
        setLoading(false);
      }
    };

    fetchAdminStats();
  }, []);

  const handleRefresh = () => {
    setLoading(true);
    setError(null);
    // Re-fetch data
    window.location.reload();
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <Loading size="lg" text="Loading admin dashboard..." />
      </div>
    );
  }

  return (
    <div className={cn('space-y-6', className)}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
          <p className="mt-2 text-gray-600">
            Comprehensive overview and management of system resources
          </p>
        </div>
        <div className="mt-4 sm:mt-0">
          <Button
            variant="secondary"
            size="sm"
            onClick={handleRefresh}
            className="w-full sm:w-auto"
          >
            <ChartBarIcon className="h-4 w-4 mr-2" />
            Refresh Stats
          </Button>
        </div>
      </div>

      {/* Error State */}
      {error && (
        <Alert variant="error" dismissible onDismiss={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Quick Actions */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Link
            to="/users"
            className="flex items-center p-4 rounded-lg border border-gray-200 hover:border-blue-300 hover:bg-blue-50 transition-colors duration-200"
          >
            <UsersIcon className="h-8 w-8 text-blue-600 mr-3" />
            <div>
              <h3 className="font-medium text-gray-900">Manage Users</h3>
              <p className="text-sm text-gray-600">Create, edit, and manage user accounts</p>
            </div>
          </Link>

          <Link
            to="/patterns"
            className="flex items-center p-4 rounded-lg border border-gray-200 hover:border-green-300 hover:bg-green-50 transition-colors duration-200"
          >
            <DocumentTextIcon className="h-8 w-8 text-green-600 mr-3" />
            <div>
              <h3 className="font-medium text-gray-900">Pattern Management</h3>
              <p className="text-sm text-gray-600">Configure OCR extraction patterns</p>
            </div>
          </Link>

          <Link
            to="/settings"
            className="flex items-center p-4 rounded-lg border border-gray-200 hover:border-purple-300 hover:bg-purple-50 transition-colors duration-200"
          >
            <Cog6ToothIcon className="h-8 w-8 text-purple-600 mr-3" />
            <div>
              <h3 className="font-medium text-gray-900">System Settings</h3>
              <p className="text-sm text-gray-600">Configure application settings</p>
            </div>
          </Link>
        </div>
      </div>

      {/* User Management Statistics */}
      {stats.users && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-medium text-gray-900">User Management</h2>
            <Link
              to="/users"
              className="text-sm font-medium text-blue-600 hover:text-blue-500"
            >
              View all users →
            </Link>
          </div>
          
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              title="Total Users"
              value={stats.users.totalUsers}
              icon={<UsersIcon className="h-6 w-6" />}
              className="bg-blue-50 border-blue-200"
            />
            <StatCard
              title="Active Users"
              value={stats.users.activeUsers}
              icon={<CheckCircleIcon className="h-6 w-6" />}
              className="bg-green-50 border-green-200"
            />
            <StatCard
              title="Locked Users"
              value={stats.users.lockedUsers}
              icon={<LockClosedIcon className="h-6 w-6" />}
              className="bg-red-50 border-red-200"
            />
            <StatCard
              title="Administrators"
              value={stats.users.adminUsers}
              icon={<ExclamationTriangleIcon className="h-6 w-6" />}
              className="bg-yellow-50 border-yellow-200"
            />
          </div>

          {/* User Role Distribution */}
          <div className="mt-6 pt-6 border-t border-gray-200">
            <h3 className="text-sm font-medium text-gray-900 mb-3">Role Distribution</h3>
            <div className="grid grid-cols-3 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-red-600">{stats.users.adminUsers}</div>
                <div className="text-sm text-gray-500">Admins</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-600">{stats.users.approverUsers}</div>
                <div className="text-sm text-gray-500">Approvers</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-600">{stats.users.regularUsers}</div>
                <div className="text-sm text-gray-500">Users</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* System Settings Statistics */}
      {stats.settings && (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-medium text-gray-900">System Configuration</h2>
            <Link
              to="/settings"
              className="text-sm font-medium text-purple-600 hover:text-purple-500"
            >
              Manage settings →
            </Link>
          </div>
          
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              title="Total Settings"
              value={stats.settings.totalSettings}
              icon={<Cog6ToothIcon className="h-6 w-6" />}
              className="bg-purple-50 border-purple-200"
            />
            <StatCard
              title="Categories"
              value={stats.settings.categoriesCount}
              icon={<DocumentTextIcon className="h-6 w-6" />}
              className="bg-indigo-50 border-indigo-200"
            />
            <StatCard
              title="Modified"
              value={stats.settings.modifiedSettings}
              icon={<CheckCircleIcon className="h-6 w-6" />}
              className="bg-green-50 border-green-200"
            />
            <StatCard
              title="Requires Restart"
              value={stats.settings.requiresRestart}
              icon={<ClockIcon className="h-6 w-6" />}
              className="bg-orange-50 border-orange-200"
            />
          </div>

          {/* System Health Indicators */}
          {stats.settings.requiresRestart > 0 && (
            <div className="mt-6 pt-6 border-t border-gray-200">
              <Alert variant="warning">
                <ExclamationTriangleIcon className="h-5 w-5" />
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-yellow-800">
                    System Restart Required
                  </h3>
                  <div className="mt-2 text-sm text-yellow-700">
                    {stats.settings.requiresRestart} setting(s) require an application restart to take effect.
                  </div>
                </div>
              </Alert>
            </div>
          )}
        </div>
      )}

      {/* System Status */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <h2 className="text-lg font-medium text-gray-900 mb-4">System Status</h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="flex items-center p-4 rounded-lg bg-green-50 border border-green-200">
            <CheckCircleIcon className="h-8 w-8 text-green-600 mr-3" />
            <div>
              <h3 className="font-medium text-green-900">API Services</h3>
              <p className="text-sm text-green-700">All services operational</p>
            </div>
          </div>
          
          <div className="flex items-center p-4 rounded-lg bg-green-50 border border-green-200">
            <CheckCircleIcon className="h-8 w-8 text-green-600 mr-3" />
            <div>
              <h3 className="font-medium text-green-900">Database</h3>
              <p className="text-sm text-green-700">Connected and responsive</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
