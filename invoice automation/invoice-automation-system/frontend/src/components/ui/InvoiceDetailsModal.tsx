import React, { useState } from 'react';
import { XMarkIcon, DocumentTextIcon as DocumentOutlineIcon, CodeBracketIcon } from '@heroicons/react/24/outline';
import { DocumentTextIcon } from '@heroicons/react/24/solid';

export interface InvoiceModalData {
  id: string | number;
  fileName: string;
  status: string;
  vendorName?: string;
  totalAmount?: number;
  currency?: string;
  uploadDate: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  dueDate?: string;
  receivedDate?: string;
  ocrStatus?: string;
  ocrConfidence?: number;
  fileSize?: number;
  rawText?: string; // Add raw OCR text for debugging
  extractedData?: Record<string, any>; // Add extracted data object
}

export interface InvoiceDetailsModalProps {
  isOpen: boolean;
  onClose: () => void;
  invoice: InvoiceModalData | null;
}

/**
 * Invoice Details Modal Component
 * Displays invoice information in a clean modal popup
 */
const InvoiceDetailsModal: React.FC<InvoiceDetailsModalProps> = ({
  isOpen,
  onClose,
  invoice,
}) => {
  const [activeTab, setActiveTab] = useState<'details' | 'rawText' | 'extractedData'>('details');
  
  if (!isOpen || !invoice) return null;

  const formatCurrency = (amount?: number, currency = 'USD') => {
    if (!amount) return 'N/A';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
    }).format(amount);
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch {
      return dateString;
    }
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'N/A';
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + ' ' + sizes[i];
  };

  const getStatusBadge = (status: string) => {
    const statusConfig: Record<string, { color: string; text: string }> = {
      PENDING: { color: 'bg-yellow-100 text-yellow-800 border-yellow-200', text: 'Pending' },
      PROCESSING: { color: 'bg-blue-100 text-blue-800 border-blue-200', text: 'Processing' },
      COMPLETED: { color: 'bg-green-100 text-green-800 border-green-200', text: 'Completed' },
      FAILED: { color: 'bg-red-100 text-red-800 border-red-200', text: 'Failed' },
    };

    const config = statusConfig[status] || { color: 'bg-gray-100 text-gray-800 border-gray-200', text: status };
    return (
      <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${config.color}`}>
        {config.text}
      </span>
    );
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop */}
      <div 
        className="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
        onClick={onClose}
      />
      
      {/* Modal */}
      <div className="flex min-h-full items-center justify-center p-4">
        <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <div className="flex items-center space-x-3">
              <DocumentTextIcon className="h-6 w-6 text-blue-600" />
              <div>
                <h3 className="text-lg font-semibold text-gray-900">Invoice Details</h3>
                <p className="text-sm text-gray-500">ID: {invoice.id}</p>
              </div>
            </div>
            <button
              onClick={onClose}
              className="rounded-md p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <XMarkIcon className="h-5 w-5" />
            </button>
          </div>

          {/* Tab Navigation */}
          <div className="border-b border-gray-200">
            <nav className="flex space-x-8 px-6" aria-label="Tabs">
              <button
                onClick={() => setActiveTab('details')}
                className={`py-3 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'details'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <DocumentOutlineIcon className="h-4 w-4 inline mr-2" />
                Details
              </button>
              {invoice.rawText && (
                <button
                  onClick={() => setActiveTab('rawText')}
                  className={`py-3 px-1 border-b-2 font-medium text-sm ${
                    activeTab === 'rawText'
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  <CodeBracketIcon className="h-4 w-4 inline mr-2" />
                  Raw Text
                </button>
              )}
              {invoice.extractedData && Object.keys(invoice.extractedData).length > 0 && (
                <button
                  onClick={() => setActiveTab('extractedData')}
                  className={`py-3 px-1 border-b-2 font-medium text-sm ${
                    activeTab === 'extractedData'
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  <DocumentTextIcon className="h-4 w-4 inline mr-2" />
                  Extracted Data
                </button>
              )}
            </nav>
          </div>

          {/* Content */}
          <div className="p-6">{activeTab === 'details' && (
            <div className="space-y-6">
            {/* File Information */}
            <div>
              <h4 className="text-sm font-medium text-gray-900 mb-3">File Information</h4>
              <div className="bg-gray-50 rounded-lg p-4 space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">File Name</label>
                    <p className="mt-1 text-sm text-gray-900 font-medium">{invoice.fileName}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Status</label>
                    <div className="mt-1">{getStatusBadge(invoice.status)}</div>
                  </div>
                  {invoice.fileSize && (
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">File Size</label>
                      <p className="mt-1 text-sm text-gray-900">{formatFileSize(invoice.fileSize)}</p>
                    </div>
                  )}
                  <div>
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Upload Date</label>
                    <p className="mt-1 text-sm text-gray-900">{formatDate(invoice.uploadDate)}</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Invoice Data */}
            <div>
              <h4 className="text-sm font-medium text-gray-900 mb-3">Invoice Data</h4>
              <div className="bg-gray-50 rounded-lg p-4 space-y-3">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Vendor</label>
                    <p className="mt-1 text-sm text-gray-900">{invoice.vendorName || 'N/A'}</p>
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Amount</label>
                    <p className="mt-1 text-sm text-gray-900 font-medium">
                      {formatCurrency(invoice.totalAmount, invoice.currency)}
                    </p>
                  </div>
                  {invoice.invoiceNumber && (
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Invoice Number</label>
                      <p className="mt-1 text-sm text-gray-900">{invoice.invoiceNumber}</p>
                    </div>
                  )}
                  {invoice.invoiceDate && (
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Invoice Date</label>
                      <p className="mt-1 text-sm text-gray-900">{formatDate(invoice.invoiceDate)}</p>
                    </div>
                  )}
                  {invoice.dueDate && (
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Due Date</label>
                      <p className="mt-1 text-sm text-gray-900">{formatDate(invoice.dueDate)}</p>
                    </div>
                  )}
                  {invoice.receivedDate && (
                    <div>
                      <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">Received Date</label>
                      <p className="mt-1 text-sm text-gray-900">{formatDate(invoice.receivedDate)}</p>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Processing Information */}
            {(invoice.ocrStatus || invoice.ocrConfidence) && (
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-3">Processing Information</h4>
                <div className="bg-gray-50 rounded-lg p-4 space-y-3">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {invoice.ocrStatus && (
                      <div>
                        <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">OCR Status</label>
                        <p className="mt-1 text-sm text-gray-900">{invoice.ocrStatus}</p>
                      </div>
                    )}
                    {invoice.ocrConfidence && (
                      <div>
                        <label className="text-xs font-medium text-gray-500 uppercase tracking-wide">OCR Confidence</label>
                        <p className="mt-1 text-sm text-gray-900">{Math.round(invoice.ocrConfidence * 100) / 100}%</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            )}
            </div>
          )}

          {/* Raw Text Tab */}
          {activeTab === 'rawText' && invoice.rawText && (
            <div className="space-y-4">
              <h4 className="text-sm font-medium text-gray-900">Raw OCR Text</h4>
              <div className="bg-gray-50 rounded-lg p-4 max-h-96 overflow-y-auto">
                <pre className="text-xs text-gray-700 whitespace-pre-wrap font-mono leading-relaxed">
                  {invoice.rawText}
                </pre>
              </div>
              <p className="text-xs text-gray-500">
                This is the raw text extracted from the invoice using OCR. You can use this to debug pattern matching issues.
              </p>
            </div>
          )}

          {/* Extracted Data Tab */}
          {activeTab === 'extractedData' && invoice.extractedData && (
            <div className="space-y-4">
              <h4 className="text-sm font-medium text-gray-900">Extracted Data</h4>
              <div className="bg-gray-50 rounded-lg p-4 max-h-96 overflow-y-auto">
                <pre className="text-xs text-gray-700 whitespace-pre-wrap font-mono">
                  {JSON.stringify(invoice.extractedData, null, 2)}
                </pre>
              </div>
              <p className="text-xs text-gray-500">
                This shows all data extracted from the invoice using pattern matching. Null values indicate extraction failures.
              </p>
            </div>
          )}
          </div>

          {/* Footer */}
          <div className="px-6 py-4 border-t border-gray-200 bg-gray-50 rounded-b-lg">
            <div className="flex justify-end">
              <button
                onClick={onClose}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InvoiceDetailsModal;
