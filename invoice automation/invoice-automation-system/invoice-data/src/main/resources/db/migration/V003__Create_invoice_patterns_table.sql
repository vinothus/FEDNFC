-- Create invoice_patterns table for dynamic pattern matching
CREATE TABLE invoice_patterns (
    id BIGSERIAL PRIMARY KEY,
    pattern_name VARCHAR(100) NOT NULL,
    pattern_category VARCHAR(50) NOT NULL, -- 'INVOICE_NUMBER', 'AMOUNT', 'DATE', 'VENDOR', 'ADDRESS'
    pattern_regex TEXT NOT NULL,
    pattern_flags INTEGER DEFAULT 2, -- Pattern.CASE_INSENSITIVE = 2
    pattern_description TEXT,
    pattern_priority INTEGER DEFAULT 100, -- Lower number = higher priority
    confidence_weight DECIMAL(3,2) DEFAULT 1.0, -- Weight for confidence calculation (0.1 to 1.0)
    is_active BOOLEAN DEFAULT TRUE,
    date_format VARCHAR(50), -- For date patterns: 'MM/dd/yyyy', 'dd-MM-yyyy', etc.
    capture_group INTEGER DEFAULT 1, -- Which regex group contains the value (usually 1)
    validation_regex TEXT, -- Optional additional validation for extracted value
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    notes TEXT
);

-- Create indexes for performance
CREATE INDEX idx_invoice_patterns_category ON invoice_patterns(pattern_category);
CREATE INDEX idx_invoice_patterns_priority ON invoice_patterns(pattern_priority);
CREATE INDEX idx_invoice_patterns_active ON invoice_patterns(is_active);
CREATE INDEX idx_invoice_patterns_category_active_priority ON invoice_patterns(pattern_category, is_active, pattern_priority);

-- Insert golden patterns based on your invoice format
INSERT INTO invoice_patterns (pattern_name, pattern_category, pattern_regex, pattern_priority, confidence_weight, pattern_description, date_format, notes) VALUES

-- INVOICE NUMBER PATTERNS (High Priority)
('InvoiceNumber_Tabular', 'INVOICE_NUMBER', '(?i)invoice\s+number\s+([A-Z0-9-]{3,})', 10, 1.0, 'Invoice Number INV-3337 (tabular format)', NULL, 'Your invoice format'),
('InvoiceNumber_WithColon', 'INVOICE_NUMBER', '(?i)invoice\s+number\s*:\s*([A-Z0-9-]{3,})', 20, 0.9, 'Invoice Number: INV-3337', NULL, 'Traditional format'),
('OrderNumber_Tabular', 'INVOICE_NUMBER', '(?i)order\s+number\s+([A-Z0-9-]{3,})', 15, 0.8, 'Order Number 12345 (as fallback)', NULL, 'Order number as invoice reference'),

-- AMOUNT PATTERNS (Prioritized for your format)
('TotalDue_Tabular', 'AMOUNT', '(?i)total\s+due\s+(?:\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\.[0-9]{2})?)', 10, 1.0, 'Total Due $93.50', NULL, 'Your invoice primary total'),
('Total_EndOfLine', 'AMOUNT', '(?i)^total\s+(?:\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\.[0-9]{2})?)$', 20, 0.9, 'Total $93.50 (end of line)', NULL, 'Your invoice final total'),
('SubTotal_Tabular', 'AMOUNT', '(?i)sub\s+total\s+(?:\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\.[0-9]{2})?)', 30, 0.7, 'Sub Total $85.00', NULL, 'Subtotal extraction'),
('Total_WithColon', 'AMOUNT', '(?i)(?:total|amount|sum)\s*:\s*(?:\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)?\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\.[0-9]{2})?)', 40, 0.8, 'Total: $93.50', NULL, 'Traditional colon format'),

-- DATE PATTERNS (Your format: January 25, 2016)
('Date_MonthDayYear', 'DATE', '([A-Za-z]{3,9}\s+\d{1,2},?\s+\d{4})', 10, 1.0, 'January 25, 2016', 'MMMM d, yyyy', 'Your invoice date format'),
('Date_DayMonthYear', 'DATE', '(\d{1,2}\s+[A-Za-z]{3,9}\s+\d{4})', 20, 0.9, '25 January 2016', 'd MMMM yyyy', 'Alternative format'),
('Date_MMDDYYYY', 'DATE', '(\d{1,2}/\d{1,2}/\d{4})', 30, 0.8, '01/25/2016', 'M/d/yyyy', 'US format'),
('Date_DDMMYYYY', 'DATE', '(\d{1,2}/\d{1,2}/\d{4})', 40, 0.7, '25/01/2016', 'd/M/yyyy', 'EU format'),

-- VENDOR PATTERNS
('Vendor_From', 'VENDOR', '(?i)from:\s*([A-Za-z0-9\s\-,.&]+)(?=\s*(?:suite|street|avenue|road|po box|\d+|$))', 10, 1.0, 'From: DEMO - Sliced Invoices', NULL, 'Your vendor format'),
('Vendor_Company', 'VENDOR', '(?i)(?:company|vendor|supplier):\s*([A-Za-z0-9\s\-,.&]+)', 20, 0.9, 'Company: ABC Corp', NULL, 'Traditional vendor'),

-- TAX PATTERNS
('Tax_Tabular', 'TAX_AMOUNT', '(?i)tax\s+(?:\$|USD|€|EUR|£|GBP|¥|JPY|₹|INR)\s*([0-9]{1,3}(?:,?[0-9]{3})*(?:\.[0-9]{2})?)', 10, 1.0, 'Tax $8.50', NULL, 'Your tax format'),

-- ADDRESS PATTERNS
('Address_Standard', 'ADDRESS', '([A-Za-z0-9\s,.-]+),\s*([A-Za-z\s]+),?\s*([A-Z]{2})\s+([0-9]{5}(?:-[0-9]{4})?)', 10, 0.8, 'Street, City, State ZIP', NULL, 'US address format');

-- Add trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_invoice_patterns_updated_at 
    BEFORE UPDATE ON invoice_patterns 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments
COMMENT ON TABLE invoice_patterns IS 'Dynamic pattern library for invoice data extraction';
COMMENT ON COLUMN invoice_patterns.pattern_category IS 'INVOICE_NUMBER, AMOUNT, DATE, VENDOR, ADDRESS, TAX_AMOUNT, SUBTOTAL_AMOUNT';
COMMENT ON COLUMN invoice_patterns.pattern_priority IS 'Lower number = higher priority (1-1000)';
COMMENT ON COLUMN invoice_patterns.confidence_weight IS 'Weight for confidence calculation (0.1-1.0)';
