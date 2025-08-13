import React, { forwardRef } from 'react';
import { cn } from '../../utils/cn';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
  startIcon?: React.ReactNode;
  endIcon?: React.ReactNode;
}

/**
 * Input component following React UI Cursor Rules
 * - Uses semantic HTML with proper label association
 * - WCAG AA compliant with proper htmlFor + id
 * - Supports error states with accessible error messages
 * - Responsive design with full width on mobile
 * - Keyboard accessible
 */
const Input = forwardRef<HTMLInputElement, InputProps>(({
  label,
  error,
  helperText,
  fullWidth = false,
  startIcon,
  endIcon,
  className,
  id,
  type = 'text',
  'aria-describedby': ariaDescribedBy,
  ...props
}, ref) => {
  const inputId = id || `input-${Math.random().toString(36).substr(2, 9)}`;
  const errorId = error ? `${inputId}-error` : undefined;
  const helperTextId = helperText ? `${inputId}-helper` : undefined;
  
  const describedBy = [
    ariaDescribedBy,
    errorId,
    helperTextId,
  ].filter(Boolean).join(' ') || undefined;

  const baseInputClasses = [
    'block px-3 py-2 border border-gray-300 rounded-md shadow-sm',
    'placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500',
    'disabled:bg-gray-50 disabled:text-gray-500 disabled:border-gray-200',
    'transition-colors duration-200',
  ];

  const errorClasses = error ? 'border-red-300 text-red-900 focus:ring-red-500 focus:border-red-500' : '';
  const widthClasses = fullWidth ? 'w-full' : 'w-full sm:max-w-xs';

  const inputClasses = cn(
    baseInputClasses,
    errorClasses,
    widthClasses,
    startIcon && 'pl-10',
    endIcon && 'pr-10',
    className
  );

  return (
    <div className={cn('space-y-1', fullWidth && 'w-full')}>
      {label && (
        <label 
          htmlFor={inputId}
          className="block text-sm font-medium text-gray-700"
        >
          {label}
        </label>
      )}
      
      <div className="relative">
        {startIcon && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <div className="h-5 w-5 text-gray-400" aria-hidden="true">
              {startIcon}
            </div>
          </div>
        )}
        
        <input
          ref={ref}
          type={type}
          id={inputId}
          className={inputClasses}
          aria-describedby={describedBy}
          aria-invalid={error ? 'true' : 'false'}
          {...props}
        />
        
        {endIcon && (
          <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none">
            <div className="h-5 w-5 text-gray-400" aria-hidden="true">
              {endIcon}
            </div>
          </div>
        )}
      </div>
      
      {error && (
        <p 
          id={errorId}
          className="text-sm text-red-600"
          role="alert"
          aria-live="polite"
        >
          {error}
        </p>
      )}
      
      {helperText && !error && (
        <p 
          id={helperTextId}
          className="text-sm text-gray-500"
        >
          {helperText}
        </p>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
