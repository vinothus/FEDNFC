import axios, { AxiosResponse } from 'axios';
import { Invoice, UploadResponse, DashboardStats, ApiError } from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/invoice-automation';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
      console.log('🔑 API: Adding auth token to request:', config.url);
    } else {
      console.log('⚠️ API: No token found for request:', config.url);
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError: ApiError = {
      message: error.response?.data?.message || error.message || 'An unexpected error occurred',
      status: error.response?.status || 500,
    };
    
    // Handle 401 errors (unauthorized)
    if (error.response?.status === 401) {
      // Don't clear tokens if this is a login request (to prevent clearing just-stored tokens)
      const isLoginRequest = error.config?.url?.includes('/auth/login') || error.config?.url?.includes('/api/auth/login');
      const isAuthRequest = error.config?.url?.includes('/auth/') || error.config?.url?.includes('/api/auth/');
      
      if (!isLoginRequest && !isAuthRequest) {
        console.log('🔒 API Interceptor: 401 error on non-auth request');
        console.log('🔒 API Interceptor: Request URL was:', error.config?.url);
        console.log('🔒 API Interceptor: Request token:', error.config?.headers?.Authorization?.substring(0, 30) + '...');
        console.log('🔒 API Interceptor: Current stored token:', localStorage.getItem('accessToken')?.substring(0, 30) + '...');
        
        // Check if the request was made with a stale token (different from current stored token)
        const requestToken = error.config?.headers?.Authorization?.replace('Bearer ', '');
        const currentToken = localStorage.getItem('accessToken');
        
        // Only clear tokens if the failed request was made with the current token
        // This prevents clearing tokens during race conditions where old requests fail
        // while new tokens are being refreshed
        if (requestToken === currentToken) {
          console.log('🔒 API Interceptor: 401 with current token, clearing tokens');
          
          // Clear tokens on unauthorized for non-auth requests
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          
          // For React apps, let the AuthContext handle routing
          // Only force redirect if we're not in a React-managed route
          if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/invoice-automation')) {
            window.location.href = '/invoice-automation/login';
          }
        } else {
          console.log('🔒 API Interceptor: 401 with stale token, not clearing (race condition avoided)');
        }
      } else {
        console.log('🔑 API Interceptor: 401 on auth request, not clearing tokens');
        console.log('🔑 API Interceptor: Auth request URL was:', error.config?.url);
      }
    }
    
    return Promise.reject(apiError);
  }
);

export const invoiceApi = {
  // Get all invoices (without pagination)
  getAllInvoices: async (): Promise<Invoice[]> => {
    const response: AxiosResponse<Invoice[]> = await api.get('/api/invoices');
    return response.data;
  },

  // Get all invoices with pagination
  getAllInvoicesWithPagination: async (params?: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<{
    content: Invoice[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
    numberOfElements: number;
    first: boolean;
    last: boolean;
    empty: boolean;
  }> => {
    const response: AxiosResponse<any> = await api.get('/api/invoices', { params });
    return response.data;
  },

  // Get invoice by ID
  getInvoiceById: async (id: string): Promise<Invoice> => {
    const response: AxiosResponse<Invoice> = await api.get(`/api/invoices/${id}`);
    return response.data;
  },

  // Get detailed invoice data including raw text
  getInvoiceDetails: async (id: string): Promise<Invoice & { rawText?: string; extractedData?: Record<string, any> }> => {
    console.log('📄 Fetching detailed invoice data for ID:', id);
    const response: AxiosResponse<Invoice & { rawText?: string; extractedData?: Record<string, any> }> = await api.get(`/api/invoices/${id}/details`);
    console.log('✅ Invoice details fetched successfully:', {
      id: response.data.id,
      hasRawText: !!response.data.rawText,
      rawTextLength: response.data.rawText?.length || 0,
      hasExtractedData: !!response.data.extractedData
    });
    return response.data;
  },

  // Upload invoice
  uploadInvoice: async (file: File): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    
    const response: AxiosResponse<UploadResponse> = await api.post('/api/invoices/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // Delete invoice
  deleteInvoice: async (id: string): Promise<void> => {
    await api.delete(`/api/invoices/${id}`);
  },

  // Get dashboard stats
  getDashboardStats: async (): Promise<DashboardStats> => {
    const response: AxiosResponse<DashboardStats> = await api.get('/dashboard/stats');
    return response.data;
  },

  // Reprocess failed invoice
  reprocessInvoice: async (id: string): Promise<UploadResponse> => {
    const response: AxiosResponse<UploadResponse> = await api.post(`/api/invoices/${id}/reprocess`);
    return response.data;
  },
};

export { api };
export default api;
