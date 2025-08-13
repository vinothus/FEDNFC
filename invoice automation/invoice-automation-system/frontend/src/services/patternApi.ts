import axios, { AxiosResponse } from 'axios';
import { 
  InvoicePattern, 
  PatternRequest, 
  PatternResponse, 
  PatternCategory, 
  PatternTestRequest,
  PatternTestResult,
  PatternStats 
} from '../types/pattern';
import { ApiError } from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/invoice-automation';

const patternApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
patternApi.interceptors.request.use(
  (config) => {
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
patternApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiError: ApiError = {
      message: error.response?.data?.message || error.message || 'An unexpected error occurred',
      status: error.response?.status || 500,
    };
    return Promise.reject(apiError);
  }
);

export const patternService = {
  // Get all patterns with pagination and filtering
  getPatterns: async (params?: {
    page?: number;
    size?: number;
    category?: string;
    active?: boolean;
  }): Promise<{
    patterns: InvoicePattern[];
    totalPages: number;
    totalElements: number;
  }> => {
    const response: AxiosResponse = await patternApi.get('/admin/patterns', { params });
    return response.data;
  },

  // Get pattern by ID
  getPatternById: async (id: string): Promise<InvoicePattern> => {
    const response: AxiosResponse<InvoicePattern> = await patternApi.get(`/admin/patterns/${id}`);
    return response.data;
  },

  // Create new pattern
  createPattern: async (pattern: PatternRequest): Promise<PatternResponse> => {
    const response: AxiosResponse<PatternResponse> = await patternApi.post('/admin/patterns', pattern);
    return response.data;
  },

  // Update existing pattern
  updatePattern: async (id: string, pattern: Partial<PatternRequest>): Promise<PatternResponse> => {
    const response: AxiosResponse<PatternResponse> = await patternApi.put(`/admin/patterns/${id}`, pattern);
    return response.data;
  },

  // Delete pattern
  deletePattern: async (id: string): Promise<void> => {
    await patternApi.delete(`/admin/patterns/${id}`);
  },

  // Test pattern against sample text
  testPattern: async (testRequest: PatternTestRequest): Promise<PatternTestResult> => {
    const response: AxiosResponse<PatternTestResult> = await patternApi.post('/admin/patterns/test', testRequest);
    return response.data;
  },

  // Get active patterns only
  getActivePatterns: async (): Promise<InvoicePattern[]> => {
    const response: AxiosResponse<InvoicePattern[]> = await patternApi.get('/admin/patterns/active');
    return response.data;
  },

  // Get pattern categories
  getCategories: async (): Promise<PatternCategory[]> => {
    const response: AxiosResponse<PatternCategory[]> = await patternApi.get('/admin/patterns/categories');
    return response.data;
  },

  // Get patterns by category
  getPatternsByCategory: async (category: string): Promise<InvoicePattern[]> => {
    const response: AxiosResponse<InvoicePattern[]> = await patternApi.get(`/admin/patterns/category/${category}`);
    return response.data;
  },

  // Get pattern statistics
  getPatternStats: async (): Promise<PatternStats> => {
    const response: AxiosResponse<PatternStats> = await patternApi.get('/admin/patterns/stats');
    return response.data;
  },

  // Bulk operations
  bulkUpdatePatterns: async (patternIds: string[], updates: Partial<PatternRequest>): Promise<void> => {
    await patternApi.patch('/admin/patterns/bulk', { patternIds, updates });
  },

  // Toggle pattern active status
  togglePatternStatus: async (id: string): Promise<InvoicePattern> => {
    const response: AxiosResponse<InvoicePattern> = await patternApi.patch(`/admin/patterns/${id}/toggle`);
    return response.data;
  },
};

export default patternApi;
