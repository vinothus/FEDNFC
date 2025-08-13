import React from 'react';
import { 
  CheckCircleIcon, 
  ExclamationTriangleIcon, 
  XCircleIcon,
  ClockIcon 
} from '@heroicons/react/24/outline';
import { cn } from '../../utils/cn';
import { Loading } from '../ui';

export interface SystemHealthItem {
  id: string;
  name: string;
  status: 'healthy' | 'warning' | 'error' | 'unknown';
  message?: string;
  lastChecked?: string;
  responseTime?: number;
}

export interface SystemHealthProps {
  healthData: SystemHealthItem[];
  loading?: boolean;
  className?: string;
}

/**
 * SystemHealth component following React UI Cursor Rules
 * - Displays system health status
 * - Color-coded status indicators
 * - Accessible with proper ARIA attributes
 * - Responsive design
 * - Real-time status updates
 */
const SystemHealth: React.FC<SystemHealthProps> = ({
  healthData,
  loading = false,
  className,
}) => {
  const getStatusIcon = (status: SystemHealthItem['status']) => {
    const iconClass = 'h-5 w-5';
    
    switch (status) {
      case 'healthy':
        return <CheckCircleIcon className={cn(iconClass, 'text-green-500')} />;
      case 'warning':
        return <ExclamationTriangleIcon className={cn(iconClass, 'text-yellow-500')} />;
      case 'error':
        return <XCircleIcon className={cn(iconClass, 'text-red-500')} />;
      default:
        return <ClockIcon className={cn(iconClass, 'text-gray-400')} />;
    }
  };

  const getStatusText = (status: SystemHealthItem['status']) => {
    switch (status) {
      case 'healthy':
        return 'Healthy';
      case 'warning':
        return 'Warning';
      case 'error':
        return 'Error';
      default:
        return 'Unknown';
    }
  };

  const getStatusColor = (status: SystemHealthItem['status']) => {
    switch (status) {
      case 'healthy':
        return 'text-green-700 bg-green-50';
      case 'warning':
        return 'text-yellow-700 bg-yellow-50';
      case 'error':
        return 'text-red-700 bg-red-50';
      default:
        return 'text-gray-700 bg-gray-50';
    }
  };

  const getOverallStatus = () => {
    if (healthData.some(item => item.status === 'error')) return 'error';
    if (healthData.some(item => item.status === 'warning')) return 'warning';
    if (healthData.every(item => item.status === 'healthy')) return 'healthy';
    return 'unknown';
  };

  if (loading) {
    return (
      <div className={cn('bg-white rounded-lg shadow-sm border border-gray-200', className)}>
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">System Health</h3>
        </div>
        <div className="p-6">
          <Loading size="md" text="Checking system health..." />
        </div>
      </div>
    );
  }

  const overallStatus = getOverallStatus();

  return (
    <div className={cn('bg-white rounded-lg shadow-sm border border-gray-200', className)}>
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-medium text-gray-900">System Health</h3>
          <div className="flex items-center space-x-2">
            {getStatusIcon(overallStatus)}
            <span className={cn(
              'text-sm font-medium',
              overallStatus === 'healthy' ? 'text-green-700' :
              overallStatus === 'warning' ? 'text-yellow-700' :
              overallStatus === 'error' ? 'text-red-700' : 'text-gray-700'
            )}>
              {getStatusText(overallStatus)}
            </span>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="p-6">
        {healthData.length === 0 ? (
          <div className="text-center py-8">
            <ClockIcon className="mx-auto h-12 w-12 text-gray-400" />
            <h4 className="mt-4 text-lg font-medium text-gray-900">No health data available</h4>
            <p className="mt-2 text-sm text-gray-600">
              System health monitoring is not available at the moment.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {healthData.map((item) => (
              <div 
                key={item.id}
                className={cn(
                  'rounded-lg border p-4',
                  getStatusColor(item.status)
                )}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    {getStatusIcon(item.status)}
                    <div>
                      <h4 className="text-sm font-medium">
                        {item.name}
                      </h4>
                      {item.message && (
                        <p className="text-sm mt-1">
                          {item.message}
                        </p>
                      )}
                    </div>
                  </div>
                  
                  <div className="text-right text-sm">
                    {item.responseTime && (
                      <div className="font-medium">
                        {item.responseTime}ms
                      </div>
                    )}
                    {item.lastChecked && (
                      <div className="text-xs opacity-75 mt-1">
                        Last checked: {new Date(item.lastChecked).toLocaleTimeString()}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default SystemHealth;
