import axios, { AxiosResponse } from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/invoice-automation';

const dashboardApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add auth token
dashboardApi.interceptors.request.use(
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
dashboardApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear tokens on unauthorized
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      
      // Redirect to login if not already there
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/invoice-automation/login';
      }
    }
    
    return Promise.reject(error);
  }
);

// Dashboard API interfaces
export interface DashboardSummary {
  overallStats: OverallStats;
  processingStats: ProcessingStats;
  statusBreakdown: StatusBreakdown;
  systemHealth: SystemHealth;
  recentInvoices: RecentInvoice[];
}

export interface OverallStats {
  totalInvoices: number;
  totalAmount: number;
  avgProcessingTime: number;
  successRate: number;
  totalVendors: number;
}

export interface ProcessingStats {
  todayCount: number;
  weekCount: number;
  monthCount: number;
  todayAmount: number;
  weekAmount: number;
  monthAmount: number;
}

export interface StatusBreakdown {
  completed: number;
  inProgress: number;
  failed: number;
  pendingReview: number;
}

export interface SystemHealth {
  emailSchedulerStatus: string;
  lastEmailCheck: string;
  ocrQueueSize: number;
  databaseStatus: string;
  avgOcrConfidence: number;
}

export interface RecentInvoice {
  id: number;
  invoiceNumber: string;
  vendorName: string;
  totalAmount: number;
  currency: string;
  invoiceDate: string;
  receivedDate: string;
  processingStatus: string;
  ocrStatus: string;
  ocrConfidence: number | null;
  emailSubject: string | null;
  senderEmail: string;
  originalFilename: string;
  fileSize: number;
  downloadUrl: string | null;
}

export const dashboardService = {
  // Get complete dashboard summary
  getDashboardSummary: async (): Promise<DashboardSummary> => {
    const response: AxiosResponse<DashboardSummary> = await dashboardApi.get('/dashboard/summary');
    return response.data;
  },

  // Get overall statistics
  getOverallStats: async (): Promise<OverallStats> => {
    const response: AxiosResponse<OverallStats> = await dashboardApi.get('/dashboard/overall-stats');
    return response.data;
  },

  // Get processing statistics
  getProcessingStats: async (): Promise<ProcessingStats> => {
    const response: AxiosResponse<ProcessingStats> = await dashboardApi.get('/dashboard/processing-stats');
    return response.data;
  },

  // Get recent invoices
  getRecentInvoices: async (limit: number = 10): Promise<RecentInvoice[]> => {
    const response: AxiosResponse<RecentInvoice[]> = await dashboardApi.get(`/dashboard/recent-invoices?limit=${limit}`);
    return response.data;
  },

  // Get status breakdown
  getStatusBreakdown: async (): Promise<StatusBreakdown> => {
    const response: AxiosResponse<StatusBreakdown> = await dashboardApi.get('/dashboard/status-breakdown');
    return response.data;
  },

  // Get system health (admin only)
  getSystemHealth: async (): Promise<SystemHealth> => {
    const response: AxiosResponse<SystemHealth> = await dashboardApi.get('/dashboard/system-health');
    return response.data;
  },

  // Get invoices by status
  getInvoicesByStatus: async (status: string, limit: number = 20): Promise<RecentInvoice[]> => {
    const response: AxiosResponse<RecentInvoice[]> = await dashboardApi.get(`/dashboard/invoices/by-status/${status}?limit=${limit}`);
    return response.data;
  },

  // Get processing stats for date range
  getProcessingStatsForRange: async (startDate: string, endDate: string): Promise<ProcessingStats> => {
    const response: AxiosResponse<ProcessingStats> = await dashboardApi.get(
      `/dashboard/processing-stats/range?startDate=${startDate}&endDate=${endDate}`
    );
    return response.data;
  },
};

export default dashboardApi;
