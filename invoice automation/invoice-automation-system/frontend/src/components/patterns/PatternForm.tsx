import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { XMarkIcon, PlusIcon, BeakerIcon, CheckCircleIcon, XCircleIcon } from '@heroicons/react/24/outline';
import { Button, Input, Modal, Alert } from '../ui';
import { InvoicePattern, PatternRequest, PatternTestRequest, PatternTestResult } from '../../types/pattern';
import { patternService } from '../../services/patternApi';

// Validation schema
const patternSchema = yup.object({
  name: yup
    .string()
    .required('Pattern name is required')
    .min(3, 'Name must be at least 3 characters'),
  description: yup.string(),
  category: yup
    .string()
    .required('Category is required'),
  regexPattern: yup
    .string()
    .required('Regex pattern is required')
    .test('valid-regex', 'Invalid regular expression', (value) => {
      if (!value) return false;
      try {
        new RegExp(value);
        return true;
      } catch {
        return false;
      }
    }),
  extractionField: yup
    .string()
    .required('Extraction field is required'),
  isActive: yup.boolean().default(true),
  priority: yup
    .number()
    .required('Priority is required')
    .min(1, 'Priority must be at least 1')
    .max(100, 'Priority must be at most 100'),
  examples: yup.array().of(yup.string()),
});

type PatternFormData = yup.InferType<typeof patternSchema>;

export interface PatternFormProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  pattern?: InvoicePattern;
  mode: 'create' | 'edit';
}

/**
 * PatternForm component following React UI Cursor Rules
 * - Modal form for creating/editing patterns
 * - Comprehensive validation with yup
 * - Dynamic examples array management
 * - Real-time regex validation
 * - Accessible form with proper labels
 */
const PatternForm: React.FC<PatternFormProps> = ({
  isOpen,
  onClose,
  onSuccess,
  pattern,
  mode,
}) => {
  const [categories, setCategories] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Pattern testing state
  const [sampleText, setSampleText] = useState('');
  const [testResults, setTestResults] = useState<PatternTestResult | null>(null);
  const [testing, setTesting] = useState(false);
  const [testError, setTestError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
    watch,
  } = useForm<PatternFormData>({
    resolver: yupResolver(patternSchema) as any,
    defaultValues: {
      name: '',
      description: '',
      category: '',
      regexPattern: '',
      extractionField: '',
      isActive: true,
      priority: 50,
      examples: [],
    },
  });

  // Simplified examples handling (remove useFieldArray for now)
  const [examples, setExamples] = useState<string[]>([]);

  const regexPattern = watch('regexPattern');

  // Pattern testing functionality
  const handleTestPattern = async () => {
    if (!regexPattern || !sampleText) {
      setTestError('Please provide both a regex pattern and sample text');
      return;
    }

    if (!testRegex(regexPattern)) {
      setTestError('Invalid regular expression. Please fix the regex pattern before testing.');
      return;
    }

    try {
      setTesting(true);
      setTestError(null);
      setTestResults(null);

      console.log('🧪 PatternForm: Testing pattern:', { pattern: regexPattern, sampleText: sampleText.substring(0, 100) + '...' });

      // Auto-detect flags needed for complex patterns
      let flags = '';
      if (regexPattern.includes('(?<=') || regexPattern.includes('(?=') || regexPattern.includes('[\\s\\S]')) {
        flags = 'MULTILINE,DOTALL';
      }
      
      const testRequest: PatternTestRequest = {
        pattern: regexPattern,
        sampleText: sampleText,
        flags: flags,
      };

      const result = await patternService.testPattern(testRequest);
      console.log('✅ PatternForm: Test result:', result);
      setTestResults(result);
      
      // Clear any previous errors since test was successful
      setTestError(null);
    } catch (err: any) {
      console.error('❌ PatternForm: Test failed:', err);
      setTestError(err.message || 'Failed to test pattern. Please check your regex and try again.');
      setTestResults(null);
    } finally {
      setTesting(false);
    }
  };

  // Reset test results when pattern changes
  useEffect(() => {
    setTestResults(null);
    setTestError(null);
  }, [regexPattern]);

  // Cleanup when modal closes
  useEffect(() => {
    if (!isOpen) {
      console.log('🧹 PatternForm: Cleaning up form state on close');
      setTestResults(null);
      setTestError(null);
      setSampleText('');
      setError(null);
      setExamples([]);
    }
  }, [isOpen]);

  // Load categories on mount
  useEffect(() => {
    const loadCategories = async () => {
      try {
        const categoryData = await patternService.getCategories();
        console.log('📦 PatternForm: Category data received:', categoryData);
        const categoryStrings = categoryData
          .map(cat => typeof cat === 'string' ? cat : cat?.name)
          .filter((name): name is string => Boolean(name));
        setCategories(categoryStrings);
      } catch (error) {
        console.error('Failed to load categories:', error);
        // Fallback categories
        setCategories(['AMOUNT', 'INVOICE_DATE', 'VENDOR', 'INVOICE_NUMBER', 'CUSTOMER', 'EMAIL', 'PHONE', 'CURRENCY']);
      }
    };

    if (isOpen) {
      loadCategories();
    }
  }, [isOpen]);

  // Set form values when editing
  useEffect(() => {
    if (pattern && mode === 'edit') {
      console.log('📝 PatternForm: Loading pattern for edit:', pattern);
      
      // Handle different field name formats from API response
      const patternAny = pattern as any;
      
      const formData = {
        name: patternAny.patternName || pattern.name || '',
        description: patternAny.patternDescription || pattern.description || '',
        category: patternAny.patternCategory || pattern.category || '',
        regexPattern: patternAny.patternRegex || pattern.regexPattern || '',
        extractionField: patternAny.extractionField || pattern.extractionField || '',
        isActive: patternAny.isActive !== undefined ? patternAny.isActive : pattern.isActive !== undefined ? pattern.isActive : true,
        priority: patternAny.patternPriority || pattern.priority || 50,
      };
      
      console.log('📝 PatternForm: Mapped form data:', formData);
      
      reset(formData);
      setExamples(pattern.examples || []);
      
      // Clear any previous test results when loading a pattern
      setTestResults(null);
      setTestError(null);
      setSampleText('');
    } else if (mode === 'create') {
      console.log('📝 PatternForm: Resetting form for create mode');
      reset({
        name: '',
        description: '',
        category: '',
        regexPattern: '',
        extractionField: '',
        isActive: true,
        priority: 50,
      });
      setExamples([]);
      setTestResults(null);
      setTestError(null);
      setSampleText('');
    }
  }, [pattern, mode, reset]);

  const onSubmit = async (data: PatternFormData) => {
    try {
      setLoading(true);
      setError(null);

      // Validation: For new patterns, require successful test
      if (mode === 'create') {
        if (!testResults) {
          setError('Please test your pattern before saving. Use the "Test Pattern" section to validate your regex.');
          return;
        }
        
        if (!testResults.success) {
          setError('Pattern test failed. Please ensure your regex pattern matches the sample text before saving.');
          return;
        }

        if (testResults.confidence < 0.5) {
          const confirmLowConfidence = window.confirm(
            `Pattern confidence is low (${Math.round(testResults.confidence * 100)}%). ` +
            'This may result in unreliable extractions. Do you want to continue?'
          );
          if (!confirmLowConfidence) {
            return;
          }
        }
      }

      // Validation: For existing patterns, strongly recommend testing if pattern changed
      if (mode === 'edit' && pattern && data.regexPattern !== pattern.regexPattern) {
        if (!testResults) {
          const confirmWithoutTest = window.confirm(
            'You have modified the regex pattern but haven\'t tested it. ' +
            'It is strongly recommended to test the pattern before saving. ' +
            'Do you want to save without testing?'
          );
          if (!confirmWithoutTest) {
            setError('Please test your modified pattern using the "Test Pattern" section.');
            return;
          }
        } else if (!testResults.success) {
          const confirmFailedTest = window.confirm(
            'Your pattern test failed, but you can still save the pattern. ' +
            'This may cause issues with data extraction. Do you want to continue?'
          );
          if (!confirmFailedTest) {
            return;
          }
        }
      }

      const patternData: PatternRequest = {
        name: data.name,
        description: data.description,
        category: data.category,
        regexPattern: data.regexPattern,
        extractionField: data.extractionField,
        isActive: data.isActive,
        priority: data.priority,
        examples: examples.filter(Boolean),
      };

      if (mode === 'edit' && pattern) {
        console.log('✏️ PatternForm: Updating pattern:', pattern.id);
        await patternService.updatePattern(pattern.id, patternData);
        console.log('✅ PatternForm: Pattern updated successfully');
      } else {
        console.log('➕ PatternForm: Creating new pattern');
        await patternService.createPattern(patternData);
        console.log('✅ PatternForm: Pattern created successfully');
      }

      // Reset form state and close
      reset();
      setExamples([]);
      setTestResults(null);
      setTestError(null);
      setSampleText('');
      setError(null);
      
      onSuccess();
      onClose();
    } catch (err: any) {
      console.error('❌ PatternForm: Error saving pattern:', err);
      setError(err.message || 'Failed to save pattern');
    } finally {
      setLoading(false);
    }
  };

  const handleAddExample = () => {
    setExamples(prev => [...prev, '']);
  };

  const handleRemoveExample = (index: number) => {
    setExamples(prev => prev.filter((_, i) => i !== index));
  };

  const handleExampleChange = (index: number, value: string) => {
    setExamples(prev => prev.map((example, i) => i === index ? value : example));
  };

  const testRegex = (pattern: string) => {
    try {
      new RegExp(pattern);
      return true;
    } catch {
      return false;
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={mode === 'edit' ? 'Edit Pattern' : 'Create New Pattern'}
      size="lg"
      closeOnOverlayClick={false}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        {error && (
          <Alert variant="error" dismissible onDismiss={() => setError(null)}>
            {error}
          </Alert>
        )}

        {/* Basic Information */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
          <Input
            {...register('name')}
            label="Pattern Name"
            placeholder="Enter pattern name"
            error={errors.name?.message}
            disabled={loading || isSubmitting}
            fullWidth
          />

          <div>
            <label htmlFor="category" className="block text-sm font-medium text-gray-700">
              Category
            </label>
            <select
              {...register('category')}
              id="category"
              className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              disabled={loading || isSubmitting}
            >
              <option value="">Select a category</option>
              {(categories || []).map((category) => (
                <option key={category} value={category}>
                  {category ? category.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()) : 'Unknown'}
                </option>
              ))}
            </select>
            {errors.category && (
              <p className="mt-1 text-sm text-red-600">{errors.category.message}</p>
            )}
          </div>
        </div>

        <Input
          {...register('description')}
          label="Description"
          placeholder="Describe what this pattern extracts"
          error={errors.description?.message}
          disabled={loading || isSubmitting}
          fullWidth
        />

        {/* Pattern Configuration */}
        <div>
          <Input
            {...register('regexPattern')}
            label="Regular Expression Pattern"
            placeholder="Enter regex pattern"
            error={errors.regexPattern?.message}
            disabled={loading || isSubmitting}
            fullWidth
            helperText={
              regexPattern && testRegex(regexPattern) 
                ? '✓ Valid regular expression' 
                : regexPattern 
                ? '✗ Invalid regular expression' 
                : undefined
            }
          />
        </div>

        {/* Pattern Testing Section */}
        {regexPattern && testRegex(regexPattern) && (
          <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-medium text-gray-900 flex items-center">
                <BeakerIcon className="h-4 w-4 mr-2 text-blue-500" />
                Test Pattern
                {mode === 'create' && (
                  <span className="ml-2 text-xs bg-amber-100 text-amber-800 px-2 py-1 rounded">
                    Required for new patterns
                  </span>
                )}
              </h3>
            </div>
            
            <div className="space-y-4">
              <div>
                <label htmlFor="sampleText" className="block text-sm font-medium text-gray-700 mb-2">
                  Sample Text
                  <span className="text-xs text-gray-500 font-normal ml-1">
                    (Use text that contains the data you want to extract)
                  </span>
                </label>
                
                {/* Quick sample options */}
                <div className="flex flex-wrap gap-2 mb-2">
                  <button
                    type="button"
                    onClick={() => setSampleText('Total: $1,146.48\nBalance Due: $1,146.48\nSubtotal: $1,053.27')}
                    className="text-xs px-2 py-1 bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors"
                    disabled={testing}
                  >
                    Total Sample (USD)
                  </button>
                  <button
                    type="button"
                    onClick={() => setSampleText('Gross Amount incl. VAT 453,53\nTotal 381,12\nVAT 19 % 72,41')}
                    className="text-xs px-2 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200 transition-colors"
                    disabled={testing}
                  >
                    Total Sample (EUR)
                  </button>
                  <button
                    type="button"
                    onClick={() => setSampleText('Invoice Date: 2025-01-15\nDate: Nov 23 2012\nPeriod: 01.02.2024 - 29.02.2024')}
                    className="text-xs px-2 py-1 bg-purple-100 text-purple-700 rounded hover:bg-purple-200 transition-colors"
                    disabled={testing}
                  >
                    Date Sample
                  </button>
                  <button
                    type="button"
                    onClick={() => setSampleText('Invoice #: INV-2025-001234\nINVOICE # 17042\nInvoice No 123100401')}
                    className="text-xs px-2 py-1 bg-yellow-100 text-yellow-700 rounded hover:bg-yellow-200 transition-colors"
                    disabled={testing}
                  >
                    Invoice # Sample
                  </button>
                  <button
                    type="button"
                    onClick={() => setSampleText('Vendor: ABC Company Ltd.\nBill To: Trudy Brown\nCPB Software (Germany) GmbH')}
                    className="text-xs px-2 py-1 bg-orange-100 text-orange-700 rounded hover:bg-orange-200 transition-colors"
                    disabled={testing}
                  >
                    Vendor Sample
                  </button>
                </div>
                
                <textarea
                  id="sampleText"
                  value={sampleText}
                  onChange={(e) => setSampleText(e.target.value)}
                  placeholder="Enter sample text to test the pattern against..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 resize-none"
                  rows={3}
                  disabled={testing}
                />
              </div>

              <div className="flex items-center space-x-3">
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={handleTestPattern}
                  disabled={testing || !sampleText.trim()}
                  loading={testing}
                >
                  <BeakerIcon className="h-4 w-4 mr-1" />
                  Test Pattern
                </Button>
                
                {testResults && (
                  <div className="flex items-center text-sm">
                    {testResults.success ? (
                      <div className="flex items-center text-green-600">
                        <CheckCircleIcon className="h-4 w-4 mr-1" />
                        Match found!
                      </div>
                    ) : (
                      <div className="flex items-center text-red-600">
                        <XCircleIcon className="h-4 w-4 mr-1" />
                        No match
                      </div>
                    )}
                  </div>
                )}
              </div>

              {testError && (
                <Alert variant="error" dismissible onDismiss={() => setTestError(null)}>
                  {testError}
                </Alert>
              )}

              {testResults && (
                <div className="bg-white rounded border border-gray-200 p-3">
                  <h4 className="text-sm font-medium text-gray-900 mb-2">Test Results</h4>
                  
                  <div className="space-y-2 text-sm">
                    <div>
                      <span className="font-medium text-gray-700">Success:</span>{' '}
                      <span className={testResults.success ? 'text-green-600' : 'text-red-600'}>
                        {testResults.success ? 'Yes' : 'No'}
                      </span>
                    </div>
                    
                    <div>
                      <span className="font-medium text-gray-700">Confidence:</span>{' '}
                      <span className="text-gray-900">{Math.round(testResults.confidence * 100)}%</span>
                    </div>
                    
                    {testResults.matches && testResults.matches.length > 0 && (
                      <div>
                        <span className="font-medium text-gray-700">Matches:</span>
                        <div className="mt-1 space-y-1">
                          {testResults.matches.map((match, index) => (
                            <div key={index} className="bg-yellow-100 text-yellow-800 px-2 py-1 rounded text-xs font-mono">
                              {match}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        <Input
          {...register('extractionField')}
          label="Extraction Field"
          placeholder="e.g., total_amount, invoice_date, vendor_name"
          error={errors.extractionField?.message}
          disabled={loading || isSubmitting}
          fullWidth
        />

        {/* Settings */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
          <Input
            {...register('priority', { valueAsNumber: true })}
            label="Priority"
            type="number"
            min="1"
            max="100"
            placeholder="1-100"
            error={errors.priority?.message}
            disabled={loading || isSubmitting}
            helperText="Higher numbers = higher priority"
            fullWidth
          />

          <div className="flex items-center space-x-3 pt-6">
            <input
              {...register('isActive')}
              id="isActive"
              type="checkbox"
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              disabled={loading || isSubmitting}
            />
            <label htmlFor="isActive" className="text-sm font-medium text-gray-700">
              Active Pattern
            </label>
          </div>
        </div>

        {/* Examples */}
        <div>
          <div className="flex items-center justify-between mb-3">
            <label className="block text-sm font-medium text-gray-700">
              Example Text (Optional)
            </label>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleAddExample}
              disabled={loading || isSubmitting}
            >
              <PlusIcon className="h-4 w-4 mr-1" />
              Add Example
            </Button>
          </div>

          <div className="space-y-3">
            {examples.map((example, index) => (
              <div key={index} className="flex items-center space-x-2">
                <Input
                  value={example}
                  onChange={(e) => handleExampleChange(index, e.target.value)}
                  placeholder="Enter sample text that matches this pattern"
                  disabled={loading || isSubmitting}
                  fullWidth
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => handleRemoveExample(index)}
                  disabled={loading || isSubmitting}
                  aria-label="Remove example"
                >
                  <XMarkIcon className="h-4 w-4" />
                </Button>
              </div>
            ))}
          </div>
        </div>

        {/* Validation Status */}
        {regexPattern && testRegex(regexPattern) && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-start space-x-3">
              <BeakerIcon className="h-5 w-5 text-blue-500 mt-0.5 flex-shrink-0" />
              <div className="flex-1">
                <h4 className="text-sm font-medium text-blue-900 mb-1">
                  Pattern Validation Status
                </h4>
                
                {mode === 'create' ? (
                  <div className="text-sm text-blue-800">
                    {!testResults ? (
                      <div className="flex items-center text-amber-700">
                        <XCircleIcon className="h-4 w-4 mr-1" />
                        <span>Testing required before saving new patterns</span>
                      </div>
                    ) : testResults.success ? (
                      <div className="flex items-center text-green-700">
                        <CheckCircleIcon className="h-4 w-4 mr-1" />
                        <span>Pattern test passed! Ready to save.</span>
                        {testResults.confidence < 0.7 && (
                          <span className="ml-2 text-amber-600">
                            (Low confidence: {Math.round(testResults.confidence * 100)}%)
                          </span>
                        )}
                      </div>
                    ) : (
                      <div className="flex items-center text-red-700">
                        <XCircleIcon className="h-4 w-4 mr-1" />
                        <span>Pattern test failed. Please fix before saving.</span>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-sm text-blue-800">
                    {pattern && regexPattern !== pattern.regexPattern ? (
                      !testResults ? (
                        <div className="flex items-center text-amber-700">
                          <XCircleIcon className="h-4 w-4 mr-1" />
                          <span>Pattern changed - testing recommended</span>
                        </div>
                      ) : testResults.success ? (
                        <div className="flex items-center text-green-700">
                          <CheckCircleIcon className="h-4 w-4 mr-1" />
                          <span>Updated pattern test passed!</span>
                        </div>
                      ) : (
                        <div className="flex items-center text-amber-700">
                          <XCircleIcon className="h-4 w-4 mr-1" />
                          <span>Pattern test failed - review recommended</span>
                        </div>
                      )
                    ) : (
                      <div className="flex items-center text-gray-700">
                        <CheckCircleIcon className="h-4 w-4 mr-1" />
                        <span>No changes detected</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center justify-end space-x-3 pt-6 border-t border-gray-200">
          <Button
            type="button"
            variant="secondary"
            onClick={onClose}
            disabled={loading || isSubmitting}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            loading={loading || isSubmitting}
            disabled={loading || isSubmitting}
          >
            {mode === 'edit' ? 'Update Pattern' : 'Create Pattern'}
          </Button>
        </div>
      </form>
    </Modal>
  );
};

export default PatternForm;
