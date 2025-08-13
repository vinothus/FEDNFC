import React from 'react';
import { cn } from '../../utils/cn';

export interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon?: React.ReactNode;
  trend?: {
    value: number;
    label: string;
    type: 'increase' | 'decrease' | 'neutral';
  };
  loading?: boolean;
  className?: string;
}

/**
 * StatCard component following React UI Cursor Rules
 * - Responsive design with mobile-first approach
 * - Accessible with proper ARIA labels
 * - Loading states with skeleton animation
 * - Semantic HTML structure
 * - WCAG AA color contrast
 */
const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  loading = false,
  className,
}) => {
  if (loading) {
    return (
      <div className={cn(
        'bg-white rounded-lg shadow-sm border border-gray-200 p-6',
        'animate-pulse',
        className
      )}>
        <div className="flex items-center justify-between">
          <div className="space-y-2 flex-1">
            <div className="h-4 bg-gray-200 rounded w-24"></div>
            <div className="h-8 bg-gray-200 rounded w-16"></div>
            <div className="h-3 bg-gray-200 rounded w-20"></div>
          </div>
          <div className="h-8 w-8 bg-gray-200 rounded"></div>
        </div>
      </div>
    );
  }

  const getTrendColor = (type: string) => {
    switch (type) {
      case 'increase':
        return 'text-green-600';
      case 'decrease':
        return 'text-red-600';
      default:
        return 'text-gray-600';
    }
  };

  const getTrendIcon = (type: string) => {
    switch (type) {
      case 'increase':
        return (
          <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
            <path fillRule="evenodd" d="M3.293 9.707a1 1 0 010-1.414l6-6a1 1 0 011.414 0l6 6a1 1 0 01-1.414 1.414L11 5.414V17a1 1 0 11-2 0V5.414L4.707 9.707a1 1 0 01-1.414 0z" clipRule="evenodd" />
          </svg>
        );
      case 'decrease':
        return (
          <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
            <path fillRule="evenodd" d="M16.707 10.293a1 1 0 010 1.414l-6 6a1 1 0 01-1.414 0l-6-6a1 1 0 111.414-1.414L9 14.586V3a1 1 0 012 0v11.586l4.293-4.293a1 1 0 011.414 0z" clipRule="evenodd" />
          </svg>
        );
      default:
        return null;
    }
  };

  return (
    <div 
      className={cn(
        'bg-white rounded-lg shadow-sm border border-gray-200 p-6',
        'hover:shadow-md transition-shadow duration-200',
        className
      )}
      role="region"
      aria-labelledby={`stat-${title.replace(/\s+/g, '-').toLowerCase()}`}
    >
      <div className="flex items-center justify-between">
        <div className="space-y-2 flex-1 min-w-0">
          <h3 
            id={`stat-${title.replace(/\s+/g, '-').toLowerCase()}`}
            className="text-sm font-medium text-gray-600 truncate"
          >
            {title}
          </h3>
          
          <p className="text-2xl sm:text-3xl font-bold text-gray-900 truncate">
            {typeof value === 'number' ? value.toLocaleString() : value}
          </p>
          
          {subtitle && (
            <p className="text-sm text-gray-500 truncate">
              {subtitle}
            </p>
          )}
          
          {trend && (
            <div className={cn(
              'flex items-center text-sm',
              getTrendColor(trend.type)
            )}>
              {getTrendIcon(trend.type)}
              <span className="ml-1 font-medium">
                {Math.abs(trend.value)}%
              </span>
              <span className="ml-1 text-gray-600">
                {trend.label}
              </span>
            </div>
          )}
        </div>
        
        {icon && (
          <div className="flex-shrink-0 ml-4">
            <div className="h-8 w-8 text-gray-400" aria-hidden="true">
              {icon}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default StatCard;
