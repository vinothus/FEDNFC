// Pattern management related types

export interface InvoicePattern {
  id: string;
  name: string;
  description?: string;
  category: string;
  regexPattern: string;
  extractionField: string;
  isActive: boolean;
  priority: number;
  confidence: number;
  usageCount: number;
  lastUsed?: string;
  createdBy: string;
  createdAt: string;
  updatedAt?: string;
  examples?: string[];
  testResults?: PatternTestResult[];
}

export interface PatternTestResult {
  sampleText: string;
  matches: string[];
  confidence: number;
  success: boolean;
  // Additional fields from backend
  isValid?: boolean;
  matchedText?: string;
  startIndex?: number;
  endIndex?: number;
  captureGroups?: string[];
  errorMessage?: string;
}

export interface PatternRequest {
  name: string;
  description?: string;
  category: string;
  regexPattern: string;
  extractionField: string;
  isActive: boolean;
  priority: number;
  examples?: string[];
}

// Backend-compatible request interface
export interface BackendPatternRequest {
  patternName: string;
  patternDescription?: string;
  patternCategory: string;
  patternRegex: string;
  patternPriority: number;
  confidenceWeight: number;
  isActive: boolean;
  patternFlags?: string;
  dateFormat?: string;
  notes?: string;
}

export interface PatternResponse {
  success: boolean;
  pattern?: InvoicePattern;
  message: string;
}

export interface PatternCategory {
  id: string;
  name: string;
  displayName?: string;
  description?: string;
  count: number;
}

export interface PatternTestRequest {
  pattern: string;
  sampleText: string;
  flags?: string;
}

export interface PatternStats {
  totalPatterns: number;
  activePatterns: number;
  categoriesCount: number;
  avgConfidence: number;
  topPerformingPattern?: InvoicePattern;
}
