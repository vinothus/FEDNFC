import React from 'react';
import { cn } from '../../utils/cn';
import { ASSETS } from '../../utils/paths';

export interface AuthLayoutProps {
  children: React.ReactNode;
  title: string;
  subtitle?: string;
  className?: string;
}

/**
 * Authentication layout component following React UI Cursor Rules
 * - Responsive design: mobile-first approach
 * - Semantic HTML structure
 * - Accessible heading hierarchy
 * - Professional and clean design
 */
const AuthLayout: React.FC<AuthLayoutProps> = ({
  children,
  title,
  subtitle,
  className,
}) => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        {/* Header */}
        <div className="text-center">
          {/* Montra Logo - Larger and Centered */}
          <div className="mx-auto flex justify-center mb-8">
            <img 
              src={ASSETS.MONTRA_LOGO_LOGIN} 
              alt="Montra Logo" 
              className="h-32 w-auto"
            />
          </div>
          
          <h1 className="mt-6 text-3xl font-extrabold text-gray-900">
            {title}
          </h1>
          
          {subtitle && (
            <p className="mt-2 text-sm text-gray-600">
              {subtitle}
            </p>
          )}
        </div>

        {/* Content */}
        <div className={cn('mt-8 space-y-6', className)}>
          {children}
        </div>

        {/* Footer */}
        <div className="text-center">
          <p className="text-xs text-gray-500">
            Invoice Automation System
          </p>
          <p className="text-xs text-gray-400 mt-1">
            Powered by Montra
          </p>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
