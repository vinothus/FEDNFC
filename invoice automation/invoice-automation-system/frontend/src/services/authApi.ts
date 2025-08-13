import axios, { AxiosResponse } from 'axios';
import { LoginRequest, LoginResponse, RefreshTokenRequest, User } from '../types/auth';
import { ApiError } from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/invoice-automation';

console.log('🔧 AuthAPI: Configuration:', {
  baseURL: API_BASE_URL,
  env: process.env.REACT_APP_API_URL,
  currentURL: window.location.href
});

const authApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
authApi.interceptors.request.use(
  (config) => {
    console.log('📤 AuthAPI Request:', {
      method: config.method?.toUpperCase(),
      url: config.url,
      baseURL: config.baseURL,
      fullURL: `${config.baseURL}${config.url}`,
      headers: config.headers
    });
    
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
authApi.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('🚨 AuthAPI Error:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      url: error.config?.url,
      method: error.config?.method,
      baseURL: error.config?.baseURL,
      fullURL: error.response?.config?.url,
      data: error.response?.data
    });
    
    const apiError: ApiError = {
      message: error.response?.data?.message || error.message || 'An unexpected error occurred',
      status: error.response?.status || 500,
    };
    
    // Don't clear tokens here - let the main api.ts interceptor handle it
    // This prevents conflicts between multiple interceptors
    
    return Promise.reject(apiError);
  }
);

export const authService = {
  // Login user
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    console.log('🔐 AuthAPI: Attempting login with URL:', `${authApi.defaults.baseURL}/auth/login`);
    console.log('🔐 AuthAPI: Login credentials:', { usernameOrEmail: credentials.usernameOrEmail, password: '[HIDDEN]' });
    
    // Use the correct auth path (discovered from debugging)
    const response: AxiosResponse<LoginResponse> = await authApi.post('/auth/login', credentials);
    console.log('✅ AuthAPI: Login response received:', response.status);
    return response.data;
  },

  // Logout user
  logout: async (): Promise<void> => {
    try {
      console.log('🚪 AuthService: Attempting logout with /auth/logout');
      await authApi.post('/auth/logout');
      console.log('✅ AuthService: Logout successful');
    } catch (error) {
      // Continue with logout even if API call fails
      console.warn('⚠️ AuthService: Logout API call failed:', error);
    } finally {
      // Always clear local storage
      console.log('🗑️ AuthService: Clearing local storage');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  // Refresh token
  refreshToken: async (refreshTokenRequest: RefreshTokenRequest): Promise<LoginResponse> => {
    console.log('🔄 AuthService: Attempting token refresh with /auth/refresh');
    const response: AxiosResponse<LoginResponse> = await authApi.post('/auth/refresh', refreshTokenRequest);
    console.log('✅ AuthService: Token refresh successful');
    return response.data;
  },

  // Get current user info
  getCurrentUser: async (): Promise<User> => {
    console.log('🔍 AuthService: Calling /auth/me to validate token...');
    const response: AxiosResponse<User> = await authApi.get('/auth/me');
    console.log('✅ AuthService: /auth/me successful, user:', response.data);
    return response.data;
  },

  // Register new user (if needed)
  register: async (userData: any): Promise<LoginResponse> => {
    console.log('📝 AuthService: Attempting registration with /auth/register');
    const response: AxiosResponse<LoginResponse> = await authApi.post('/auth/register', userData);
    console.log('✅ AuthService: Registration successful');
    return response.data;
  },
};

export default authApi;
