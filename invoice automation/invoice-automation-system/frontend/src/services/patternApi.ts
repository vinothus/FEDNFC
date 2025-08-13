import axios, { AxiosResponse } from 'axios';
import { 
  InvoicePattern, 
  PatternRequest, 
  PatternResponse, 
  PatternCategory, 
  PatternTestRequest,
  PatternTestResult,
  PatternStats,
  BackendPatternRequest 
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
    // Auto-detect flags needed for complex patterns
    let patternFlags = '';
    if (pattern.regexPattern.includes('(?<=') || pattern.regexPattern.includes('(?=') || pattern.regexPattern.includes('[\\s\\S]')) {
      patternFlags = 'MULTILINE,DOTALL';
    } else if (pattern.category.includes('AMOUNT')) {
      patternFlags = 'CASE_INSENSITIVE';
    }
    
    // Transform frontend request to backend format
    const backendRequest: BackendPatternRequest = {
      patternName: pattern.name,
      patternDescription: pattern.description,
      patternCategory: pattern.category,
      patternRegex: pattern.regexPattern,
      patternPriority: pattern.priority,
      confidenceWeight: 75, // Default confidence weight
      isActive: pattern.isActive,
      patternFlags: patternFlags || undefined,
      dateFormat: pattern.category.includes('DATE') ? 'yyyy-MM-dd' : undefined,
      notes: `Extraction field: ${pattern.extractionField}`
    };
    
    const response: AxiosResponse<PatternResponse> = await patternApi.post('/admin/patterns', backendRequest);
    return response.data;
  },

  // Update existing pattern
  updatePattern: async (id: string, pattern: Partial<PatternRequest>): Promise<PatternResponse> => {
    // Transform frontend request to backend format
    const backendRequest: Partial<BackendPatternRequest> = {};
    
    if (pattern.name !== undefined) backendRequest.patternName = pattern.name;
    if (pattern.description !== undefined) backendRequest.patternDescription = pattern.description;
    if (pattern.category !== undefined) backendRequest.patternCategory = pattern.category;
    if (pattern.regexPattern !== undefined) backendRequest.patternRegex = pattern.regexPattern;
    if (pattern.priority !== undefined) backendRequest.patternPriority = pattern.priority;
    if (pattern.isActive !== undefined) backendRequest.isActive = pattern.isActive;
    if (pattern.extractionField !== undefined) backendRequest.notes = `Extraction field: ${pattern.extractionField}`;
    
    // Set confidence weight if not provided
    if (pattern.priority !== undefined) {
      backendRequest.confidenceWeight = Math.min(pattern.priority + 25, 100);
    }
    
    const response: AxiosResponse<PatternResponse> = await patternApi.put(`/admin/patterns/${id}`, backendRequest);
    return response.data;
  },

  // Delete pattern
  deletePattern: async (id: string): Promise<void> => {
    await patternApi.delete(`/admin/patterns/${id}`);
  },

  // Test pattern against sample text
  testPattern: async (testRequest: PatternTestRequest): Promise<PatternTestResult> => {
    // Transform frontend request to backend format
    const backendRequest = {
      regex: testRequest.pattern,
      sampleText: testRequest.sampleText,
      flags: testRequest.flags || ''
    };
    
    const response: AxiosResponse = await patternApi.post('/admin/patterns/test', backendRequest);
    
    // Transform backend response to frontend format
    const backendResult = response.data;
    
    // Debug: Log the exact backend response structure
    console.log('🔍 PatternApi: Raw backend response:', JSON.stringify(backendResult, null, 2));
    
    // Handle the actual backend response format
    const isValid = backendResult.valid ?? backendResult.isValid ?? false;
    const hasMatches = backendResult.matches ?? false;
    const success = isValid && hasMatches;
    
    console.log('✅ PatternApi: Transformed - isValid:', isValid, 'hasMatches:', hasMatches, 'success:', success);
    
    return {
      sampleText: testRequest.sampleText,
      matches: backendResult.matchedText ? [backendResult.matchedText] : [],
      confidence: success ? 0.9 : 0.0,
      success: success,
      isValid: isValid,
      matchedText: backendResult.matchedText,
      startIndex: backendResult.startIndex,
      endIndex: backendResult.endIndex,
      captureGroups: backendResult.captureGroups,
      errorMessage: backendResult.errorMessage
    };
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
