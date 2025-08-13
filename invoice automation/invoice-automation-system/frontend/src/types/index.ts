export interface Invoice {
  id: string;
  fileName: string;
  uploadDate: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  totalAmount?: number;
  currency?: string;
  vendorName?: string;
  invoiceNumber?: string;
  invoiceDate?: string;
  dueDate?: string;
  errorMessage?: string;
  downloadUrl?: string;
  // Extended fields for detailed invoice data
  ocrStatus?: string;
  ocrConfidence?: number;
  fileSize?: number;
}

export interface UploadResponse {
  success: boolean;
  message: string;
  invoiceId?: string;
}

export interface DashboardStats {
  totalInvoices: number;
  pendingInvoices: number;
  completedInvoices: number;
  failedInvoices: number;
  totalAmount: number;
}

export interface ApiError {
  message: string;
  status: number;
}

// Re-export auth types
export * from './auth';

// Re-export pattern types
export * from './pattern';

// Re-export user types
export * from './user';

// Re-export settings types
export * from './settings';