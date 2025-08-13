import { AxiosResponse } from 'axios';
import api from './api';

// Analytics API interfaces
export interface TrendDataPoint {
  date: string;
  rawDate: string;
  totalInvoices: number;
  completed: number;
  pending: number;
  processing: number;
  failed: number;
  amount: number;
  averageProcessingTime?: number;
  successRate: number;
}

export interface TrendSummary {
  totalInvoices: number;
  totalAmount: number;
  averageSuccessRate: number;
  averageProcessingTime?: number;
  invoiceTrend: 'UP' | 'DOWN' | 'STABLE' | 'UNKNOWN';
  amountTrend: 'UP' | 'DOWN' | 'STABLE' | 'UNKNOWN';
  successRateTrend: 'UP' | 'DOWN' | 'STABLE' | 'UNKNOWN';
  processingTimeTrend: 'UP' | 'DOWN' | 'STABLE' | 'UNKNOWN';
  period: string;
}

export interface TrendDataResponse {
  title: string;
  description: string;
  granularity: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  startDate: string;
  endDate: string;
  data: TrendDataPoint[];
  summary: TrendSummary;
}

export interface MetricDataPoint {
  date: string;
  rawDate: string;
  value: number;
  average: number;
  minimum: number;
  maximum: number;
  count: number;
  breakdown?: Record<string, any>;
}

export interface MetricsSummary {
  totalValue: number;
  averageValue: number;
  minimumValue: number;
  maximumValue: number;
  standardDeviation?: number;
  trend: 'UP' | 'DOWN' | 'STABLE' | 'UNKNOWN';
  trendPercentage?: number;
  interpretation: string;
  insights?: Array<{
    type: string;
    title: string;
    description: string;
    impact?: number;
    recommendation?: string;
  }>;
}

export interface MetricsTrendResponse {
  metricType: string;
  title: string;
  description: string;
  unit: string;
  granularity: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  startDate: string;
  endDate: string;
  data: MetricDataPoint[];
  summary: MetricsSummary;
}

export interface FilterOption {
  value: string;
  label: string;
  description: string;
  count?: number;
  recommended?: boolean;
  metadata?: Record<string, any>;
}

export interface QuickRange {
  label: string;
  value: string;
  days: number;
  description: string;
}

export interface AnalyticsFilterResponse {
  dateRange: {
    earliestDate: string;
    latestDate: string;
    suggestedStartDate: string;
    suggestedEndDate: string;
    quickRanges: QuickRange[];
  };
  statuses: FilterOption[];
  currencies: FilterOption[];
  vendors: FilterOption[];
  granularities: FilterOption[];
  metricTypes: FilterOption[];
  suggestions: Record<string, any>;
}

export interface AnalyticsParams {
  days?: number;
  startDate?: string;
  endDate?: string;
  granularity?: 'DAILY' | 'WEEKLY' | 'MONTHLY';
  status?: string;
  currency?: string;
}

/**
 * Analytics API service for historical trend data and metrics
 */
export const analyticsService = {
  // Get processing trends
  getProcessingTrends: async (params: AnalyticsParams = {}): Promise<TrendDataResponse> => {
    console.log('📈 Analytics: Fetching processing trends with params:', params);
    const response: AxiosResponse<TrendDataResponse> = await api.get('/api/analytics/trends/processing', { params });
    console.log('✅ Analytics: Processing trends fetched successfully');
    return response.data;
  },

  // Get amount trends
  getAmountTrends: async (params: AnalyticsParams = {}): Promise<MetricsTrendResponse> => {
    console.log('💰 Analytics: Fetching amount trends with params:', params);
    const response: AxiosResponse<MetricsTrendResponse> = await api.get('/api/analytics/trends/amounts', { params });
    console.log('✅ Analytics: Amount trends fetched successfully');
    return response.data;
  },

  // Get processing time trends
  getProcessingTimeTrends: async (params: AnalyticsParams = {}): Promise<MetricsTrendResponse> => {
    console.log('⏱️ Analytics: Fetching processing time trends with params:', params);
    const response: AxiosResponse<MetricsTrendResponse> = await api.get('/api/analytics/trends/processing-time', { params });
    console.log('✅ Analytics: Processing time trends fetched successfully');
    return response.data;
  },

  // Get available filters
  getAvailableFilters: async (): Promise<AnalyticsFilterResponse> => {
    console.log('🔍 Analytics: Fetching available filters');
    const response: AxiosResponse<AnalyticsFilterResponse> = await api.get('/api/analytics/filters/available');
    console.log('✅ Analytics: Available filters fetched successfully');
    return response.data;
  },

  // Get advanced summary
  getAdvancedSummary: async (params: { days?: number; includeTrends?: boolean } = {}): Promise<any> => {
    console.log('📊 Analytics: Fetching advanced summary with params:', params);
    const response: AxiosResponse<any> = await api.get('/api/analytics/summary/advanced', { params });
    console.log('✅ Analytics: Advanced summary fetched successfully');
    return response.data;
  },

  // Export trend data
  exportTrendData: async (params: { 
    format?: 'JSON' | 'CSV'; 
    days?: number; 
    dataType?: 'PROCESSING' | 'AMOUNTS' | 'PROCESSING_TIME';
  } = {}): Promise<string> => {
    console.log('📤 Analytics: Exporting trend data with params:', params);
    const response: AxiosResponse<string> = await api.get('/api/analytics/export/trends', { 
      params,
      responseType: 'text'
    });
    console.log('✅ Analytics: Trend data exported successfully');
    return response.data;
  },

  // Helper methods for common use cases

  // Get last 30 days processing trends
  getLast30DaysTrends: async (): Promise<TrendDataResponse> => {
    return analyticsService.getProcessingTrends({ days: 30, granularity: 'DAILY' });
  },

  // Get weekly summary for last 3 months
  getWeeklySummary: async (): Promise<TrendDataResponse> => {
    return analyticsService.getProcessingTrends({ days: 90, granularity: 'WEEKLY' });
  },

  // Get monthly summary for last year
  getMonthlySummary: async (): Promise<TrendDataResponse> => {
    return analyticsService.getProcessingTrends({ days: 365, granularity: 'MONTHLY' });
  },

  // Get amount trends by currency
  getAmountTrendsByCurrency: async (currency: string, days: number = 30): Promise<MetricsTrendResponse> => {
    return analyticsService.getAmountTrends({ currency, days, granularity: 'DAILY' });
  },

  // Get processing performance metrics
  getProcessingPerformance: async (days: number = 30): Promise<MetricsTrendResponse> => {
    return analyticsService.getProcessingTimeTrends({ days, granularity: 'DAILY' });
  },

  // Get status-specific trends
  getStatusTrends: async (status: string, days: number = 30): Promise<TrendDataResponse> => {
    return analyticsService.getProcessingTrends({ status, days, granularity: 'DAILY' });
  },

  // Get date range trends
  getDateRangeTrends: async (startDate: string, endDate: string, granularity: 'DAILY' | 'WEEKLY' | 'MONTHLY' = 'DAILY'): Promise<TrendDataResponse> => {
    return analyticsService.getProcessingTrends({ startDate, endDate, granularity });
  }
};

export default analyticsService;
