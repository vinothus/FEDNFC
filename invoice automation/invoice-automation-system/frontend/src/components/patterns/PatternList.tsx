import React, { useState, useEffect, useCallback } from 'react';
import { 
  PlusIcon, 
  FunnelIcon, 
  MagnifyingGlassIcon,
  ArrowPathIcon 
} from '@heroicons/react/24/outline';
import { InvoicePattern, PatternCategory } from '../../types/pattern';
import { patternService } from '../../services/patternApi';
import { Button, Input, Alert, Loading } from '../ui';
import PatternCard from './PatternCard';
import PatternForm from './PatternForm';

export interface PatternListProps {
  className?: string;
}

/**
 * PatternList component following React UI Cursor Rules
 * - Paginated list of patterns with search and filtering
 * - Responsive grid layout
 * - Loading and error states
 * - Bulk operations support
 * - Accessible with proper ARIA labels
 */
const PatternList: React.FC<PatternListProps> = ({ className }) => {
  const [patterns, setPatterns] = useState<InvoicePattern[]>([]);
  const [categories, setCategories] = useState<PatternCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [showActiveOnly, setShowActiveOnly] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  
  // Form state
  const [showForm, setShowForm] = useState(false);
  const [formMode, setFormMode] = useState<'create' | 'edit'>('create');
  const [selectedPattern, setSelectedPattern] = useState<InvoicePattern | undefined>();

  const fetchPatterns = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await patternService.getPatterns({
        page: currentPage,
        size: 12,
        category: selectedCategory || undefined,
        active: showActiveOnly || undefined,
      });
      
      console.log('📦 PatternList: API response received:', response);
      
      // Filter by search term on frontend (could be moved to backend)
      const responseAny = response as any;
      let filteredPatterns = responseAny.content || response.patterns || [];
      if (searchTerm && (responseAny.content || response.patterns)) {
        const patternsArray = responseAny.content || response.patterns;
        filteredPatterns = patternsArray.filter((pattern: any) =>
          pattern.patternName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          pattern.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          pattern.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
          pattern.extractionField?.toLowerCase().includes(searchTerm.toLowerCase())
        );
      }
      
      console.log('🎯 PatternList: Setting patterns:', filteredPatterns);
      setPatterns(filteredPatterns);
      setTotalPages(response.totalPages || 0);
    } catch (err: any) {
      console.error('❌ PatternList: Error fetching patterns:', err);
      setError(err.message || 'Failed to load patterns');
      setPatterns([]); // Clear patterns on error
    } finally {
      setLoading(false);
    }
  }, [currentPage, selectedCategory, showActiveOnly, searchTerm]);

  const fetchCategories = useCallback(async () => {
    try {
      console.log('📂 PatternList: Fetching categories...');
      const categoryData = await patternService.getCategories();
      console.log('📂 PatternList: Raw category data:', categoryData);
      
      // Backend returns simple string array, transform to objects with name and count
      const categoriesWithCounts: PatternCategory[] = (categoryData as unknown as string[]).map((categoryName: string) => ({
        id: categoryName.toLowerCase(),
        name: categoryName.toLowerCase(),
        displayName: categoryName.split('_').map(word => 
          word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
        ).join(' '),
        count: 0 // TODO: Get actual counts from backend
      }));
      
      console.log('📂 PatternList: Processed categories:', categoriesWithCounts);
      setCategories(categoriesWithCounts);
    } catch (err) {
      console.error('❌ PatternList: Failed to load categories:', err);
    }
  }, []);

  useEffect(() => {
    fetchPatterns();
  }, [fetchPatterns]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const handleCreatePattern = () => {
    setFormMode('create');
    setSelectedPattern(undefined);
    setShowForm(true);
  };

  const handleEditPattern = (pattern: InvoicePattern) => {
    console.log('✏️ PatternList: Editing pattern:', pattern);
    setFormMode('edit');
    setSelectedPattern(pattern);
    setShowForm(true);
  };

  const handleDeletePattern = async (pattern: InvoicePattern) => {
    if (!window.confirm(`Are you sure you want to delete "${pattern.name}"? This action cannot be undone.`)) {
      return;
    }

    try {
      await patternService.deletePattern(pattern.id);
      await fetchPatterns(); // Refresh the list
    } catch (err: any) {
      alert(`Failed to delete pattern: ${err.message}`);
    }
  };

  const handleToggleStatus = async (pattern: InvoicePattern) => {
    try {
      await patternService.togglePatternStatus(pattern.id);
      await fetchPatterns(); // Refresh the list
    } catch (err: any) {
      alert(`Failed to toggle pattern status: ${err.message}`);
    }
  };

  const handleTestPattern = (pattern: InvoicePattern) => {
    // TODO: Implement pattern testing modal
    alert(`Pattern testing for "${pattern.name}" - Feature coming soon!`);
  };

  const handleViewDetails = (pattern: InvoicePattern) => {
    // TODO: Implement pattern details/stats modal
    alert(`Pattern details for "${pattern.name}" - Feature coming soon!`);
  };

  const handleFormSuccess = () => {
    console.log('✅ PatternList: Form success, refreshing current page:', currentPage);
    // Don't reset pagination - just refresh current page
    fetchPatterns(); // This will maintain currentPage due to dependency
  };

  const handleRefresh = () => {
    setCurrentPage(0);
    fetchPatterns();
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
    setCurrentPage(0); // Reset to first page when searching
  };

  const handleCategoryChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedCategory(e.target.value);
    setCurrentPage(0); // Reset to first page when filtering
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  return (
    <div className={className}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Pattern Management</h1>
          <p className="mt-1 text-sm text-gray-600">
            Manage OCR extraction patterns for invoice processing
          </p>
        </div>
        <div className="mt-4 sm:mt-0">
          <Button
            variant="primary"
            size="sm"
            onClick={handleCreatePattern}
            className="w-full sm:w-auto"
          >
            <PlusIcon className="h-4 w-4 mr-2" />
            Create Pattern
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {/* Search */}
          <Input
            placeholder="Search patterns..."
            value={searchTerm}
            onChange={handleSearchChange}
            startIcon={<MagnifyingGlassIcon />}
            fullWidth
          />

          {/* Category Filter */}
          <div>
            <select
              value={selectedCategory}
              onChange={handleCategoryChange}
              className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">All Categories</option>
              {categories.map((category) => (
                <option key={category.id} value={category.name}>
                  {category.displayName || category.name || 'Unknown'} ({category.count || 0})
                </option>
              ))}
            </select>
          </div>

          {/* Active Filter */}
          <div className="flex items-center space-x-3">
            <input
              id="activeOnly"
              type="checkbox"
              checked={showActiveOnly}
              onChange={(e) => setShowActiveOnly(e.target.checked)}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label htmlFor="activeOnly" className="text-sm text-gray-700">
              Active Only
            </label>
          </div>

          {/* Refresh Button */}
          <div className="flex justify-end">
            <Button
              variant="secondary"
              size="sm"
              onClick={handleRefresh}
              loading={loading}
            >
              <ArrowPathIcon className="h-4 w-4 mr-2" />
              Refresh
            </Button>
          </div>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <Alert variant="error" className="mb-6" onDismiss={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Loading State */}
      {loading ? (
        <div className="flex justify-center items-center py-12">
          <Loading size="lg" />
        </div>
      ) : (
        <>
          {/* Results Summary */}
          {!loading && patterns && patterns.length > 0 && (
            <div className="mb-4 text-sm text-gray-600">
              {searchTerm || selectedCategory || showActiveOnly
                ? `Found ${patterns.length} patterns`
                : `Showing ${patterns.length} patterns`
              }
              {(searchTerm || selectedCategory || showActiveOnly) && (
                <button
                  onClick={handleCreatePattern}
                  className="ml-2 text-blue-600 hover:text-blue-500"
                >
                  Clear filters
                </button>
              )}
            </div>
          )}

          {/* Empty State */}
          {!loading && (!patterns || patterns.length === 0) && (
            <div className="text-center py-12">
              <div className="mx-auto h-12 w-12 text-gray-400">
                <FunnelIcon />
              </div>
              <h3 className="mt-2 text-sm font-semibold text-gray-900">
                No patterns found
              </h3>
              <p className="mt-1 text-sm text-gray-500">
                {searchTerm || selectedCategory || showActiveOnly
                  ? 'Try adjusting your search or filter criteria.'
                  : 'Get started by creating your first extraction pattern.'}
              </p>
              <div className="mt-6">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleCreatePattern}
                >
                  <PlusIcon className="h-4 w-4 mr-2" />
                  Create Pattern
                </Button>
              </div>
            </div>
          )}

          {/* Pattern Grid */}
          {!loading && patterns && patterns.length > 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
              {patterns.map((pattern) => (
                <PatternCard
                  key={pattern.id}
                  pattern={pattern}
                  onEdit={handleEditPattern}
                  onDelete={handleDeletePattern}
                  onToggleStatus={handleToggleStatus}
                  onTest={handleTestPattern}
                  onViewDetails={handleViewDetails}
                />
              ))}
            </div>
          )}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-6 flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1}
                >
                  Next
                </Button>
              </div>

              <div className="text-sm text-gray-700">
                Page {currentPage + 1} of {totalPages}
              </div>

              {/* Page numbers */}
              <div className="flex items-center space-x-1">
                {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                  const pageNumber = Math.max(0, Math.min(totalPages - 5, currentPage - 2)) + i;
                  return (
                    <Button
                      key={pageNumber}
                      variant={pageNumber === currentPage ? 'primary' : 'secondary'}
                      size="sm"
                      onClick={() => handlePageChange(pageNumber)}
                    >
                      {pageNumber + 1}
                    </Button>
                  );
                })}
              </div>
            </div>
          )}
        </>
      )}

      {/* Pattern Form Modal */}
      <PatternForm
        isOpen={showForm}
        onClose={() => {
          console.log('🔒 PatternList: Closing form modal');
          setShowForm(false);
          setSelectedPattern(undefined);
        }}
        onSuccess={handleFormSuccess}
        pattern={selectedPattern}
        mode={formMode}
      />
    </div>
  );
};

export default PatternList;