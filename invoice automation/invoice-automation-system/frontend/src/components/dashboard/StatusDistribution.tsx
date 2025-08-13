import React from 'react';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  Legend
} from 'recharts';
import { 
  CheckCircleIcon, 
  ClockIcon, 
  ExclamationTriangleIcon,
  DocumentTextIcon 
} from '@heroicons/react/24/outline';

// Status data structure
interface StatusData {
  name: string;
  value: number;
  color: string;
  icon: React.ReactNode;
  description: string;
}

interface StatusDistributionProps {
  data?: {
    completed: number;
    pending: number;
    processing: number;
    failed: number;
  };
  loading?: boolean;
  className?: string;
}

/**
 * StatusDistribution component with Recharts
 * Shows the distribution of invoice statuses using a pie chart
 */
const StatusDistribution: React.FC<StatusDistributionProps> = ({
  data,
  loading = false,
  className = ''
}) => {
  // Generate sample data if none provided
  const sampleData = {
    completed: 156,
    pending: 23,
    processing: 8,
    failed: 3
  };

  const statusData = data || sampleData;
  
  // Transform data for the pie chart
  const chartData: StatusData[] = [
    {
      name: 'Completed',
      value: statusData.completed,
      color: '#10b981', // Green
      icon: <CheckCircleIcon className="h-5 w-5" />,
      description: 'Successfully processed'
    },
    {
      name: 'Pending',
      value: statusData.pending,
      color: '#f59e0b', // Amber
      icon: <ClockIcon className="h-5 w-5" />,
      description: 'Awaiting processing'
    },
    {
      name: 'Processing',
      value: statusData.processing,
      color: '#3b82f6', // Blue
      icon: <DocumentTextIcon className="h-5 w-5" />,
      description: 'Currently being processed'
    },
    {
      name: 'Failed',
      value: statusData.failed,
      color: '#ef4444', // Red
      icon: <ExclamationTriangleIcon className="h-5 w-5" />,
      description: 'Processing failed'
    }
  ].filter(item => item.value > 0); // Only show statuses with values > 0

  const total = chartData.reduce((sum, item) => sum + item.value, 0);

  // Custom tooltip component
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      const percentage = ((data.value / total) * 100).toFixed(1);
      
      return (
        <div className="bg-white p-4 border border-gray-200 rounded-lg shadow-lg">
          <div className="flex items-center space-x-2 mb-2">
            <div 
              className="w-3 h-3 rounded-full" 
              style={{ backgroundColor: data.color }}
            />
            <p className="font-medium text-gray-900">{data.name}</p>
          </div>
          <p className="text-sm text-gray-600 mb-1">{data.description}</p>
          <p className="text-sm font-medium">
            {data.value.toLocaleString()} invoices ({percentage}%)
          </p>
        </div>
      );
    }
    return null;
  };

  // Custom legend component
  const CustomLegend = ({ payload }: any) => {
    return (
      <div className="flex flex-col space-y-2 mt-4">
        {payload.map((entry: any, index: number) => {
          const data = chartData.find(item => item.name === entry.value);
          const percentage = ((data?.value || 0) / total * 100).toFixed(1);
          
          return (
            <div key={index} className="flex items-center justify-between text-sm">
              <div className="flex items-center space-x-2">
                <div 
                  className="w-3 h-3 rounded-full" 
                  style={{ backgroundColor: entry.color }}
                />
                <div className="flex items-center space-x-1 text-gray-600">
                  {data?.icon}
                  <span>{entry.value}</span>
                </div>
              </div>
              <div className="flex items-center space-x-2 text-right">
                <span className="font-medium text-gray-900">
                  {data?.value.toLocaleString()}
                </span>
                <span className="text-gray-500 text-xs">
                  ({percentage}%)
                </span>
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  if (loading) {
    return (
      <div className={`h-80 flex items-center justify-center ${className}`}>
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (total === 0) {
    return (
      <div className={`h-80 flex items-center justify-center ${className}`}>
        <div className="text-center text-gray-500">
          <DocumentTextIcon className="mx-auto h-12 w-12 text-gray-400 mb-2" />
          <p>No invoice data available</p>
        </div>
      </div>
    );
  }

  return (
    <div className={`h-80 ${className}`}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={chartData}
            cx="50%"
            cy="40%"
            innerRadius={45}
            outerRadius={80}
            paddingAngle={2}
            dataKey="value"
            stroke="none"
          >
            {chartData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip content={<CustomTooltip />} />
          <Legend 
            content={<CustomLegend />}
            wrapperStyle={{ paddingTop: '10px' }}
          />
        </PieChart>
      </ResponsiveContainer>
      
      {/* Summary stats */}
      <div className="border-t border-gray-200 pt-3 mt-2">
        <div className="flex justify-between items-center text-sm">
          <span className="text-gray-600">Total Invoices</span>
          <span className="font-semibold text-gray-900">{total.toLocaleString()}</span>
        </div>
        <div className="flex justify-between items-center text-sm mt-1">
          <span className="text-gray-600">Success Rate</span>
          <span className="font-semibold text-green-600">
            {total > 0 ? ((statusData.completed / total) * 100).toFixed(1) : 0}%
          </span>
        </div>
      </div>
    </div>
  );
};

export default StatusDistribution;
