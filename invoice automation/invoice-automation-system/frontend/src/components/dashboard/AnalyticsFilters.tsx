import React, { useState, useEffect } from 'react';
import { 
  CalendarIcon, 
  FunnelIcon, 
  ArrowPathIcon,
  ChevronDownIcon
} from '@heroicons/react/24/outline';
import { Button, Input } from '../ui';
import { analyticsService, AnalyticsFilterResponse, AnalyticsParams } from '../../services/analyticsApi';

interface AnalyticsFiltersProps {
  onFiltersChange: (filters: AnalyticsParams) => void;
  className?: string;
  initialFilters?: AnalyticsParams;
}

/**
 * AnalyticsFilters component for filtering trend data
 * Provides date range, status, currency, and granularity filters
 */
const AnalyticsFilters: React.FC<AnalyticsFiltersProps> = ({
  onFiltersChange,
  className = '',
  initialFilters = {}
}) => {
  const [filters, setFilters] = useState<AnalyticsParams>(initialFilters);
  const [availableFilters, setAvailableFilters] = useState<AnalyticsFilterResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);

  // Load available filter options
  useEffect(() => {
    const loadFilters = async () => {
      try {
        setLoading(true);
        const filterOptions = await analyticsService.getAvailableFilters();
        setAvailableFilters(filterOptions);
      } catch (error) {
        console.error('Failed to load filter options:', error);
      } finally {
        setLoading(false);
      }
    };

    loadFilters();
  }, []);

  // Handle filter changes
  const handleFilterChange = (key: keyof AnalyticsParams, value: any) => {
    const newFilters = { ...filters, [key]: value };
    setFilters(newFilters);
    onFiltersChange(newFilters);
  };

  // Handle quick range selection
  const handleQuickRange = (range: { days: number; label: string }) => {
    const endDate = new Date().toISOString().split('T')[0];
    const startDate = new Date(Date.now() - range.days * 24 * 60 * 60 * 1000)
      .toISOString().split('T')[0];
    
    const newFilters = { 
      ...filters, 
      days: range.days,
      startDate, 
      endDate 
    };
    setFilters(newFilters);
    onFiltersChange(newFilters);
  };

  // Reset filters
  const handleReset = () => {
    const resetFilters: AnalyticsParams = {
      days: 30,
      granularity: 'DAILY'
    };
    setFilters(resetFilters);
    onFiltersChange(resetFilters);
  };

  return (
    <div className={`bg-white rounded-lg border border-gray-200 p-4 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <FunnelIcon className="h-5 w-5 text-gray-500" />
          <h3 className="text-sm font-medium text-gray-900">Analytics Filters</h3>
        </div>
        <div className="flex items-center space-x-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShowAdvanced(!showAdvanced)}
          >
            <ChevronDownIcon 
              className={`h-4 w-4 transition-transform ${showAdvanced ? 'rotate-180' : ''}`} 
            />
            Advanced
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleReset}
          >
            <ArrowPathIcon className="h-4 w-4 mr-1" />
            Reset
          </Button>
        </div>
      </div>

      {/* Quick Ranges */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-4">
        {availableFilters?.dateRange.quickRanges.map((range) => (
          <Button
            key={range.value}
            variant={filters.days === range.days ? 'primary' : 'secondary'}
            size="sm"
            onClick={() => handleQuickRange(range)}
            className="text-xs"
          >
            {range.label}
          </Button>
        ))}
      </div>

      {/* Basic Filters */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Granularity */}
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Granularity
          </label>
          <select
            value={filters.granularity || 'DAILY'}
            onChange={(e) => handleFilterChange('granularity', e.target.value)}
            className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            {availableFilters?.granularities.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        {/* Status Filter */}
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Status
          </label>
          <select
            value={filters.status || ''}
            onChange={(e) => handleFilterChange('status', e.target.value || undefined)}
            className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Statuses</option>
            {availableFilters?.statuses.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label} ({option.count})
              </option>
            ))}
          </select>
        </div>

        {/* Currency Filter */}
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">
            Currency
          </label>
          <select
            value={filters.currency || ''}
            onChange={(e) => handleFilterChange('currency', e.target.value || undefined)}
            className="w-full px-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Currencies</option>
            {availableFilters?.currencies.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label} ({option.count})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Advanced Filters */}
      {showAdvanced && (
        <div className="mt-4 pt-4 border-t border-gray-200">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Custom Date Range */}
            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                <CalendarIcon className="h-4 w-4 inline mr-1" />
                Start Date
              </label>
              <Input
                type="date"
                value={filters.startDate || ''}
                onChange={(e) => handleFilterChange('startDate', e.target.value || undefined)}
                max={new Date().toISOString().split('T')[0]}
                className="text-sm"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-gray-700 mb-1">
                <CalendarIcon className="h-4 w-4 inline mr-1" />
                End Date
              </label>
              <Input
                type="date"
                value={filters.endDate || ''}
                onChange={(e) => handleFilterChange('endDate', e.target.value || undefined)}
                max={new Date().toISOString().split('T')[0]}
                min={filters.startDate}
                className="text-sm"
              />
            </div>
          </div>

          {/* Filter Summary */}
          <div className="mt-4 p-3 bg-gray-50 rounded-md">
            <h4 className="text-xs font-medium text-gray-700 mb-2">Active Filters:</h4>
            <div className="flex flex-wrap gap-2">
              {Object.entries(filters).map(([key, value]) => {
                if (value) {
                  return (
                    <span
                      key={key}
                      className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-blue-100 text-blue-800"
                    >
                      {key}: {value}
                    </span>
                  );
                }
                return null;
              })}
              {Object.keys(filters).length === 0 && (
                <span className="text-xs text-gray-500">No active filters</span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Loading State */}
      {loading && (
        <div className="flex items-center justify-center py-4">
          <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-600"></div>
          <span className="ml-2 text-sm text-gray-500">Loading filters...</span>
        </div>
      )}
    </div>
  );
};

export default AnalyticsFilters;
