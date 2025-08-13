import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { invoiceApi } from '../services/api';
import { Invoice, ApiError } from '../types';
import { useAuth } from '../contexts/AuthContext';
import InvoiceDetailsModal, { InvoiceModalData } from '../components/ui/InvoiceDetailsModal';

const Invoices: React.FC = () => {
  const { isAuthenticated, isLoading: authLoading, accessToken } = useAuth();
  // const navigate = useNavigate(); // TODO: Use for future navigation features
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [deleteLoading, setDeleteLoading] = useState<string | null>(null);
  const [selectedInvoice, setSelectedInvoice] = useState<InvoiceModalData | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loadingDetails, setLoadingDetails] = useState(false);
  
  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize] = useState(20);

  const fetchInvoices = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      // Double-check that we have a token before making the API call
      const token = localStorage.getItem('accessToken');
      if (!token) {
        throw new Error('No authentication token found');
      }
      
      console.log('📄 Invoices: Fetching invoices page:', currentPage, 'size:', pageSize);
      const response = await invoiceApi.getAllInvoicesWithPagination({
        page: currentPage,
        size: pageSize,
        sortBy: 'createdAt',
        sortDir: 'desc'
      });
      
      console.log('📄 Invoices: Response received:', response);
      
      // Handle both paginated response and simple array response
      if (response.content) {
        // Paginated response
        setInvoices(response.content);
        setTotalPages(response.totalPages || 0);
        setTotalElements(response.totalElements || 0);
      } else {
        // Simple array response (fallback)
        const invoicesArray = response as unknown as Invoice[];
        setInvoices(invoicesArray);
        setTotalPages(1);
        setTotalElements(invoicesArray.length || 0);
      }
    } catch (err) {
      const apiError = err as ApiError;
      console.error('❌ Invoices: Error fetching invoices:', apiError);
      setError(apiError.message);
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize]);

  useEffect(() => {
    // Only fetch invoices when authentication is complete and user is authenticated
    if (!authLoading && isAuthenticated && accessToken) {
      fetchInvoices();
    }
  }, [authLoading, isAuthenticated, accessToken, fetchInvoices]);

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this invoice?')) {
      return;
    }

    try {
      setDeleteLoading(id);
      await invoiceApi.deleteInvoice(id);
      setInvoices(prev => prev.filter(invoice => invoice.id !== id));
    } catch (err) {
      const apiError = err as ApiError;
      alert(`Failed to delete invoice: ${apiError.message}`);
    } finally {
      setDeleteLoading(null);
    }
  };

  const handleReprocess = async (id: string) => {
    try {
      await invoiceApi.reprocessInvoice(id);
      // Refresh the list to get updated status
      await fetchInvoices();
    } catch (err) {
      const apiError = err as ApiError;
      alert(`Failed to reprocess invoice: ${apiError.message}`);
    }
  };

  const handleViewInvoice = async (id: string) => {
    try {
      setLoadingDetails(true);
      
      // Find basic invoice data
      const invoice = invoices.find(inv => inv.id === id);
      if (!invoice) {
        alert('Invoice not found');
        return;
      }

      console.log('📄 Invoices: Fetching detailed data for invoice:', id);
      
      // Fetch detailed invoice data including raw text
      const detailedInvoice = await invoiceApi.getInvoiceDetails(id.toString());
      
      const modalData: InvoiceModalData = {
        id: invoice.id,
        fileName: invoice.fileName,
        status: invoice.status,
        vendorName: invoice.vendorName || detailedInvoice.vendorName,
        totalAmount: invoice.totalAmount || detailedInvoice.totalAmount,
        currency: invoice.currency || detailedInvoice.currency,
        uploadDate: invoice.uploadDate || detailedInvoice.uploadDate,
        invoiceNumber: invoice.invoiceNumber || detailedInvoice.invoiceNumber,
        invoiceDate: invoice.invoiceDate || detailedInvoice.invoiceDate,
        dueDate: invoice.dueDate || detailedInvoice.dueDate,
        rawText: detailedInvoice.rawText,
        extractedData: detailedInvoice.extractedData,
        ocrStatus: invoice.ocrStatus || detailedInvoice.ocrStatus,
        ocrConfidence: invoice.ocrConfidence || detailedInvoice.ocrConfidence,
        fileSize: invoice.fileSize || detailedInvoice.fileSize,
      };
      
      console.log('✅ Invoices: Detailed data fetched:', modalData);
      setSelectedInvoice(modalData);
      setIsModalOpen(true);
    } catch (error) {
      console.error('❌ Invoices: Failed to fetch invoice details:', error);
      // Fallback to basic data if detailed fetch fails
      const invoice = invoices.find(inv => inv.id === id);
      if (invoice) {
        const modalData: InvoiceModalData = {
          id: invoice.id,
          fileName: invoice.fileName,
          status: invoice.status,
          vendorName: invoice.vendorName,
          totalAmount: invoice.totalAmount,
          currency: invoice.currency,
          uploadDate: invoice.uploadDate,
          invoiceNumber: invoice.invoiceNumber,
          invoiceDate: invoice.invoiceDate,
          dueDate: invoice.dueDate,
          rawText: 'Failed to load raw text - please try again',
          extractedData: { error: 'Failed to load extracted data' },
        };
        setSelectedInvoice(modalData);
        setIsModalOpen(true);
      }
    } finally {
      setLoadingDetails(false);
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setSelectedInvoice(null);
  };

  const getDownloadUrl = (invoice: Invoice) => {
    if (invoice.downloadUrl) {
      // Check if downloadUrl already contains the full URL
      if (invoice.downloadUrl.startsWith('http://') || invoice.downloadUrl.startsWith('https://')) {
        return invoice.downloadUrl;
      }
      // Otherwise, prepend the base URL
      return `http://localhost:8080${invoice.downloadUrl}`;
    } else {
      // Fallback to API download endpoint (requires auth)
      return `http://localhost:8080/invoice-automation/api/invoices/${invoice.id}/download`;
    }
  };

  const getStatusBadge = (status: Invoice['status']) => {
    const statusConfig = {
      PENDING: { color: 'bg-yellow-100 text-yellow-800', text: 'Pending' },
      PROCESSING: { color: 'bg-blue-100 text-blue-800', text: 'Processing' },
      COMPLETED: { color: 'bg-green-100 text-green-800', text: 'Completed' },
      FAILED: { color: 'bg-red-100 text-red-800', text: 'Failed' },
    };

    const config = statusConfig[status];
    return (
      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.color}`}>
        {config.text}
      </span>
    );
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600" aria-label="Loading invoices"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-md p-4">
        <div className="flex">
          <div className="ml-3">
            <h3 className="text-sm font-medium text-red-800">Error loading invoices</h3>
            <div className="mt-2 text-sm text-red-700">
              <p>{error}</p>
            </div>
            <div className="mt-4">
              <button
                onClick={fetchInvoices}
                className="bg-red-100 hover:bg-red-200 text-red-800 px-3 py-2 rounded text-sm font-medium transition-colors duration-200"
              >
                Try Again
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Invoices</h1>
            <p className="mt-2 text-gray-600">Manage and view all processed invoices</p>
          </div>
          <Link
            to="/upload"
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
          >
            Upload New Invoice
          </Link>
        </div>

      {invoices.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-gray-400 text-6xl mb-4">📄</div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No invoices found</h3>
          <p className="text-gray-600 mb-4">Get started by uploading your first invoice</p>
          <Link
            to="/upload"
            className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors duration-200"
          >
            Upload Invoice
          </Link>
        </div>
      ) : (
        <div className="bg-white shadow overflow-visible sm:rounded-md">
          <ul className="divide-y divide-gray-200">
            {invoices.map((invoice) => (
              <li key={invoice.id} className="px-6 py-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-3">
                      <div className="flex-shrink-0">
                        {getStatusBadge(invoice.status)}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {invoice.fileName}
                        </p>
                        <p className="text-sm text-gray-500">
                          Uploaded: {new Date(invoice.uploadDate).toLocaleDateString()}
                        </p>
                      </div>
                    </div>
                    
                    {invoice.status === 'COMPLETED' && (
                      <div className="mt-2 grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm text-gray-600">
                        {invoice.vendorName && (
                          <div>
                            <span className="font-medium">Vendor:</span> {invoice.vendorName}
                          </div>
                        )}
                        {invoice.totalAmount && (
                          <div>
                            <span className="font-medium">Amount:</span> {invoice.currency || '$'}{invoice.totalAmount}
                          </div>
                        )}
                        {invoice.invoiceNumber && (
                          <div>
                            <span className="font-medium">Invoice #:</span> {invoice.invoiceNumber}
                          </div>
                        )}
                      </div>
                    )}
                    
                    {invoice.status === 'FAILED' && invoice.errorMessage && (
                      <div className="mt-2 text-sm text-red-600">
                        <span className="font-medium">Error:</span> {invoice.errorMessage}
                      </div>
                    )}
                  </div>
                  
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={() => handleViewInvoice(invoice.id)}
                      disabled={loadingDetails}
                      className="text-blue-600 hover:text-blue-500 text-sm font-medium transition-colors duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {loadingDetails ? 'Loading...' : 'View'}
                    </button>
                    {invoice.status === 'COMPLETED' && (
                      <div className="relative group">
                        <a
                          href={getDownloadUrl(invoice)}
                          download={invoice.fileName}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-green-600 hover:text-green-500 text-sm font-medium transition-colors duration-200"
                        >
                          Download
                        </a>
                        {/* Tooltip */}
                        <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-2 bg-gray-900 text-white text-xs rounded-md opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
                          <div className="font-medium">{invoice.fileName}</div>
                          <div className="text-gray-300 text-xs mt-1 max-w-xs truncate">{getDownloadUrl(invoice)}</div>
                          <div className="absolute top-full left-1/2 transform -translate-x-1/2 w-0 h-0 border-l-4 border-r-4 border-t-4 border-transparent border-t-gray-900"></div>
                        </div>
                      </div>
                    )}
                    {invoice.status === 'FAILED' && (
                      <button
                        onClick={() => handleReprocess(invoice.id)}
                        className="text-blue-600 hover:text-blue-500 text-sm font-medium transition-colors duration-200"
                      >
                        Reprocess
                      </button>
                    )}
                    <button
                      onClick={() => handleDelete(invoice.id)}
                      disabled={deleteLoading === invoice.id}
                      className="text-red-600 hover:text-red-500 text-sm font-medium disabled:opacity-50 transition-colors duration-200"
                    >
                      {deleteLoading === invoice.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-between">
          <div className="text-sm text-gray-700">
            Showing {(currentPage * pageSize) + 1} to {Math.min((currentPage + 1) * pageSize, totalElements)} of {totalElements} invoices
          </div>
          <div className="flex space-x-2">
            <button
              onClick={() => setCurrentPage(0)}
              disabled={currentPage === 0}
              className="px-3 py-1 text-sm border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              First
            </button>
            <button
              onClick={() => setCurrentPage(currentPage - 1)}
              disabled={currentPage === 0}
              className="px-3 py-1 text-sm border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <span className="px-3 py-1 text-sm">
              Page {currentPage + 1} of {totalPages}
            </span>
            <button
              onClick={() => setCurrentPage(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              className="px-3 py-1 text-sm border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
            <button
              onClick={() => setCurrentPage(totalPages - 1)}
              disabled={currentPage >= totalPages - 1}
              className="px-3 py-1 text-sm border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Last
            </button>
          </div>
        </div>
      )}
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

export default Invoices;
