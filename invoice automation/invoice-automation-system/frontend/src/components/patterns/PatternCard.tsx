import React from 'react';
import { format } from 'date-fns';
import { 
  PencilIcon, 
  TrashIcon,
  PlayIcon,
  PauseIcon,
  ChartBarIcon 
} from '@heroicons/react/24/outline';
import { InvoicePattern } from '../../types/pattern';
import { Button } from '../ui';
import { cn } from '../../utils/cn';

export interface PatternCardProps {
  pattern: InvoicePattern;
  onEdit: (pattern: InvoicePattern) => void;
  onDelete: (pattern: InvoicePattern) => void;
  onToggleStatus: (pattern: InvoicePattern) => void;
  onTest: (pattern: InvoicePattern) => void;
  onViewDetails: (pattern: InvoicePattern) => void;
  className?: string;
}

/**
 * PatternCard component following React UI Cursor Rules
 * - Displays pattern information in a card layout
 * - Accessible with proper button labels and keyboard navigation
 * - Responsive design with mobile-first approach
 * - Status indicators with appropriate colors
 * - Quick action buttons
 */
const PatternCard: React.FC<PatternCardProps> = ({
  pattern,
  onEdit,
  onDelete,
  onToggleStatus,
  onTest,
  onViewDetails,
  className,
}) => {
  const getStatusColor = (isActive: boolean) => {
    return isActive ? 'text-green-600 bg-green-100' : 'text-gray-600 bg-gray-100';
  };

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.8) return 'text-green-600';
    if (confidence >= 0.6) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getCategoryColor = (category: string) => {
    if (!category) return 'bg-gray-100 text-gray-800';
    
    const colors = {
      'amount': 'bg-blue-100 text-blue-800',
      'invoice_date': 'bg-green-100 text-green-800',
      'date': 'bg-green-100 text-green-800',
      'vendor': 'bg-purple-100 text-purple-800',
      'invoice_number': 'bg-yellow-100 text-yellow-800',
      'invoice': 'bg-yellow-100 text-yellow-800',
      'address': 'bg-pink-100 text-pink-800',
      'customer': 'bg-indigo-100 text-indigo-800',
      'email': 'bg-cyan-100 text-cyan-800',
      'phone': 'bg-orange-100 text-orange-800',
      'currency': 'bg-emerald-100 text-emerald-800',
      'default': 'bg-gray-100 text-gray-800',
    };
    return colors[category.toLowerCase() as keyof typeof colors] || colors.default;
  };

  return (
    <div 
      className={cn(
        'bg-white rounded-lg shadow-sm border border-gray-200 p-6',
        'hover:shadow-md transition-shadow duration-200',
        className
      )}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1 min-w-0">
          <h3 className="text-lg font-medium text-gray-900 truncate">
            {(pattern as any).patternName || pattern.name || 'Unnamed Pattern'}
          </h3>
          {pattern.description && (
            <p className="text-sm text-gray-600 mt-1 line-clamp-2">
              {pattern.description}
            </p>
          )}
        </div>
        
        <div className="flex items-center space-x-2 ml-4">
          <span className={cn(
            'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
            getStatusColor(pattern.isActive)
          )}>
            {pattern.isActive ? 'Active' : 'Inactive'}
          </span>
          
          <span className={cn(
            'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
            getCategoryColor((pattern as any).patternCategory || pattern.category)
          )}>
            {(pattern as any).patternCategory || pattern.category || 'Unknown'}
          </span>
        </div>
      </div>

      {/* Pattern Info */}
      <div className="space-y-3">
        {/* Regex Pattern */}
        <div>
          <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
            Pattern
          </label>
          <div className="mt-1 bg-gray-50 rounded-md p-3">
            <code className="text-sm font-mono text-gray-900 break-all">
              {(pattern as any).patternRegex || pattern.regexPattern || 'No pattern defined'}
            </code>
          </div>
        </div>

        {/* Extraction Field */}
        <div>
          <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">
            Extracts
          </label>
          <p className="text-sm text-gray-900 mt-1">
            {pattern.extractionField}
          </p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4 pt-3 border-t border-gray-200">
          <div className="text-center">
            <p className="text-sm font-medium text-gray-900">
              {(pattern as any).patternPriority || pattern.priority || 'N/A'}
            </p>
            <p className="text-xs text-gray-500">
              Priority
            </p>
          </div>
          
          <div className="text-center">
            <p className="text-sm font-medium text-gray-900">
              {pattern.usageCount || 0}
            </p>
            <p className="text-xs text-gray-500">
              Uses
            </p>
          </div>
          
          <div className="text-center">
            <p className="text-sm font-medium text-gray-900">
              {(pattern as any).isActive ? 'Yes' : 'No'}
            </p>
            <p className="text-xs text-gray-500">
              Active
            </p>
          </div>
        </div>

        {/* Last Used */}
        {pattern.lastUsed && (
          <div className="text-xs text-gray-500">
            Last used: {format(new Date(pattern.lastUsed), 'MMM dd, yyyy')}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between pt-4 border-t border-gray-200 mt-4">
        <div className="flex items-center space-x-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onTest(pattern)}
            aria-label={`Test pattern ${pattern.name}`}
          >
            <PlayIcon className="h-4 w-4 mr-1" />
            Test
          </Button>
          
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onViewDetails(pattern)}
            aria-label={`View details for ${pattern.name}`}
          >
            <ChartBarIcon className="h-4 w-4 mr-1" />
            Stats
          </Button>
        </div>

        <div className="flex items-center space-x-1">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onToggleStatus(pattern)}
            aria-label={`${pattern.isActive ? 'Deactivate' : 'Activate'} pattern ${pattern.name}`}
          >
            {pattern.isActive ? (
              <PauseIcon className="h-4 w-4" />
            ) : (
              <PlayIcon className="h-4 w-4" />
            )}
          </Button>
          
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onEdit(pattern)}
            aria-label={`Edit pattern ${pattern.name}`}
          >
            <PencilIcon className="h-4 w-4" />
          </Button>
          
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onDelete(pattern)}
            aria-label={`Delete pattern ${pattern.name}`}
            className="text-red-600 hover:text-red-700 hover:bg-red-50"
          >
            <TrashIcon className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  );
};

export default PatternCard;
