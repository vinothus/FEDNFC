import React, { useState, useEffect } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Area,
  AreaChart
} from 'recharts';
import { analyticsService, TrendDataResponse, AnalyticsParams } from '../../services/analyticsApi';

// Sample data structure for invoice processing trends
interface TrendDataPoint {
  date: string;
  totalInvoices: number;
  completed: number;
  pending: number;
  failed: number;
  amount: number;
}

interface InvoiceProcessingTrendsProps {
  data?: TrendDataResponse;
  loading?: boolean;
  className?: string;
  params?: AnalyticsParams;
}

// Generate sample trend data for the last 30 days
const generateSampleData = (): TrendDataPoint[] => {
  const data: TrendDataPoint[] = [];
  const today = new Date();
  
  for (let i = 29; i >= 0; i--) {
    const date = new Date(today);
    date.setDate(date.getDate() - i);
    
    // Generate realistic random data with some trends
    const baseInvoices = 20 + Math.random() * 30;
    const completedRate = 0.7 + Math.random() * 0.25; // 70-95% completion rate
    const failureRate = 0.02 + Math.random() * 0.08; // 2-10% failure rate
    
    const totalInvoices = Math.floor(baseInvoices + Math.sin(i / 7) * 10); // Weekly pattern
    const completed = Math.floor(totalInvoices * completedRate);
    const failed = Math.floor(totalInvoices * failureRate);
    const pending = totalInvoices - completed - failed;
    const amount = completed * (500 + Math.random() * 2000); // $500-$2500 per invoice
    
    data.push({
      date: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
      totalInvoices,
      completed,
      pending,
      failed,
      amount: Math.round(amount)
    });
  }
  
  return data;
};

/**
 * InvoiceProcessingTrends component with Recharts
 * Shows invoice processing trends over time with multiple metrics
 */
const InvoiceProcessingTrends: React.FC<InvoiceProcessingTrendsProps> = ({
  data,
  loading = false,
  className = '',
  params = {}
}) => {
  const [apiData, setApiData] = useState<TrendDataResponse | null>(null);
  const [apiLoading, setApiLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  // Fetch data from API when component mounts or params change
  useEffect(() => {
    const fetchTrendData = async () => {
      if (data) {
        // Use provided data instead of fetching
        return;
      }

      try {
        setApiLoading(true);
        setApiError(null);
        console.log('📈 InvoiceProcessingTrends: Fetching trend data with params:', params);
        
        const trendData = await analyticsService.getProcessingTrends({
          days: 30,
          granularity: 'DAILY',
          ...params
        });
        
        console.log('✅ InvoiceProcessingTrends: Trend data fetched:', trendData);
        setApiData(trendData);
      } catch (error: any) {
        console.error('❌ InvoiceProcessingTrends: Failed to fetch trend data:', error);
        setApiError(error.message || 'Failed to load trend data');
      } finally {
        setApiLoading(false);
      }
    };

    fetchTrendData();
  }, [data, params]);

  // Use provided data, API data, or generate sample data
  const chartData = data?.data || apiData?.data || generateSampleData();
  const isLoading = loading || apiLoading;

  // Custom tooltip component
  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white p-4 border border-gray-200 rounded-lg shadow-lg">
          <p className="font-medium text-gray-900 mb-2">{`Date: ${label}`}</p>
          {payload.map((entry: any, index: number) => (
            <p key={index} style={{ color: entry.color }} className="text-sm">
              {`${entry.name}: ${entry.value.toLocaleString()}`}
              {entry.dataKey === 'amount' && entry.value > 0 && ` ($${(entry.value / 1000).toFixed(1)}k)`}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  if (isLoading) {
    return (
      <div className={`h-80 flex items-center justify-center ${className}`}>
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (apiError) {
    return (
      <div className={`h-80 flex items-center justify-center ${className}`}>
        <div className="text-center text-red-500">
          <p className="text-sm">Failed to load trend data</p>
          <p className="text-xs text-gray-500 mt-1">{apiError}</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`h-80 ${className}`}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart
          data={chartData}
          margin={{
            top: 10,
            right: 30,
            left: 0,
            bottom: 0,
          }}
        >
          <defs>
            <linearGradient id="completedGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
            </linearGradient>
            <linearGradient id="pendingGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#f59e0b" stopOpacity={0}/>
            </linearGradient>
            <linearGradient id="failedGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#ef4444" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey="date" 
            stroke="#6b7280"
            fontSize={12}
            tickLine={false}
            axisLine={false}
          />
          <YAxis 
            stroke="#6b7280"
            fontSize={12}
            tickLine={false}
            axisLine={false}
          />
          <Tooltip content={<CustomTooltip />} />
          <Legend 
            wrapperStyle={{ paddingTop: '20px' }}
            iconType="circle"
          />
          
          <Area
            type="monotone"
            dataKey="completed"
            stackId="1"
            stroke="#10b981"
            fill="url(#completedGradient)"
            name="Completed"
            strokeWidth={2}
          />
          <Area
            type="monotone"
            dataKey="pending"
            stackId="1"
            stroke="#f59e0b"
            fill="url(#pendingGradient)"
            name="Pending"
            strokeWidth={2}
          />
          <Area
            type="monotone"
            dataKey="failed"
            stackId="1"
            stroke="#ef4444"
            fill="url(#failedGradient)"
            name="Failed"
            strokeWidth={2}
          />
          
          {/* Total line overlay */}
          <Line
            type="monotone"
            dataKey="totalInvoices"
            stroke="#1f2937"
            strokeWidth={3}
            strokeDasharray="5 5"
            dot={{ fill: '#1f2937', strokeWidth: 2, r: 4 }}
            name="Total Invoices"
            connectNulls
          />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
};

export default InvoiceProcessingTrends;
