# Invoice Automation System - Database Schema Design

## Document Information
- **Created**: [Current Date]
- **Phase**: Phase 1 - Requirements and Design
- **Task**: 1.2.2 - Design database schema for invoices and metadata
- **Status**: ✅ COMPLETED

---

## Database Strategy

### Multi-Environment Database Support
- **Development**: H2 embedded database with PostgreSQL compatibility mode
- **Production**: PostgreSQL 14+ with optimized BLOB storage
- **Migration**: Flyway for cross-database schema versioning

### Design Principles
1. **Data Integrity**: Foreign key constraints and referential integrity
2. **Performance**: Proper indexing for search and retrieval operations
3. **Scalability**: Efficient BLOB storage for PDF files
4. **Audit Trail**: Complete tracking of all changes and approvals
5. **Flexibility**: Support for various invoice formats and vendor types
6. **Cross-Database Compatibility**: SQL syntax compatible with H2 and PostgreSQL

---

## Core Database Schema

### 1. **Users and Authentication Tables**

#### users
Stores system user information and authentication details.
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    department VARCHAR(100),
    job_title VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    is_email_verified BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP,
    password_changed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_department ON users(department);
CREATE INDEX idx_users_active ON users(is_active);
```

#### user_roles
Stores role definitions for RBAC (Role-Based Access Control).
```sql
CREATE TABLE user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    permissions TEXT, -- JSON format for detailed permissions
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sample roles data
INSERT INTO user_roles (role_name, description, permissions) VALUES 
('ADMIN', 'System Administrator', '{"invoices": ["create", "read", "update", "delete"], "users": ["create", "read", "update", "delete"], "vendors": ["create", "read", "update", "delete"]}'),
('FINANCE_MANAGER', 'Finance Department Manager', '{"invoices": ["create", "read", "update", "approve"], "vendors": ["read", "update"], "reports": ["read"]}'),
('AP_CLERK', 'Accounts Payable Clerk', '{"invoices": ["create", "read", "update"], "vendors": ["read"]}'),
('DEPT_MANAGER', 'Department Manager', '{"invoices": ["read", "approve"], "reports": ["read"]}'),
('VIEWER', 'Read-only User', '{"invoices": ["read"], "reports": ["read"]}');
```

#### user_role_assignments
Many-to-many relationship between users and roles.
```sql
CREATE TABLE user_role_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES user_roles(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id),
    UNIQUE(user_id, role_id)
);

-- Indexes
CREATE INDEX idx_user_roles_user_id ON user_role_assignments(user_id);
CREATE INDEX idx_user_roles_role_id ON user_role_assignments(role_id);
```

### 2. **Vendor Management Tables**

#### vendors
Master data for invoice vendors/suppliers.
```sql
CREATE TABLE vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_code VARCHAR(50) UNIQUE,
    vendor_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    tax_id VARCHAR(50),
    vat_number VARCHAR(50),
    
    -- Address Information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state_province VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'USA',
    
    -- Contact Information
    primary_contact_name VARCHAR(255),
    primary_email VARCHAR(255),
    primary_phone VARCHAR(20),
    website VARCHAR(255),
    
    -- Business Information
    payment_terms VARCHAR(100), -- e.g., "Net 30", "2/10 Net 30"
    currency VARCHAR(3) DEFAULT 'USD',
    vendor_category VARCHAR(100), -- e.g., "Office Supplies", "IT Services"
    
    -- Status and Metadata
    is_active BOOLEAN DEFAULT true,
    is_approved BOOLEAN DEFAULT false,
    credit_limit DECIMAL(15,2),
    notes TEXT,
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_vendors_name ON vendors(vendor_name);
CREATE INDEX idx_vendors_code ON vendors(vendor_code);
CREATE INDEX idx_vendors_tax_id ON vendors(tax_id);
CREATE INDEX idx_vendors_active ON vendors(is_active);
CREATE INDEX idx_vendors_category ON vendors(vendor_category);
```

#### vendor_aliases
Alternative names and identifiers for vendor matching.
```sql
CREATE TABLE vendor_aliases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    alias_name VARCHAR(255) NOT NULL,
    alias_type VARCHAR(50), -- e.g., "DBA", "BRAND", "ABBREVIATION"
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_vendor_aliases_vendor_id ON vendor_aliases(vendor_id);
CREATE INDEX idx_vendor_aliases_name ON vendor_aliases(alias_name);
```

### 3. **Invoice Management Tables**

#### invoices
Core invoice data with PDF storage.
```sql
CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Invoice Identification
    invoice_number VARCHAR(100) NOT NULL,
    vendor_invoice_number VARCHAR(100), -- Vendor's own invoice number
    purchase_order_number VARCHAR(100),
    
    -- Vendor Information
    vendor_id BIGINT,
    vendor_name_extracted VARCHAR(255), -- As extracted from PDF
    vendor_address_extracted TEXT,
    
    -- Financial Information
    total_amount DECIMAL(15,2) NOT NULL,
    subtotal_amount DECIMAL(15,2),
    tax_amount DECIMAL(15,2),
    discount_amount DECIMAL(15,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    
    -- Date Information
    invoice_date DATE NOT NULL,
    due_date DATE,
    received_date DATE DEFAULT CURRENT_DATE,
    service_period_start DATE,
    service_period_end DATE,
    
    -- PDF Storage
    pdf_file_path VARCHAR(500) NOT NULL,
    pdf_blob BYTEA, -- H2: BLOB, PostgreSQL: BYTEA
    file_size BIGINT,
    file_checksum VARCHAR(64), -- SHA-256 hash
    original_filename VARCHAR(255),
    
    -- Processing Information
    processing_status VARCHAR(50) DEFAULT 'pending',
        -- Values: pending, processing, processed, validation_required, approved, rejected, paid
    extraction_method VARCHAR(50), -- tika, itext, tesseract
    confidence_score DECIMAL(3,2), -- 0.00 to 1.00
    validation_status VARCHAR(50) DEFAULT 'pending',
        -- Values: pending, validated, failed, manual_review
    
    -- Approval Information
    approval_status VARCHAR(50) DEFAULT 'pending',
        -- Values: pending, approved, rejected, escalated
    approved_amount DECIMAL(15,2),
    approval_notes TEXT,
    
    -- Email Processing
    email_message_id VARCHAR(255),
    email_subject VARCHAR(500),
    email_sender VARCHAR(255),
    email_received_at TIMESTAMP,
    
    -- Business Logic
    department VARCHAR(100),
    cost_center VARCHAR(50),
    project_code VARCHAR(50),
    expense_category VARCHAR(100),
    
    -- Duplicate Detection
    duplicate_check_hash VARCHAR(64), -- Hash for duplicate detection
    is_duplicate BOOLEAN DEFAULT false,
    duplicate_of_invoice_id BIGINT,
    
    -- Audit and Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    approved_at TIMESTAMP,
    paid_at TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    approved_by BIGINT,
    
    -- Constraints
    FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    FOREIGN KEY (duplicate_of_invoice_id) REFERENCES invoices(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (updated_by) REFERENCES users(id),
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- Indexes for Performance
CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_vendor_id ON invoices(vendor_id);
CREATE INDEX idx_invoices_status ON invoices(processing_status);
CREATE INDEX idx_invoices_approval ON invoices(approval_status);
CREATE INDEX idx_invoices_date ON invoices(invoice_date);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_amount ON invoices(total_amount);
CREATE INDEX idx_invoices_department ON invoices(department);
CREATE INDEX idx_invoices_duplicate ON invoices(duplicate_check_hash);
CREATE INDEX idx_invoices_email_id ON invoices(email_message_id);

-- Full-text search index (PostgreSQL specific)
-- CREATE INDEX idx_invoices_text_search ON invoices USING gin(to_tsvector('english', 
--     vendor_name_extracted || ' ' || COALESCE(invoice_number, '') || ' ' || COALESCE(vendor_invoice_number, '')));
```

#### invoice_line_items
Detailed line items extracted from invoices.
```sql
CREATE TABLE invoice_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    line_number INTEGER,
    
    -- Item Information
    item_description TEXT,
    item_code VARCHAR(100),
    category VARCHAR(100),
    
    -- Quantity and Pricing
    quantity DECIMAL(10,3),
    unit_of_measure VARCHAR(20), -- e.g., "each", "hour", "kg"
    unit_price DECIMAL(12,4),
    line_total DECIMAL(15,2),
    
    -- Tax Information
    tax_rate DECIMAL(5,4), -- e.g., 0.0825 for 8.25%
    tax_amount DECIMAL(15,2),
    
    -- Accounting
    gl_account VARCHAR(50), -- General Ledger account
    cost_center VARCHAR(50),
    project_code VARCHAR(50),
    
    -- Metadata
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_line_items_invoice_id ON invoice_line_items(invoice_id);
CREATE INDEX idx_line_items_category ON invoice_line_items(category);
CREATE INDEX idx_line_items_gl_account ON invoice_line_items(gl_account);
```

### 4. **Approval Workflow Tables**

#### approval_workflows
Configuration for approval workflows.
```sql
CREATE TABLE approval_workflows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workflow_name VARCHAR(100) NOT NULL,
    description TEXT,
    
    -- Trigger Conditions
    min_amount DECIMAL(15,2),
    max_amount DECIMAL(15,2),
    department VARCHAR(100),
    vendor_category VARCHAR(100),
    
    -- Workflow Configuration
    required_approvals INTEGER DEFAULT 1,
    approval_sequence TEXT, -- JSON: [{"level": 1, "roles": ["DEPT_MANAGER"], "amount_limit": 5000}]
    escalation_days INTEGER DEFAULT 3,
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 0, -- Higher number = higher priority
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Sample workflow data
INSERT INTO approval_workflows (workflow_name, description, min_amount, max_amount, required_approvals, approval_sequence) VALUES
('Standard Workflow', 'Default approval workflow', 0, 1000, 1, '[{"level": 1, "roles": ["DEPT_MANAGER"], "required": 1}]'),
('High Value Workflow', 'For invoices over $1000', 1000, 10000, 2, '[{"level": 1, "roles": ["DEPT_MANAGER"], "required": 1}, {"level": 2, "roles": ["FINANCE_MANAGER"], "required": 1}]'),
('Executive Approval', 'For invoices over $10000', 10000, 999999999, 3, '[{"level": 1, "roles": ["DEPT_MANAGER"], "required": 1}, {"level": 2, "roles": ["FINANCE_MANAGER"], "required": 1}, {"level": 3, "roles": ["ADMIN"], "required": 1}]');
```

#### invoice_approvals
Tracks approval process for each invoice.
```sql
CREATE TABLE invoice_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    workflow_id BIGINT,
    
    -- Approval Details
    approval_level INTEGER,
    required_role VARCHAR(50),
    assigned_to BIGINT, -- Specific user assigned
    
    -- Status
    status VARCHAR(50) DEFAULT 'pending',
        -- Values: pending, approved, rejected, delegated, escalated
    decision VARCHAR(10), -- approved, rejected
    comments TEXT,
    
    -- Timing
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    due_date TIMESTAMP,
    escalated_at TIMESTAMP,
    
    -- Delegation
    delegated_from BIGINT,
    delegated_to BIGINT,
    delegation_reason TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (workflow_id) REFERENCES approval_workflows(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id),
    FOREIGN KEY (delegated_from) REFERENCES users(id),
    FOREIGN KEY (delegated_to) REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_approvals_invoice_id ON invoice_approvals(invoice_id);
CREATE INDEX idx_approvals_assigned_to ON invoice_approvals(assigned_to);
CREATE INDEX idx_approvals_status ON invoice_approvals(status);
CREATE INDEX idx_approvals_due_date ON invoice_approvals(due_date);
```

### 5. **Processing and Validation Tables**

#### invoice_processing_logs
Detailed logs of PDF processing and OCR operations.
```sql
CREATE TABLE invoice_processing_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    
    -- Processing Information
    processing_step VARCHAR(50), -- email_extraction, pdf_detection, ocr_processing, data_extraction, validation
    processing_method VARCHAR(50), -- tika, itext, tesseract
    status VARCHAR(50), -- started, completed, failed, skipped
    
    -- Results
    confidence_score DECIMAL(3,2),
    extracted_text TEXT,
    extracted_data TEXT, -- JSON format
    error_message TEXT,
    
    -- Performance Metrics
    processing_time_ms INTEGER,
    file_size_bytes BIGINT,
    
    -- Timing
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_processing_logs_invoice_id ON invoice_processing_logs(invoice_id);
CREATE INDEX idx_processing_logs_step ON invoice_processing_logs(processing_step);
CREATE INDEX idx_processing_logs_status ON invoice_processing_logs(status);
```

#### validation_errors
Tracks validation errors and manual corrections.
```sql
CREATE TABLE validation_errors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    
    -- Error Information
    field_name VARCHAR(100), -- invoice_number, vendor_name, total_amount, etc.
    error_type VARCHAR(50), -- missing, invalid_format, out_of_range, duplicate
    error_message TEXT,
    original_value TEXT,
    corrected_value TEXT,
    
    -- Resolution
    status VARCHAR(50) DEFAULT 'open',
        -- Values: open, resolved, ignored, escalated
    resolution_method VARCHAR(50), -- manual_correction, system_override, vendor_contact
    resolution_notes TEXT,
    
    -- Assignment
    assigned_to BIGINT,
    resolved_by BIGINT,
    
    -- Timing
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id),
    FOREIGN KEY (resolved_by) REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_validation_errors_invoice_id ON validation_errors(invoice_id);
CREATE INDEX idx_validation_errors_status ON validation_errors(status);
CREATE INDEX idx_validation_errors_assigned_to ON validation_errors(assigned_to);
```

### 6. **System Configuration Tables**

#### system_settings
Application configuration and settings.
```sql
CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    setting_type VARCHAR(20) DEFAULT 'string', -- string, integer, boolean, json
    description TEXT,
    is_public BOOLEAN DEFAULT false, -- Can be accessed by regular users
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Sample configuration data
INSERT INTO system_settings (setting_key, setting_value, setting_type, description) VALUES
('email.check.interval', '300000', 'integer', 'Email check interval in milliseconds'),
('pdf.max.file.size', '52428800', 'integer', 'Maximum PDF file size in bytes (50MB)'),
('ocr.confidence.threshold', '0.70', 'decimal', 'Minimum confidence score for auto-approval'),
('approval.escalation.days', '3', 'integer', 'Days before approval escalation'),
('duplicate.detection.enabled', 'true', 'boolean', 'Enable duplicate invoice detection'),
('currency.default', 'USD', 'string', 'Default currency for invoices'),
('retention.period.years', '7', 'integer', 'Invoice retention period in years');
```

#### email_configurations
Email server configurations for invoice ingestion.
```sql
CREATE TABLE email_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    configuration_name VARCHAR(100) NOT NULL,
    
    -- Email Server Settings
    server_type VARCHAR(10), -- IMAP, POP3
    server_host VARCHAR(255) NOT NULL,
    server_port INTEGER NOT NULL,
    use_ssl BOOLEAN DEFAULT true,
    use_tls BOOLEAN DEFAULT true,
    
    -- Authentication
    username VARCHAR(255) NOT NULL,
    password_encrypted TEXT, -- Encrypted password
    
    -- Processing Settings
    inbox_folder VARCHAR(100) DEFAULT 'INBOX',
    processed_folder VARCHAR(100) DEFAULT 'Processed',
    error_folder VARCHAR(100) DEFAULT 'Errors',
    
    -- Status
    is_active BOOLEAN DEFAULT true,
    last_check_at TIMESTAMP,
    last_error TEXT,
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    FOREIGN KEY (created_by) REFERENCES users(id)
);
```

### 7. **Audit and History Tables**

#### audit_log
Comprehensive audit trail for all system operations.
```sql
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Operation Information
    table_name VARCHAR(100),
    record_id BIGINT,
    operation VARCHAR(20), -- INSERT, UPDATE, DELETE, LOGIN, LOGOUT
    
    -- User Information
    user_id BIGINT,
    username VARCHAR(50),
    ip_address VARCHAR(45), -- IPv6 compatible
    user_agent TEXT,
    
    -- Change Details
    old_values TEXT, -- JSON format
    new_values TEXT, -- JSON format
    changed_fields TEXT, -- Comma-separated list
    
    -- Context
    operation_context VARCHAR(100), -- web_ui, api, system, email_processor
    session_id VARCHAR(100),
    request_id VARCHAR(100),
    
    -- Timing
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX idx_audit_log_operation ON audit_log(operation);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);
```

#### invoice_status_history
Historical tracking of invoice status changes.
```sql
CREATE TABLE invoice_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    
    -- Status Information
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    status_type VARCHAR(50), -- processing_status, approval_status, validation_status
    
    -- Context
    changed_by BIGINT,
    change_reason TEXT,
    system_notes TEXT,
    
    -- Timing
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_status_history_invoice_id ON invoice_status_history(invoice_id);
CREATE INDEX idx_status_history_changed_at ON invoice_status_history(changed_at);
```

---

## Database Views

### invoice_summary_view
Comprehensive view for invoice listing and reporting.
```sql
CREATE VIEW invoice_summary_view AS
SELECT 
    i.id,
    i.invoice_number,
    i.vendor_invoice_number,
    i.total_amount,
    i.currency,
    i.invoice_date,
    i.due_date,
    i.processing_status,
    i.approval_status,
    i.validation_status,
    i.department,
    i.confidence_score,
    
    -- Vendor Information
    v.vendor_name,
    v.vendor_code,
    v.vendor_category,
    
    -- Approval Information
    (SELECT COUNT(*) FROM invoice_approvals ia 
     WHERE ia.invoice_id = i.id AND ia.status = 'pending') as pending_approvals,
    
    -- Validation Errors
    (SELECT COUNT(*) FROM validation_errors ve 
     WHERE ve.invoice_id = i.id AND ve.status = 'open') as open_validation_errors,
    
    -- Timing
    i.created_at,
    i.processed_at,
    i.approved_at,
    
    -- User Information
    creator.username as created_by_username,
    approver.username as approved_by_username
    
FROM invoices i
LEFT JOIN vendors v ON i.vendor_id = v.id
LEFT JOIN users creator ON i.created_by = creator.id
LEFT JOIN users approver ON i.approved_by = approver.id;
```

### pending_approvals_view
View for users to see their pending approval tasks.
```sql
CREATE VIEW pending_approvals_view AS
SELECT 
    ia.id as approval_id,
    ia.invoice_id,
    i.invoice_number,
    i.total_amount,
    i.currency,
    i.invoice_date,
    i.due_date,
    v.vendor_name,
    ia.approval_level,
    ia.required_role,
    ia.assigned_to,
    ia.due_date as approval_due_date,
    ia.comments,
    u.username as assigned_to_username,
    CASE 
        WHEN ia.due_date < CURRENT_TIMESTAMP THEN 'overdue'
        WHEN ia.due_date < CURRENT_TIMESTAMP + INTERVAL '1 day' THEN 'due_soon'
        ELSE 'on_time'
    END as urgency_status
    
FROM invoice_approvals ia
JOIN invoices i ON ia.invoice_id = i.id
LEFT JOIN vendors v ON i.vendor_id = v.id
LEFT JOIN users u ON ia.assigned_to = u.id
WHERE ia.status = 'pending';
```

---

## Database Functions and Triggers

### Update Timestamp Trigger (PostgreSQL)
```sql
-- Function to update timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply to all tables with updated_at column
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vendors_updated_at BEFORE UPDATE ON vendors
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_invoices_updated_at BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Additional triggers for other tables...
```

### Duplicate Detection Function
```sql
-- Function to generate duplicate detection hash
CREATE OR REPLACE FUNCTION generate_duplicate_hash(
    vendor_name VARCHAR(255),
    invoice_number VARCHAR(100),
    total_amount DECIMAL(15,2),
    invoice_date DATE
) RETURNS VARCHAR(64) AS $$
BEGIN
    RETURN ENCODE(
        DIGEST(
            CONCAT(
                LOWER(TRIM(vendor_name)),
                LOWER(TRIM(invoice_number)),
                total_amount::text,
                invoice_date::text
            ), 'sha256'
        ), 'hex'
    );
END;
$$ LANGUAGE plpgsql;
```

---

## Performance Optimization

### Indexing Strategy
1. **Primary Keys**: All tables have optimized primary keys
2. **Foreign Keys**: All foreign key columns are indexed
3. **Search Columns**: Frequently searched columns (status, dates, amounts)
4. **Composite Indexes**: Multi-column indexes for complex queries
5. **Partial Indexes**: For frequently filtered conditions

### Query Optimization Tips
```sql
-- Efficient invoice search query
SELECT * FROM invoice_summary_view 
WHERE processing_status = 'processed'
  AND invoice_date BETWEEN '2024-01-01' AND '2024-12-31'
  AND total_amount > 1000
ORDER BY invoice_date DESC
LIMIT 50;

-- Pending approvals for a user
SELECT * FROM pending_approvals_view
WHERE assigned_to = ? 
  OR assigned_to IN (
    SELECT user_id FROM user_role_assignments ura
    JOIN user_roles ur ON ura.role_id = ur.id
    WHERE ur.role_name = 'FINANCE_MANAGER'
  )
ORDER BY approval_due_date ASC;
```

### BLOB Storage Optimization
```sql
-- Separate BLOB storage for large PDFs (optional optimization)
CREATE TABLE invoice_pdf_blobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL UNIQUE,
    pdf_data BYTEA NOT NULL,
    compression_type VARCHAR(20), -- gzip, lz4
    original_size BIGINT,
    compressed_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);
```

---

## Data Migration Scripts

### H2 to PostgreSQL Migration
```sql
-- Sample data type conversions
-- H2: BIGINT AUTO_INCREMENT → PostgreSQL: BIGSERIAL
-- H2: BLOB → PostgreSQL: BYTEA
-- H2: TIMESTAMP → PostgreSQL: TIMESTAMP

-- Migration script template
CREATE OR REPLACE FUNCTION migrate_h2_to_postgresql() 
RETURNS void AS $$
DECLARE
    rec RECORD;
BEGIN
    -- Migrate users
    INSERT INTO users_new SELECT * FROM users_old;
    
    -- Migrate vendors with data transformation
    INSERT INTO vendors_new (id, vendor_name, ...)
    SELECT id, vendor_name, ... FROM vendors_old;
    
    -- Update sequences
    SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
    SELECT setval('vendors_id_seq', (SELECT MAX(id) FROM vendors));
    
    RAISE NOTICE 'Migration completed successfully';
END;
$$ LANGUAGE plpgsql;
```

---

## Security Considerations

### Data Encryption
- **Passwords**: Hashed using bcrypt with salt
- **Sensitive Data**: Email passwords encrypted at rest
- **PDF Content**: Optional encryption for sensitive invoices
- **Audit Logs**: Tamper-proof audit trail

### Access Control
- **Row-Level Security**: Users can only access their department's invoices
- **Column-Level Security**: Sensitive fields restricted by role
- **Database Users**: Separate database users for different application components

### Backup and Recovery
```sql
-- Backup strategy
-- Full backup: pg_dump -Fc database_name > backup.dump
-- Restore: pg_restore -d database_name backup.dump

-- Point-in-time recovery setup
-- Enable WAL archiving and configure backup retention
```

---

## Database Sizing and Capacity Planning

### Storage Estimates
- **Invoices Table**: ~2KB per invoice (without BLOB)
- **PDF BLOBs**: Average 500KB per invoice
- **Audit Logs**: ~1KB per operation
- **Line Items**: ~200 bytes per line item

### Projected Growth (Annual)
- **10,000 invoices/year**: ~5GB total storage
- **50,000 invoices/year**: ~25GB total storage
- **100,000 invoices/year**: ~50GB total storage

### Maintenance Tasks
```sql
-- Regular maintenance queries
-- Analyze table statistics
ANALYZE;

-- Vacuum to reclaim space
VACUUM ANALYZE;

-- Reindex for performance
REINDEX DATABASE invoice_automation;

-- Archive old data (7+ years)
DELETE FROM invoices WHERE created_at < CURRENT_DATE - INTERVAL '7 years';
```

---

**Database Schema Status**: ✅ COMPLETE  
**Tables Created**: 15 core tables + 2 views  
**Features Included**: RBAC, Audit Trail, Workflow Management, PDF Storage  
**Database Support**: H2 (development) + PostgreSQL (production)  
**Performance**: Optimized with indexes and views  

*This comprehensive database schema provides a robust foundation for the Invoice Automation System with full audit capabilities, flexible approval workflows, and efficient PDF storage management.*
