import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import { Button, Input, Alert } from '../ui';
import { useAuth } from '../../contexts/AuthContext';
import { LoginRequest } from '../../types/auth';

// Validation schema
const loginSchema = yup.object({
  usernameOrEmail: yup
    .string()
    .required('Username or email is required')
    .min(3, 'Username or email must be at least 3 characters'),
  password: yup
    .string()
    .required('Password is required')
    .min(6, 'Password must be at least 6 characters'),
  rememberMe: yup.boolean().default(false),
});

type LoginFormData = {
  usernameOrEmail: string;
  password: string;
  rememberMe?: boolean;
};

export interface LoginFormProps {
  onSuccess?: () => void;
}

/**
 * Login form component following React UI Cursor Rules
 * - Uses react-hook-form for form management and validation
 * - Semantic form elements with proper labels
 * - WCAG AA accessible with proper error handling
 * - Responsive design (full width on mobile)
 * - Keyboard navigation support
 * - Password visibility toggle
 */
const LoginForm: React.FC<LoginFormProps> = ({ onSuccess }) => {
  const [showPassword, setShowPassword] = useState(false);
  const { login, isLoading, error, clearError } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm<LoginFormData>({
    resolver: yupResolver(loginSchema) as any,
    defaultValues: {
      usernameOrEmail: '',
      password: '',
      rememberMe: false,
    },
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      clearError();
      await login(data as LoginRequest);
      reset();
      onSuccess?.();
    } catch (error) {
      // Error is handled by the auth context
      console.error('Login failed:', error);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
      {/* Error Alert */}
      {error && (
        <Alert 
          variant="error" 
          dismissible 
          onDismiss={clearError}
        >
          {error}
        </Alert>
      )}

      {/* Username/Email Field */}
      <Input
        {...register('usernameOrEmail')}
        label="Username or Email"
        type="text"
        autoComplete="username"
        error={errors.usernameOrEmail?.message}
        fullWidth
        placeholder="Enter your username or email"
        disabled={isLoading || isSubmitting}
      />

      {/* Password Field */}
      <div className="space-y-1">
        <Input
          {...register('password')}
          label="Password"
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          error={errors.password?.message}
          fullWidth
          placeholder="Enter your password"
          disabled={isLoading || isSubmitting}
          endIcon={
            <button
              type="button"
              onClick={togglePasswordVisibility}
              className="p-1 text-gray-400 hover:text-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              tabIndex={0}
            >
              {showPassword ? (
                <EyeSlashIcon className="h-5 w-5" />
              ) : (
                <EyeIcon className="h-5 w-5" />
              )}
            </button>
          }
        />
      </div>

      {/* Remember Me Checkbox */}
      <div className="flex items-center">
        <input
          {...register('rememberMe')}
          id="remember-me"
          type="checkbox"
          className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded disabled:opacity-50"
          disabled={isLoading || isSubmitting}
        />
        <label 
          htmlFor="remember-me" 
          className="ml-2 block text-sm text-gray-900"
        >
          Remember me
        </label>
      </div>

      {/* Submit Button */}
      <Button
        type="submit"
        variant="primary"
        size="lg"
        fullWidth
        loading={isLoading || isSubmitting}
        disabled={isLoading || isSubmitting}
      >
        {isLoading || isSubmitting ? 'Signing in...' : 'Sign in'}
      </Button>

      {/* Additional Links */}
      <div className="text-sm text-center space-y-2">
        <a
          href="#forgot-password"
          className="text-blue-600 hover:text-blue-500 focus:outline-none focus:underline"
          onClick={(e) => {
            e.preventDefault();
            // TODO: Implement forgot password
            alert('Forgot password functionality coming soon');
          }}
        >
          Forgot your password?
        </a>
      </div>
    </form>
  );
};

export default LoginForm;
