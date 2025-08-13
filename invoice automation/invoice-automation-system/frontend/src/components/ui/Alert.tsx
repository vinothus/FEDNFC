import React from 'react';
import { 
  CheckCircleIcon, 
  ExclamationTriangleIcon, 
  InformationCircleIcon, 
  XCircleIcon,
  XMarkIcon 
} from '@heroicons/react/24/outline';
import { cn } from '../../utils/cn';
import Button from './Button';

export interface AlertProps {
  variant?: 'success' | 'warning' | 'error' | 'info';
  title?: string;
  children: React.ReactNode;
  dismissible?: boolean;
  onDismiss?: () => void;
  className?: string;
}

/**
 * Alert component following React UI Cursor Rules
 * - Uses semantic structure with proper ARIA roles
 * - WCAG AA color contrast compliant
 * - Keyboard accessible dismiss functionality
 * - Screen reader friendly with live regions
 */
const Alert: React.FC<AlertProps> = ({
  variant = 'info',
  title,
  children,
  dismissible = false,
  onDismiss,
  className,
}) => {
  const variantConfig = {
    success: {
      containerClasses: 'bg-green-50 border-green-200',
      iconClasses: 'text-green-400',
      titleClasses: 'text-green-800',
      textClasses: 'text-green-700',
      Icon: CheckCircleIcon,
    },
    warning: {
      containerClasses: 'bg-yellow-50 border-yellow-200',
      iconClasses: 'text-yellow-400',
      titleClasses: 'text-yellow-800',
      textClasses: 'text-yellow-700',
      Icon: ExclamationTriangleIcon,
    },
    error: {
      containerClasses: 'bg-red-50 border-red-200',
      iconClasses: 'text-red-400',
      titleClasses: 'text-red-800',
      textClasses: 'text-red-700',
      Icon: XCircleIcon,
    },
    info: {
      containerClasses: 'bg-blue-50 border-blue-200',
      iconClasses: 'text-blue-400',
      titleClasses: 'text-blue-800',
      textClasses: 'text-blue-700',
      Icon: InformationCircleIcon,
    },
  };

  const config = variantConfig[variant];
  const { Icon } = config;

  return (
    <div
      className={cn(
        'rounded-md border p-4',
        config.containerClasses,
        className
      )}
      role={variant === 'error' ? 'alert' : 'status'}
      aria-live={variant === 'error' ? 'assertive' : 'polite'}
    >
      <div className="flex">
        <div className="flex-shrink-0">
          <Icon 
            className={cn('h-5 w-5', config.iconClasses)}
            aria-hidden="true"
          />
        </div>
        
        <div className="ml-3 flex-1">
          {title && (
            <h3 className={cn('text-sm font-medium', config.titleClasses)}>
              {title}
            </h3>
          )}
          
          <div className={cn(
            'text-sm',
            config.textClasses,
            title && 'mt-2'
          )}>
            {children}
          </div>
        </div>
        
        {dismissible && onDismiss && (
          <div className="ml-auto pl-3">
            <div className="-mx-1.5 -my-1.5">
              <Button
                variant="ghost"
                size="sm"
                onClick={onDismiss}
                aria-label="Dismiss alert"
                className={cn(
                  'inline-flex rounded-md p-1.5 focus:outline-none focus:ring-2 focus:ring-offset-2',
                  config.textClasses,
                  'hover:bg-opacity-20'
                )}
              >
                <XMarkIcon className="h-5 w-5" aria-hidden="true" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Alert;
