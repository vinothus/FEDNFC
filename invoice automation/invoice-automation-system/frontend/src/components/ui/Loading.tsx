import React from 'react';
import { cn } from '../../utils/cn';

export interface LoadingProps {
  size?: 'sm' | 'md' | 'lg';
  variant?: 'spinner' | 'dots' | 'pulse';
  text?: string;
  className?: string;
}

/**
 * Loading component following React UI Cursor Rules
 * - Accessible loading indicators with proper ARIA labels
 * - Multiple variants for different use cases
 * - Responsive sizing
 * - Semantic structure for screen readers
 */
const Loading: React.FC<LoadingProps> = ({
  size = 'md',
  variant = 'spinner',
  text,
  className,
}) => {
  const sizeClasses = {
    sm: 'h-4 w-4',
    md: 'h-8 w-8',
    lg: 'h-12 w-12',
  };

  const Spinner = () => (
    <svg
      className={cn('animate-spin', sizeClasses[size])}
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      aria-hidden="true"
    >
      <circle
        className="opacity-25"
        cx="12"
        cy="12"
        r="10"
        stroke="currentColor"
        strokeWidth="4"
      />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
      />
    </svg>
  );

  const Dots = () => (
    <div className="flex space-x-1">
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          className={cn(
            'rounded-full bg-current animate-pulse',
            size === 'sm' && 'h-1 w-1',
            size === 'md' && 'h-2 w-2',
            size === 'lg' && 'h-3 w-3'
          )}
          style={{
            animationDelay: `${i * 0.2}s`,
            animationDuration: '1s',
          }}
        />
      ))}
    </div>
  );

  const Pulse = () => (
    <div
      className={cn(
        'rounded-full bg-current animate-pulse',
        sizeClasses[size]
      )}
    />
  );

  const LoadingIndicator = () => {
    switch (variant) {
      case 'dots':
        return <Dots />;
      case 'pulse':
        return <Pulse />;
      default:
        return <Spinner />;
    }
  };

  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center text-blue-600',
        className
      )}
      role="status"
      aria-label={text || 'Loading content'}
    >
      <LoadingIndicator />
      {text && (
        <span className="mt-2 text-sm text-gray-600">
          {text}
        </span>
      )}
      <span className="sr-only">
        {text || 'Loading...'}
      </span>
    </div>
  );
};

export default Loading;
