import React from 'react';
import { cn } from '../../utils/cn';
import { Loading } from '../ui';

export interface ChartContainerProps {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  loading?: boolean;
  error?: string;
  actions?: React.ReactNode;
  className?: string;
  height?: number;
}

/**
 * ChartContainer component following React UI Cursor Rules
 * - Provides consistent layout for charts
 * - Handles loading and error states
 * - Responsive design
 * - Accessible with proper headings
 */
const ChartContainer: React.FC<ChartContainerProps> = ({
  title,
  subtitle,
  children,
  loading = false,
  error,
  actions,
  className,
  height = 300,
}) => {
  return (
    <div 
      className={cn(
        'bg-white rounded-lg shadow-sm border border-gray-200',
        className
      )}
    >
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-lg font-medium text-gray-900">
              {title}
            </h3>
            {subtitle && (
              <p className="mt-1 text-sm text-gray-600">
                {subtitle}
              </p>
            )}
          </div>
          {actions && (
            <div className="flex items-center space-x-2">
              {actions}
            </div>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="p-6">
        {loading ? (
          <div 
            className="flex items-center justify-center"
            style={{ height: `${height}px` }}
          >
            <Loading size="lg" text="Loading chart data..." />
          </div>
        ) : error ? (
          <div 
            className="flex flex-col items-center justify-center text-center"
            style={{ height: `${height}px` }}
          >
            <div className="mx-auto h-12 w-12 text-red-400 mb-4">
              <svg fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path 
                  strokeLinecap="round" 
                  strokeLinejoin="round" 
                  strokeWidth={2} 
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"
                />
              </svg>
            </div>
            <h4 className="text-lg font-medium text-gray-900 mb-2">
              Unable to load chart
            </h4>
            <p className="text-sm text-gray-600">
              {error}
            </p>
          </div>
        ) : (
          <div style={{ height: `${height}px` }}>
            {children}
          </div>
        )}
      </div>
    </div>
  );
};

export default ChartContainer;
