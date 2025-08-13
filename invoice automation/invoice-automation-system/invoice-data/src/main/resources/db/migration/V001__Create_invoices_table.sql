-- Invoice Automation System - Database Schema
-- Migration V001: Create invoices table with BLOB support
-- Compatible with both H2 (development) and PostgreSQL (production)

-- Create invoices table
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    
    -- File Information
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) DEFAULT 'application/pdf',
    checksum VARCHAR(64),
    
    -- PDF BLOB Storage (H2: BLOB, PostgreSQL: BYTEA)
    pdf_blob BYTEA,
    
    -- Download Link Management
    download_token VARCHAR(128) UNIQUE NOT NULL,
    download_expires_at TIMESTAMP,
    download_count INTEGER DEFAULT 0,
    max_downloads INTEGER DEFAULT 10,
    download_url VARCHAR(500),
    
    -- Email Source Information
    email_subject VARCHAR(500),
    sender_email VARCHAR(255),
    received_date TIMESTAMP,
    
    -- Processing Status
    processing_status VARCHAR(50) DEFAULT 'PENDING',
    ocr_status VARCHAR(50) DEFAULT 'PENDING',
    ocr_confidence DECIMAL(5,2),
    
    -- Raw extracted text and OCR metadata
    raw_extracted_text TEXT,
    ocr_method VARCHAR(50),
    ocr_processing_time_ms INTEGER,
    text_word_count INTEGER,
    text_character_count INTEGER,
    
    -- Pattern tracking for extraction analytics
    used_pattern_ids TEXT,
    pattern_match_summary TEXT,
    extraction_confidence_details TEXT,
    
    -- Invoice Data (extracted via OCR)
    vendor_name VARCHAR(255),
    vendor_address TEXT,
    vendor_tax_id VARCHAR(50),
    vendor_email VARCHAR(255),
    vendor_phone VARCHAR(50),
    invoice_number VARCHAR(100),
    vendor_invoice_number VARCHAR(100),
    purchase_order_number VARCHAR(100),
    invoice_date DATE,
    due_date DATE,
    service_period_start DATE,
    service_period_end DATE,
    total_amount DECIMAL(15,2),
    subtotal_amount DECIMAL(15,2),
    tax_amount DECIMAL(15,2),
    discount_amount DECIMAL(15,2),
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    
    -- Indexes for performance
    CONSTRAINT uk_download_token UNIQUE (download_token)
);

-- Create indexes for better query performance
CREATE INDEX idx_invoices_processing_status ON invoices(processing_status);
CREATE INDEX idx_invoices_ocr_status ON invoices(ocr_status);
CREATE INDEX idx_invoices_created_at ON invoices(created_at);
CREATE INDEX idx_invoices_vendor_name ON invoices(vendor_name);
CREATE INDEX idx_invoices_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_invoice_date ON invoices(invoice_date);
CREATE INDEX idx_invoices_download_token ON invoices(download_token);
CREATE INDEX idx_invoices_sender_email ON invoices(sender_email);

-- Create sequence for download token generation (PostgreSQL compatible)
-- H2 will auto-create sequences for BIGSERIAL

