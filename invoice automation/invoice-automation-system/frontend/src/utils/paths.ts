/**
 * Utility functions for handling paths and URLs
 */

/**
 * Get the base URL for the application
 * In development: uses React dev server (http://localhost:3000)
 * In production: uses the backend server with context path (/invoice-automation)
 */
export const getBaseUrl = (): string => {
  // Check if we're in development mode (React dev server)
  if (process.env.NODE_ENV === 'development' && window.location.port === '3000') {
    return '';
  }
  
  // Production mode or served from backend
  return '/invoice-automation';
};

/**
 * Get the full URL for a static asset
 * @param path - The path to the asset (e.g., '/montra-logo.svg')
 * @returns The full URL including base path
 */
export const getAssetUrl = (path: string): string => {
  const baseUrl = getBaseUrl();
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  return `${baseUrl}${cleanPath}`;
};

/**
 * Common asset URLs
 */
export const ASSETS = {
  MONTRA_LOGO: getAssetUrl('/montra-logo.svg'),
  MONTRA_LOGO_LOGIN: getAssetUrl('/montra-logo.jpg'),
  FAVICON: getAssetUrl('/favicon-montra.svg'),
  INVOICE_ICON: getAssetUrl('/invoice-icon.svg'),
} as const;
