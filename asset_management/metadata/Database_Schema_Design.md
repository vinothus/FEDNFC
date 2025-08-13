# Asset Management System - Database Schema Design

## Document Information
- **Created**: [Current Date]
- **Phase**: Phase 1 - Requirements and Design
- **Task**: 1.2.2 - Design database schema for assets and users
- **Status**: ✅ COMPLETED

---

## Database Overview

### Database Management System
- **Primary Database**: PostgreSQL 14+
- **Cache Layer**: Redis 7+
- **Hosting**: AWS RDS with Multi-AZ deployment
- **Backup Strategy**: Automated daily backups with 30-day retention
- **Scaling**: Read replicas for performance optimization

### Design Principles
1. **Normalization**: 3NF normalized to reduce redundancy
2. **Performance**: Strategic denormalization for query optimization
3. **Scalability**: Designed for horizontal scaling with proper indexing
4. **Audit Trail**: Complete audit logging for all critical operations
5. **Multi-tenancy**: Organization-based data isolation
6. **Referential Integrity**: Foreign key constraints and cascading rules

---

## Core Database Schema

### 1. User Management Tables

#### `organizations`
Stores organization/company information for multi-tenant support.

```sql
CREATE TABLE organizations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(255),
    logo_url VARCHAR(500),
    settings JSONB DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER,
    updated_by INTEGER
);

-- Indexes
CREATE INDEX idx_organizations_code ON organizations(code);
CREATE INDEX idx_organizations_is_active ON organizations(is_active);
```

#### `departments`
Hierarchical department structure within organizations.

```sql
CREATE TABLE departments (
    id SERIAL PRIMARY KEY,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    parent_department_id INTEGER REFERENCES departments(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    manager_id INTEGER, -- Will reference users(id) after users table creation
    level INTEGER DEFAULT 1,
    path TEXT, -- Materialized path for hierarchy queries (e.g., '/1/3/7/')
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER,
    updated_by INTEGER,
    UNIQUE(organization_id, code)
);

-- Indexes
CREATE INDEX idx_departments_organization_id ON departments(organization_id);
CREATE INDEX idx_departments_parent_id ON departments(parent_department_id);
CREATE INDEX idx_departments_path ON departments USING GIST(path);
CREATE INDEX idx_departments_manager_id ON departments(manager_id);
```

#### `roles`
System roles with permissions.

```sql
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    permissions JSONB DEFAULT '[]', -- Array of permission strings
    is_system_role BOOLEAN DEFAULT false, -- Admin, Manager, User are system roles
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default system roles
INSERT INTO roles (name, code, description, permissions, is_system_role) VALUES
('Administrator', 'ADMIN', 'Full system access', '["*"]', true),
('Manager', 'MANAGER', 'Department-level access', '["assets:read", "assets:write", "users:read", "reports:read"]', true),
('User', 'USER', 'Basic asset operations', '["assets:read", "assets:write:assigned"]', true);
```

#### `users`
System users with authentication and profile information.

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    department_id INTEGER REFERENCES departments(id) ON DELETE SET NULL,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    employee_id VARCHAR(50),
    avatar_url VARCHAR(500),
    last_login_at TIMESTAMP WITH TIME ZONE,
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    is_email_verified BOOLEAN DEFAULT false,
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expires_at TIMESTAMP WITH TIME ZONE,
    preferences JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER,
    updated_by INTEGER,
    UNIQUE(organization_id, username),
    UNIQUE(organization_id, email)
);

-- Add foreign key for department manager
ALTER TABLE departments ADD CONSTRAINT fk_departments_manager_id 
    FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL;

-- Indexes
CREATE INDEX idx_users_organization_id ON users(organization_id);
CREATE INDEX idx_users_department_id ON users(department_id);
CREATE INDEX idx_users_role_id ON users(role_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_is_active ON users(is_active);
```

### 2. Asset Management Tables

#### `asset_categories`
Asset categorization for organization and filtering.

```sql
CREATE TABLE asset_categories (
    id SERIAL PRIMARY KEY,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    parent_category_id INTEGER REFERENCES asset_categories(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    icon VARCHAR(100), -- Icon identifier for UI
    color VARCHAR(7), -- Hex color code for UI
    custom_fields JSONB DEFAULT '[]', -- Custom field definitions for this category
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER REFERENCES users(id),
    updated_by INTEGER REFERENCES users(id),
    UNIQUE(organization_id, code)
);

-- Indexes
CREATE INDEX idx_asset_categories_organization_id ON asset_categories(organization_id);
CREATE INDEX idx_asset_categories_parent_id ON asset_categories(parent_category_id);
```

#### `locations`
Physical locations where assets can be placed.

```sql
CREATE TABLE locations (
    id SERIAL PRIMARY KEY,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    parent_location_id INTEGER REFERENCES locations(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    address TEXT,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    floor VARCHAR(50),
    room VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER REFERENCES users(id),
    updated_by INTEGER REFERENCES users(id),
    UNIQUE(organization_id, code)
);

-- Indexes
CREATE INDEX idx_locations_organization_id ON locations(organization_id);
CREATE INDEX idx_locations_parent_id ON locations(parent_location_id);
CREATE INDEX idx_locations_coordinates ON locations(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
```

#### `assets`
Core asset information table.

```sql
CREATE TABLE assets (
    id SERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    category_id INTEGER REFERENCES asset_categories(id) ON DELETE SET NULL,
    current_location_id INTEGER REFERENCES locations(id) ON DELETE SET NULL,
    assigned_user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    assigned_department_id INTEGER REFERENCES departments(id) ON DELETE SET NULL,
    
    -- Basic Information
    name VARCHAR(255) NOT NULL,
    description TEXT,
    serial_number VARCHAR(255),
    model VARCHAR(255),
    manufacturer VARCHAR(255),
    
    -- Financial Information
    purchase_date DATE,
    purchase_price DECIMAL(15, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    warranty_expires_at DATE,
    
    -- Status and Lifecycle
    status VARCHAR(50) NOT NULL DEFAULT 'active', -- active, inactive, maintenance, disposed, lost
    condition VARCHAR(50) DEFAULT 'good', -- excellent, good, fair, poor, broken
    
    -- NFC/Tracking Information
    nfc_tag_id VARCHAR(255), -- Physical NFC tag identifier
    qr_code VARCHAR(255), -- QR code content if applicable
    barcode VARCHAR(255), -- Barcode content if applicable
    
    -- Custom Fields
    custom_fields JSONB DEFAULT '{}',
    
    -- Metadata
    tags TEXT[], -- Array of tags for searching
    notes TEXT,
    
    -- Timestamps
    last_scanned_at TIMESTAMP WITH TIME ZONE,
    last_scanned_by INTEGER REFERENCES users(id),
    last_scanned_location_id INTEGER REFERENCES locations(id),
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by INTEGER REFERENCES users(id),
    updated_by INTEGER REFERENCES users(id),
    
    -- Constraints
    CONSTRAINT valid_status CHECK (status IN ('active', 'inactive', 'maintenance', 'disposed', 'lost')),
    CONSTRAINT valid_condition CHECK (condition IN ('excellent', 'good', 'fair', 'poor', 'broken'))
);

-- Indexes
CREATE UNIQUE INDEX idx_assets_uuid ON assets(uuid);
CREATE INDEX idx_assets_organization_id ON assets(organization_id);
CREATE INDEX idx_assets_category_id ON assets(category_id);
CREATE INDEX idx_assets_current_location_id ON assets(current_location_id);
CREATE INDEX idx_assets_assigned_user_id ON assets(assigned_user_id);
CREATE INDEX idx_assets_assigned_department_id ON assets(assigned_department_id);
CREATE INDEX idx_assets_status ON assets(status);
CREATE INDEX idx_assets_serial_number ON assets(serial_number) WHERE serial_number IS NOT NULL;
CREATE INDEX idx_assets_nfc_tag_id ON assets(nfc_tag_id) WHERE nfc_tag_id IS NOT NULL;
CREATE INDEX idx_assets_name_search ON assets USING gin(to_tsvector('english', name));
CREATE INDEX idx_assets_tags ON assets USING gin(tags);
CREATE INDEX idx_assets_custom_fields ON assets USING gin(custom_fields);
```

#### `asset_history`
Audit trail for all asset changes and movements.

```sql
CREATE TABLE asset_history (
    id BIGSERIAL PRIMARY KEY,
    asset_id INTEGER NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    
    -- Change Information
    change_type VARCHAR(50) NOT NULL, -- created, updated, moved, assigned, scanned, status_changed
    field_name VARCHAR(100), -- Which field was changed (for updates)
    old_value TEXT, -- Previous value (JSON string for complex objects)
    new_value TEXT, -- New value (JSON string for complex objects)
    
    -- Context Information
    location_id INTEGER REFERENCES locations(id),
    user_id INTEGER REFERENCES users(id), -- User who made the change
    scan_method VARCHAR(50), -- nfc, qr, barcode, manual (for tracking scans)
    device_info JSONB, -- Information about the device used
    
    -- Metadata
    notes TEXT,
    ip_address INET,
    user_agent TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_change_type CHECK (change_type IN ('created', 'updated', 'moved', 'assigned', 'scanned', 'status_changed', 'deleted'))
);

-- Indexes
CREATE INDEX idx_asset_history_asset_id ON asset_history(asset_id);
CREATE INDEX idx_asset_history_organization_id ON asset_history(organization_id);
CREATE INDEX idx_asset_history_change_type ON asset_history(change_type);
CREATE INDEX idx_asset_history_user_id ON asset_history(user_id);
CREATE INDEX idx_asset_history_created_at ON asset_history(created_at DESC);
CREATE INDEX idx_asset_history_location_id ON asset_history(location_id);

-- Partitioning by created_at for performance (optional, for high-volume systems)
-- CREATE TABLE asset_history_y2024m01 PARTITION OF asset_history
-- FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

### 3. Synchronization and Offline Support Tables

#### `sync_queue`
Queue for offline operations that need to be synchronized.

```sql
CREATE TABLE sync_queue (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL, -- Unique device identifier
    
    -- Operation Information
    operation_type VARCHAR(50) NOT NULL, -- create, update, delete, scan
    table_name VARCHAR(100) NOT NULL, -- Target table for the operation
    record_id INTEGER, -- ID of the record being modified (null for creates)
    
    -- Data
    operation_data JSONB NOT NULL, -- The data for the operation
    conflict_resolution JSONB, -- Conflict resolution data if needed
    
    -- Status
    status VARCHAR(50) NOT NULL DEFAULT 'pending', -- pending, processing, completed, failed, conflict
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    -- Timestamps
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT valid_operation_type CHECK (operation_type IN ('create', 'update', 'delete', 'scan')),
    CONSTRAINT valid_status CHECK (status IN ('pending', 'processing', 'completed', 'failed', 'conflict'))
);

-- Indexes
CREATE INDEX idx_sync_queue_user_id ON sync_queue(user_id);
CREATE INDEX idx_sync_queue_device_id ON sync_queue(device_id);
CREATE INDEX idx_sync_queue_status ON sync_queue(status);
CREATE INDEX idx_sync_queue_created_at ON sync_queue(created_at);
CREATE INDEX idx_sync_queue_operation_type ON sync_queue(operation_type);
```

#### `device_sessions`
Track device sessions for offline/online status and sync management.

```sql
CREATE TABLE device_sessions (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    device_type VARCHAR(50) NOT NULL, -- ios, android, web
    device_name VARCHAR(255),
    app_version VARCHAR(50),
    os_version VARCHAR(50),
    
    -- Session Information
    last_sync_at TIMESTAMP WITH TIME ZONE,
    last_activity_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_online BOOLEAN DEFAULT true,
    fcm_token VARCHAR(500), -- For push notifications
    
    -- Session Metadata
    ip_address INET,
    user_agent TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(user_id, device_id)
);

-- Indexes
CREATE INDEX idx_device_sessions_user_id ON device_sessions(user_id);
CREATE INDEX idx_device_sessions_device_id ON device_sessions(device_id);
CREATE INDEX idx_device_sessions_last_activity ON device_sessions(last_activity_at);
CREATE INDEX idx_device_sessions_is_online ON device_sessions(is_online);
```

### 4. Notification and Communication Tables

#### `notifications`
System notifications for users.

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    recipient_user_id INTEGER REFERENCES users(id) ON DELETE CASCADE,
    recipient_department_id INTEGER REFERENCES departments(id) ON DELETE CASCADE,
    
    -- Notification Content
    type VARCHAR(100) NOT NULL, -- asset_assigned, asset_moved, system_alert, etc.
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'normal', -- low, normal, high, urgent
    
    -- Delivery Information
    delivery_method VARCHAR(50) NOT NULL, -- push, email, sms, in_app
    delivery_status VARCHAR(50) DEFAULT 'pending', -- pending, sent, delivered, failed
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    
    -- Related Data
    related_asset_id INTEGER REFERENCES assets(id) ON DELETE SET NULL,
    related_data JSONB,
    
    -- Metadata
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_priority CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    CONSTRAINT valid_delivery_method CHECK (delivery_method IN ('push', 'email', 'sms', 'in_app')),
    CONSTRAINT valid_delivery_status CHECK (delivery_status IN ('pending', 'sent', 'delivered', 'failed'))
);

-- Indexes
CREATE INDEX idx_notifications_recipient_user_id ON notifications(recipient_user_id);
CREATE INDEX idx_notifications_recipient_department_id ON notifications(recipient_department_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_delivery_status ON notifications(delivery_status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_related_asset_id ON notifications(related_asset_id);
```

### 5. System Configuration Tables

#### `system_settings`
Organization-level system configuration.

```sql
CREATE TABLE system_settings (
    id SERIAL PRIMARY KEY,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL, -- general, security, notifications, etc.
    key VARCHAR(255) NOT NULL,
    value JSONB,
    data_type VARCHAR(50) NOT NULL, -- string, number, boolean, json, array
    description TEXT,
    is_public BOOLEAN DEFAULT false, -- Can be accessed by non-admin users
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by INTEGER REFERENCES users(id),
    
    UNIQUE(organization_id, category, key)
);

-- Indexes
CREATE INDEX idx_system_settings_organization_id ON system_settings(organization_id);
CREATE INDEX idx_system_settings_category ON system_settings(category);
CREATE INDEX idx_system_settings_key ON system_settings(key);
```

#### `audit_logs`
Comprehensive audit trail for all system operations.

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    organization_id INTEGER NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(id) ON DELETE SET NULL,
    
    -- Action Information
    action VARCHAR(100) NOT NULL, -- login, logout, create_asset, update_user, etc.
    resource_type VARCHAR(100), -- asset, user, department, etc.
    resource_id INTEGER,
    
    -- Request Information
    ip_address INET,
    user_agent TEXT,
    request_method VARCHAR(10), -- GET, POST, PUT, DELETE
    request_path VARCHAR(500),
    
    -- Change Details
    changes JSONB, -- Before/after values for updates
    metadata JSONB, -- Additional context data
    
    -- Status
    status VARCHAR(20) DEFAULT 'success', -- success, failure, warning
    error_message TEXT,
    
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT valid_status CHECK (status IN ('success', 'failure', 'warning'))
);

-- Indexes
CREATE INDEX idx_audit_logs_organization_id ON audit_logs(organization_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_status ON audit_logs(status);

-- Partitioning by created_at for performance (recommended for high-volume)
-- This would be set up during deployment based on expected volume
```

---

## Database Functions and Triggers

### 1. Automatic Timestamp Updates

```sql
-- Function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply to all tables with updated_at columns
CREATE TRIGGER update_organizations_updated_at BEFORE UPDATE ON organizations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_departments_updated_at BEFORE UPDATE ON departments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_assets_updated_at BEFORE UPDATE ON assets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_asset_categories_updated_at BEFORE UPDATE ON asset_categories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_locations_updated_at BEFORE UPDATE ON locations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_system_settings_updated_at BEFORE UPDATE ON system_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_device_sessions_updated_at BEFORE UPDATE ON device_sessions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 2. Asset History Tracking

```sql
-- Function to automatically create asset history entries
CREATE OR REPLACE FUNCTION track_asset_changes()
RETURNS TRIGGER AS $$
DECLARE
    change_type VARCHAR(50);
    field_changes JSONB;
BEGIN
    -- Determine change type
    IF TG_OP = 'INSERT' THEN
        change_type := 'created';
        
        INSERT INTO asset_history (
            asset_id, organization_id, change_type, new_value, user_id
        ) VALUES (
            NEW.id, NEW.organization_id, change_type, 
            row_to_json(NEW)::text, NEW.created_by
        );
        
    ELSIF TG_OP = 'UPDATE' THEN
        change_type := 'updated';
        
        -- Track specific field changes
        IF OLD.current_location_id IS DISTINCT FROM NEW.current_location_id THEN
            INSERT INTO asset_history (
                asset_id, organization_id, change_type, field_name, 
                old_value, new_value, location_id, user_id
            ) VALUES (
                NEW.id, NEW.organization_id, 'moved', 'current_location_id',
                OLD.current_location_id::text, NEW.current_location_id::text,
                NEW.current_location_id, NEW.updated_by
            );
        END IF;
        
        IF OLD.status IS DISTINCT FROM NEW.status THEN
            INSERT INTO asset_history (
                asset_id, organization_id, change_type, field_name,
                old_value, new_value, user_id
            ) VALUES (
                NEW.id, NEW.organization_id, 'status_changed', 'status',
                OLD.status, NEW.status, NEW.updated_by
            );
        END IF;
        
        IF OLD.assigned_user_id IS DISTINCT FROM NEW.assigned_user_id THEN
            INSERT INTO asset_history (
                asset_id, organization_id, change_type, field_name,
                old_value, new_value, user_id
            ) VALUES (
                NEW.id, NEW.organization_id, 'assigned', 'assigned_user_id',
                OLD.assigned_user_id::text, NEW.assigned_user_id::text,
                NEW.updated_by
            );
        END IF;
        
    ELSIF TG_OP = 'DELETE' THEN
        change_type := 'deleted';
        
        INSERT INTO asset_history (
            asset_id, organization_id, change_type, old_value, user_id
        ) VALUES (
            OLD.id, OLD.organization_id, change_type,
            row_to_json(OLD)::text, OLD.updated_by
        );
        
        RETURN OLD;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

-- Apply trigger to assets table
CREATE TRIGGER track_asset_changes_trigger
    AFTER INSERT OR UPDATE OR DELETE ON assets
    FOR EACH ROW EXECUTE FUNCTION track_asset_changes();
```

### 3. Department Hierarchy Path Maintenance

```sql
-- Function to maintain materialized path for department hierarchy
CREATE OR REPLACE FUNCTION update_department_path()
RETURNS TRIGGER AS $$
DECLARE
    parent_path TEXT;
BEGIN
    IF NEW.parent_department_id IS NULL THEN
        NEW.path := '/' || NEW.id || '/';
        NEW.level := 1;
    ELSE
        SELECT path, level INTO parent_path, NEW.level 
        FROM departments 
        WHERE id = NEW.parent_department_id;
        
        NEW.path := parent_path || NEW.id || '/';
        NEW.level := NEW.level + 1;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

-- Apply trigger to departments table
CREATE TRIGGER update_department_path_trigger
    BEFORE INSERT OR UPDATE OF parent_department_id ON departments
    FOR EACH ROW EXECUTE FUNCTION update_department_path();
```

---

## Database Views

### 1. Asset Summary View

```sql
CREATE VIEW asset_summary AS
SELECT 
    a.id,
    a.uuid,
    a.name,
    a.serial_number,
    a.status,
    a.condition,
    ac.name as category_name,
    ac.color as category_color,
    l.name as location_name,
    l.code as location_code,
    u.first_name || ' ' || u.last_name as assigned_user_name,
    d.name as assigned_department_name,
    a.purchase_price,
    a.currency,
    a.purchase_date,
    a.last_scanned_at,
    scanner.first_name || ' ' || scanner.last_name as last_scanned_by_name,
    a.created_at,
    a.updated_at
FROM assets a
LEFT JOIN asset_categories ac ON a.category_id = ac.id
LEFT JOIN locations l ON a.current_location_id = l.id
LEFT JOIN users u ON a.assigned_user_id = u.id
LEFT JOIN departments d ON a.assigned_department_id = d.id
LEFT JOIN users scanner ON a.last_scanned_by = scanner.id;
```

### 2. User Permissions View

```sql
CREATE VIEW user_permissions AS
SELECT 
    u.id as user_id,
    u.username,
    u.email,
    u.first_name,
    u.last_name,
    r.name as role_name,
    r.code as role_code,
    r.permissions,
    d.name as department_name,
    d.path as department_path,
    o.name as organization_name,
    u.is_active
FROM users u
JOIN roles r ON u.role_id = r.id
LEFT JOIN departments d ON u.department_id = d.id
JOIN organizations o ON u.organization_id = o.id;
```

### 3. Asset Activity View

```sql
CREATE VIEW asset_activity AS
SELECT 
    ah.id,
    ah.asset_id,
    a.name as asset_name,
    a.uuid as asset_uuid,
    ah.change_type,
    ah.field_name,
    ah.old_value,
    ah.new_value,
    u.first_name || ' ' || u.last_name as user_name,
    l.name as location_name,
    ah.created_at,
    ah.notes
FROM asset_history ah
LEFT JOIN assets a ON ah.asset_id = a.id
LEFT JOIN users u ON ah.user_id = u.id
LEFT JOIN locations l ON ah.location_id = l.id
ORDER BY ah.created_at DESC;
```

---

## Performance Optimization

### 1. Database Indexes Summary

```sql
-- Critical indexes for performance
CREATE INDEX CONCURRENTLY idx_assets_org_status ON assets(organization_id, status);
CREATE INDEX CONCURRENTLY idx_assets_search_text ON assets USING gin(
    to_tsvector('english', coalesce(name, '') || ' ' || coalesce(description, '') || ' ' || coalesce(serial_number, ''))
);
CREATE INDEX CONCURRENTLY idx_asset_history_asset_date ON asset_history(asset_id, created_at DESC);
CREATE INDEX CONCURRENTLY idx_notifications_unread ON notifications(recipient_user_id, read_at) WHERE read_at IS NULL;
```

### 2. Partitioning Strategy

For high-volume tables, implement time-based partitioning:

```sql
-- Example partitioning for asset_history (monthly partitions)
CREATE TABLE asset_history_template (
    LIKE asset_history INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- Automated partition creation function
CREATE OR REPLACE FUNCTION create_monthly_partition(table_name TEXT, start_date DATE)
RETURNS void AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + INTERVAL '1 month';
    
    EXECUTE format('CREATE TABLE %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
                   partition_name, table_name, start_date, end_date);
END;
$$ LANGUAGE plpgsql;
```

---

## Data Integrity and Constraints

### 1. Check Constraints

```sql
-- Additional data validation constraints
ALTER TABLE assets ADD CONSTRAINT valid_purchase_price 
    CHECK (purchase_price >= 0);

ALTER TABLE assets ADD CONSTRAINT valid_purchase_date 
    CHECK (purchase_date <= CURRENT_DATE);

ALTER TABLE users ADD CONSTRAINT valid_email_format 
    CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

ALTER TABLE departments ADD CONSTRAINT valid_level 
    CHECK (level > 0 AND level <= 10);
```

### 2. Row Level Security (RLS)

```sql
-- Enable RLS for multi-tenant data isolation
ALTER TABLE assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE departments ENABLE ROW LEVEL SECURITY;

-- Example policy for assets (users can only see assets in their organization)
CREATE POLICY asset_organization_policy ON assets
    FOR ALL TO app_user
    USING (organization_id = current_setting('app.current_organization_id')::INTEGER);
```

---

## Database Migration Strategy

### 1. Version Control
- Use Flyway or Liquibase for database version control
- Sequential numbering: V1__initial_schema.sql, V2__add_asset_categories.sql
- Rollback scripts for each migration

### 2. Deployment Process
1. **Development**: Apply migrations automatically
2. **Staging**: Validate migrations with production-like data
3. **Production**: Careful migration with downtime planning

### 3. Data Migration Considerations
- Bulk import procedures for existing asset data
- Data validation and cleanup scripts
- Performance testing with expected data volumes

---

## Backup and Recovery

### 1. Backup Strategy
- **Full Backup**: Daily at 2 AM UTC
- **Incremental Backup**: Every 4 hours
- **Point-in-time Recovery**: Available for last 30 days
- **Cross-region Replication**: For disaster recovery

### 2. Recovery Procedures
- **RTO (Recovery Time Objective)**: 4 hours
- **RPO (Recovery Point Objective)**: 15 minutes
- **Automated failover**: For high availability

---

## Next Steps

### Immediate Actions (Phase 1)
1. ✅ **Task 1.2.2 COMPLETED**: Database schema designed
2. **Task 1.2.3**: Define comprehensive API specifications
3. **Task 1.2.4**: Create UI/UX wireframes and mockups

### Implementation Preparation (Phase 2)
- Set up development database with sample data
- Create database migration scripts
- Implement database connection pooling
- Set up monitoring and alerting

---

**Database Schema Status**: ✅ COMPLETE  
**Total Tables**: 15 core tables + 3 views  
**Next Task**: 1.2.3 - API Specifications Design  
**Dependencies Resolved**: System architecture, user stories  

*This schema will be refined during development based on performance testing and additional requirements.*
