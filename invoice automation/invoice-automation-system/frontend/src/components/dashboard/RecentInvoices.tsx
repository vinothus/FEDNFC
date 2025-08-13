import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';
import { 
  DocumentTextIcon, 
  EyeIcon,
  ArrowDownTrayIcon 
} from '@heroicons/react/24/outline';
import { cn } from '../../utils/cn';
import { Button, Loading } from '../ui';
import InvoiceDetailsModal, { InvoiceModalData } from '../ui/InvoiceDetailsModal';
import { invoiceApi } from '../../services/api';

export interface RecentInvoice {
  id: number;
  fileName: string;
  uploadDate: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  totalAmount?: number;
  currency?: string;
  vendorName?: string;
  downloadUrl?: string;
  rawText?: string;
  extractedData?: Record<string, any>;
  invoiceNumber?: string;
  invoiceDate?: string;
  dueDate?: string;
  ocrStatus?: string;
  ocrConfidence?: number;
  fileSize?: number;
}

export interface RecentInvoicesProps {
  invoices: RecentInvoice[];
  loading?: boolean;
  className?: string;
}

/**
 * RecentInvoices component following React UI Cursor Rules
 * - Displays recent invoice activity
 * - Responsive table design
 * - Accessible with proper table structure
 * - Status indicators with appropriate colors
 * - Quick action buttons
 */
const RecentInvoices: React.FC<RecentInvoicesProps> = ({
  invoices,
  loading = false,
  className,
}) => {
  // const navigate = useNavigate(); // TODO: Use for future navigation features
  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceModalData | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loadingDetails, setLoadingDetails] = useState(false);

  const handleViewInvoice = async (invoice: RecentInvoice) => {
    try {
      setLoadingDetails(true);
      
      // If we already have raw text, use it directly
      if (invoice.rawText || invoice.extractedData) {
        const modalData: InvoiceModalData = {
          id: invoice.id,
          fileName: invoice.fileName,
          status: invoice.status,
          vendorName: invoice.vendorName,
          totalAmount: invoice.totalAmount,
          currency: invoice.currency,
          uploadDate: invoice.uploadDate,
          rawText: invoice.rawText,
          extractedData: invoice.extractedData,
          invoiceNumber: invoice.invoiceNumber,
          invoiceDate: invoice.invoiceDate,
          dueDate: invoice.dueDate,
          ocrStatus: invoice.ocrStatus,
          ocrConfidence: invoice.ocrConfidence,
          fileSize: invoice.fileSize,
        };
        
        setSelectedInvoice(modalData);
        setIsModalOpen(true);
        return;
      }

      // Fetch detailed invoice data including raw text
      console.log('📄 RecentInvoices: Fetching detailed data for invoice:', invoice.id);
      const detailedInvoice = await invoiceApi.getInvoiceDetails(invoice.id.toString());
      
      const modalData: InvoiceModalData = {
        id: invoice.id,
        fileName: invoice.fileName,
        status: invoice.status,
        vendorName: invoice.vendorName || detailedInvoice.vendorName,
        totalAmount: invoice.totalAmount || detailedInvoice.totalAmount,
        currency: invoice.currency || detailedInvoice.currency,
        uploadDate: invoice.uploadDate || detailedInvoice.uploadDate,
        rawText: detailedInvoice.rawText,
        extractedData: detailedInvoice.extractedData,
        invoiceNumber: invoice.invoiceNumber || detailedInvoice.invoiceNumber,
        invoiceDate: invoice.invoiceDate || detailedInvoice.invoiceDate,
        dueDate: invoice.dueDate || detailedInvoice.dueDate,
        ocrStatus: invoice.ocrStatus,
        ocrConfidence: invoice.ocrConfidence,
        fileSize: invoice.fileSize,
      };
      
      console.log('✅ RecentInvoices: Detailed data fetched:', modalData);
      setSelectedInvoice(modalData);
      setIsModalOpen(true);
    } catch (error) {
      console.error('❌ RecentInvoices: Failed to fetch invoice details:', error);
      
      // Fallback to basic data if detailed fetch fails
      const modalData: InvoiceModalData = {
        id: invoice.id,
        fileName: invoice.fileName,
        status: invoice.status,
        vendorName: invoice.vendorName,
        totalAmount: invoice.totalAmount,
        currency: invoice.currency,
        uploadDate: invoice.uploadDate,
        rawText: `Failed to load raw text from server.\n\nError: ${error}\n\nPlease try again or contact support if the issue persists.`,
        extractedData: { 
          error: 'Failed to load extracted data',
          message: 'Could not fetch detailed invoice information',
          timestamp: new Date().toISOString()
        },
        invoiceNumber: invoice.invoiceNumber,
        invoiceDate: invoice.invoiceDate,
        dueDate: invoice.dueDate,
        ocrStatus: 'Error loading',
        ocrConfidence: invoice.ocrConfidence,
        fileSize: invoice.fileSize,
      };
      
      setSelectedInvoice(modalData);
      setIsModalOpen(true);
    } finally {
      setLoadingDetails(false);
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedInvoice(null);
  };

  const getDownloadUrl = (invoice: RecentInvoice) => {
    if (!invoice.downloadUrl) return null;
    // Check if downloadUrl already contains the full URL
    if (invoice.downloadUrl.startsWith('http://') || invoice.downloadUrl.startsWith('https://')) {
      return invoice.downloadUrl;
    }
    // Otherwise, prepend the base URL
    return `http://localhost:8080${invoice.downloadUrl}`;
  };
  const getStatusBadge = (status: RecentInvoice['status']) => {
    const baseClasses = 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium';
    
    switch (status) {
      case 'COMPLETED':
        return `${baseClasses} bg-green-100 text-green-800`;
      case 'PROCESSING':
        return `${baseClasses} bg-blue-100 text-blue-800`;
      case 'PENDING':
        return `${baseClasses} bg-yellow-100 text-yellow-800`;
      case 'FAILED':
        return `${baseClasses} bg-red-100 text-red-800`;
      default:
        return `${baseClasses} bg-gray-100 text-gray-800`;
    }
  };

  const formatAmount = (amount?: number, currency = 'USD') => {
    if (!amount) return '-';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
    }).format(amount);
  };

  if (loading) {
    return (
      <div className={cn('bg-white rounded-lg shadow-sm border border-gray-200', className)}>
        <div className="px-6 py-4 border-b border-gray-200">
          <h3 className="text-lg font-medium text-gray-900">Recent Invoices</h3>
        </div>
        <div className="p-6">
          <Loading size="lg" text="Loading recent invoices..." />
        </div>
      </div>
    );
  }

  return (
    <>
      <div className={cn('bg-white rounded-lg shadow-sm border border-gray-200', className)}>
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium text-gray-900">Recent Invoices</h3>
            <Link
              to="/invoices"
              className="text-sm font-medium text-blue-600 hover:text-blue-500"
            >
              View all
            </Link>
          </div>
        </div>

      {/* Content */}
      <div className="overflow-hidden">
        {invoices.length === 0 ? (
          <div className="text-center py-12">
            <DocumentTextIcon className="mx-auto h-12 w-12 text-gray-400" />
            <h4 className="mt-4 text-lg font-medium text-gray-900">No recent invoices</h4>
            <p className="mt-2 text-sm text-gray-600">
              Upload your first invoice to get started.
            </p>
            <div className="mt-6">
              <Link to="/upload">
                <Button variant="primary" size="sm">
                  Upload Invoice
                </Button>
              </Link>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th 
                    scope="col" 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Invoice
                  </th>
                  <th 
                    scope="col" 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Status
                  </th>
                  <th 
                    scope="col" 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Amount
                  </th>
                  <th 
                    scope="col" 
                    className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    Date
                  </th>
                  <th 
                    scope="col" 
                    className="relative px-6 py-3"
                  >
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {invoices.map((invoice) => (
                  <tr key={invoice.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <DocumentTextIcon className="h-5 w-5 text-gray-400 mr-3" />
                        <div>
                          <div className="text-sm font-medium text-gray-900 truncate max-w-xs">
                            {invoice.fileName}
                          </div>
                          {invoice.vendorName && (
                            <div className="text-sm text-gray-500 truncate max-w-xs">
                              {invoice.vendorName}
                            </div>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={getStatusBadge(invoice.status)}>
                        {invoice.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {formatAmount(invoice.totalAmount, invoice.currency)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {format(new Date(invoice.uploadDate), 'MMM dd, yyyy')}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <div className="flex items-center space-x-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleViewInvoice(invoice)}
                          disabled={loadingDetails}
                          loading={loadingDetails}
                          aria-label={`View invoice ${invoice.fileName}`}
                        >
                          <EyeIcon className="h-4 w-4" />
                        </Button>
                        {invoice.status === 'COMPLETED' && invoice.downloadUrl && (
                          <div className="relative group">
                            <a
                              href={getDownloadUrl(invoice) || '#'}
                              download={invoice.fileName}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="inline-flex items-center justify-center h-8 w-8 rounded-md text-gray-400 hover:text-gray-600 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500 transition-colors duration-200"
                              aria-label={`Download invoice ${invoice.fileName}`}
                            >
                              <ArrowDownTrayIcon className="h-4 w-4" />
                            </a>
                            {/* Tooltip */}
                            <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-2 bg-gray-900 text-white text-xs rounded-md opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-10">
                              <div className="font-medium">{invoice.fileName}</div>
                              <div className="text-gray-300 text-xs mt-1 max-w-xs truncate">{getDownloadUrl(invoice)}</div>
                              <div className="absolute top-full left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-900"></div>
                            </div>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      </div>
      
      {/* Invoice Details Modal */}
      <InvoiceDetailsModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        invoice={selectedInvoice}
      />
    </>
  );
};

export default RecentInvoices;
