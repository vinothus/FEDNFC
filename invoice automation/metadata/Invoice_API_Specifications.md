# Invoice Automation System - API Specifications

## Document Information
- **Created**: [Current Date]
- **Phase**: Phase 1 - Requirements and Design
- **Task**: 1.2.3 - Design API specifications for invoice processing
- **Status**: ✅ COMPLETED

---

## API Overview

### Architecture Style
- **Type**: RESTful API with JSON payloads
- **Authentication**: JWT Bearer tokens
- **Authorization**: Role-Based Access Control (RBAC)
- **Versioning**: URI versioning (`/api/v1/`)
- **Content Type**: `application/json`
- **File Uploads**: `multipart/form-data` for PDF uploads

### Base URL
- **Development**: `http://localhost:8080/api/v1`
- **Production**: `https://invoice.company.com/api/v1`

### API Design Principles
1. **RESTful**: Standard HTTP methods and status codes
2. **Consistent**: Uniform response structure and naming conventions
3. **Stateless**: Each request contains all necessary information
4. **Paginated**: Large result sets are paginated
5. **Filtered**: Comprehensive filtering and search capabilities
6. **Secure**: All endpoints require authentication and authorization

---

## Authentication & Authorization

### Authentication Endpoints

#### POST /api/v1/auth/login
Authenticate user and obtain JWT token.

**Request Body:**
```json
{
  "username": "john.doe",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 28800,
    "refreshToken": "rt_abc123...",
    "user": {
      "id": 1,
      "username": "john.doe",
      "email": "john.doe@company.com",
      "firstName": "John",
      "lastName": "Doe",
      "department": "Finance",
      "roles": ["FINANCE_MANAGER"]
    }
  },
  "message": "Login successful"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid credentials
- `403 Forbidden`: Account locked or disabled
- `429 Too Many Requests`: Rate limit exceeded

#### POST /api/v1/auth/refresh
Refresh JWT token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "rt_abc123..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 28800
  }
}
```

#### POST /api/v1/auth/logout
Invalidate current token.

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

### Authorization Headers
All protected endpoints require:
```
Authorization: Bearer <jwt_token>
```

---

## Standard Response Format

### Success Response
```json
{
  "success": true,
  "data": {
    // Response data
  },
  "message": "Operation completed successfully",
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_abc123"
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      {
        "field": "invoiceNumber",
        "message": "Invoice number is required"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req_abc123"
}
```

### Paginated Response
```json
{
  "success": true,
  "data": {
    "content": [
      // Array of items
    ],
    "page": {
      "number": 1,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8,
      "first": false,
      "last": false
    }
  }
}
```

---

## Invoice Management Endpoints

### GET /api/v1/invoices
Retrieve paginated list of invoices with filtering and search.

**Query Parameters:**
- `page` (integer, default: 0): Page number
- `size` (integer, default: 20, max: 100): Page size
- `sort` (string, default: "createdAt,desc"): Sort criteria
- `search` (string): Global search across multiple fields
- `status` (string): Filter by processing status
- `approvalStatus` (string): Filter by approval status
- `vendorId` (integer): Filter by vendor ID
- `department` (string): Filter by department
- `dateFrom` (date): Filter invoices from date (YYYY-MM-DD)
- `dateTo` (date): Filter invoices to date (YYYY-MM-DD)
- `amountMin` (decimal): Filter by minimum amount
- `amountMax` (decimal): Filter by maximum amount

**Example Request:**
```
GET /api/v1/invoices?page=0&size=20&status=processed&dateFrom=2024-01-01&sort=invoiceDate,desc
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "invoiceNumber": "INV-2024-001",
        "vendorInvoiceNumber": "VND-456",
        "vendor": {
          "id": 10,
          "name": "Office Supplies Inc",
          "code": "OS001"
        },
        "totalAmount": 1250.00,
        "currency": "USD",
        "invoiceDate": "2024-01-15",
        "dueDate": "2024-02-14",
        "processingStatus": "processed",
        "approvalStatus": "pending",
        "validationStatus": "validated",
        "department": "IT",
        "confidenceScore": 0.95,
        "createdAt": "2024-01-15T09:30:00Z",
        "processedAt": "2024-01-15T09:35:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true
    }
  }
}
```

**Required Permissions:** `invoices:read`

### GET /api/v1/invoices/{id}
Retrieve detailed information for a specific invoice.

**Path Parameters:**
- `id` (integer): Invoice ID

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "invoiceNumber": "INV-2024-001",
    "vendorInvoiceNumber": "VND-456",
    "purchaseOrderNumber": "PO-2024-789",
    "vendor": {
      "id": 10,
      "name": "Office Supplies Inc",
      "code": "OS001",
      "category": "Office Supplies",
      "paymentTerms": "Net 30"
    },
    "financial": {
      "totalAmount": 1250.00,
      "subtotalAmount": 1150.00,
      "taxAmount": 100.00,
      "discountAmount": 0.00,
      "currency": "USD"
    },
    "dates": {
      "invoiceDate": "2024-01-15",
      "dueDate": "2024-02-14",
      "receivedDate": "2024-01-15",
      "servicePeriodStart": "2024-01-01",
      "servicePeriodEnd": "2024-01-31"
    },
    "processing": {
      "status": "processed",
      "extractionMethod": "tika",
      "confidenceScore": 0.95,
      "validationStatus": "validated"
    },
    "approval": {
      "status": "pending",
      "approvedAmount": null,
      "approvedBy": null,
      "approvedAt": null,
      "notes": null
    },
    "file": {
      "originalFilename": "invoice_001.pdf",
      "fileSize": 245760,
      "fileChecksum": "sha256:abc123...",
      "pdfAvailable": true
    },
    "lineItems": [
      {
        "id": 1,
        "lineNumber": 1,
        "description": "Dell Laptop - Latitude 5520",
        "quantity": 2,
        "unitPrice": 575.00,
        "lineTotal": 1150.00,
        "taxRate": 0.0875,
        "taxAmount": 100.00,
        "category": "Hardware"
      }
    ],
    "audit": {
      "createdAt": "2024-01-15T09:30:00Z",
      "updatedAt": "2024-01-15T09:35:00Z",
      "processedAt": "2024-01-15T09:35:00Z",
      "createdBy": {
        "id": 1,
        "username": "system",
        "name": "System"
      }
    }
  }
}
```

**Error Responses:**
- `404 Not Found`: Invoice not found
- `403 Forbidden`: No permission to access this invoice

**Required Permissions:** `invoices:read`

### POST /api/v1/invoices/upload
Upload a PDF invoice for processing.

**Content-Type:** `multipart/form-data`

**Form Parameters:**
- `file` (file): PDF file (max 50MB)
- `department` (string, optional): Department for the invoice
- `costCenter` (string, optional): Cost center code
- `projectCode` (string, optional): Project code
- `notes` (string, optional): Additional notes

**Example Request:**
```bash
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -F "file=@invoice.pdf" \
  -F "department=IT" \
  -F "costCenter=IT001" \
  https://api.example.com/api/v1/invoices/upload
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 123,
    "invoiceNumber": "INV-2024-123",
    "processingStatus": "processing",
    "estimatedProcessingTime": "30 seconds",
    "uploadedAt": "2024-01-15T10:30:00Z"
  },
  "message": "Invoice uploaded successfully and processing started"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid file format or size
- `413 Payload Too Large`: File exceeds size limit
- `422 Unprocessable Entity`: PDF is corrupted or unreadable

**Required Permissions:** `invoices:create`

### PUT /api/v1/invoices/{id}
Update invoice data (manual corrections).

**Path Parameters:**
- `id` (integer): Invoice ID

**Request Body:**
```json
{
  "invoiceNumber": "INV-2024-001-CORRECTED",
  "vendorInvoiceNumber": "VND-456",
  "totalAmount": 1275.00,
  "subtotalAmount": 1175.00,
  "taxAmount": 100.00,
  "invoiceDate": "2024-01-15",
  "dueDate": "2024-02-14",
  "department": "IT",
  "costCenter": "IT001",
  "correctionReason": "Amount was incorrectly extracted",
  "lineItems": [
    {
      "id": 1,
      "description": "Dell Laptop - Latitude 5520 (Corrected)",
      "quantity": 2,
      "unitPrice": 587.50,
      "lineTotal": 1175.00
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "invoiceNumber": "INV-2024-001-CORRECTED",
    "processingStatus": "manual_corrected",
    "validationStatus": "pending",
    "updatedAt": "2024-01-15T11:00:00Z",
    "correctedBy": {
      "id": 5,
      "username": "jane.clerk",
      "name": "Jane Clerk"
    }
  },
  "message": "Invoice updated successfully"
}
```

**Required Permissions:** `invoices:update`

### GET /api/v1/invoices/{id}/pdf
Download or view the original PDF file.

**Path Parameters:**
- `id` (integer): Invoice ID

**Query Parameters:**
- `download` (boolean, default: false): Force download vs inline view

**Response (200 OK):**
```
Content-Type: application/pdf
Content-Length: 245760
Content-Disposition: inline; filename="invoice_001.pdf"

[PDF binary content]
```

**Error Responses:**
- `404 Not Found`: Invoice or PDF not found
- `403 Forbidden`: No permission to access PDF

**Required Permissions:** `invoices:read`

### POST /api/v1/invoices/{id}/reprocess
Reprocess an invoice through the OCR pipeline.

**Path Parameters:**
- `id` (integer): Invoice ID

**Request Body:**
```json
{
  "extractionMethod": "tesseract", // Optional: force specific method
  "reason": "Low confidence score on initial processing"
}
```

**Response (202 Accepted):**
```json
{
  "success": true,
  "data": {
    "processingId": "proc_abc123",
    "status": "queued",
    "estimatedTime": "45 seconds"
  },
  "message": "Invoice reprocessing started"
}
```

**Required Permissions:** `invoices:process`

---

## Approval Workflow Endpoints

### GET /api/v1/approvals/pending
Get pending approvals for the current user.

**Query Parameters:**
- `page` (integer, default: 0): Page number
- `size` (integer, default: 20): Page size
- `urgency` (string): Filter by urgency (overdue, due_soon, on_time)
- `department` (string): Filter by department
- `amountMin` (decimal): Filter by minimum amount

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "approvalId": 45,
        "invoice": {
          "id": 123,
          "invoiceNumber": "INV-2024-123",
          "totalAmount": 2500.00,
          "currency": "USD",
          "invoiceDate": "2024-01-15",
          "dueDate": "2024-02-14",
          "vendor": {
            "name": "IT Services Corp"
          }
        },
        "approval": {
          "level": 1,
          "requiredRole": "DEPT_MANAGER",
          "dueDate": "2024-01-17T17:00:00Z",
          "urgencyStatus": "due_soon",
          "assignedAt": "2024-01-15T14:30:00Z"
        }
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 3,
      "totalPages": 1
    }
  }
}
```

**Required Permissions:** `approvals:read`

### POST /api/v1/approvals/{approvalId}/approve
Approve an invoice.

**Path Parameters:**
- `approvalId` (integer): Approval ID

**Request Body:**
```json
{
  "decision": "approved",
  "approvedAmount": 2500.00, // Optional: different from invoice amount
  "comments": "Approved for IT equipment purchase",
  "conditions": [] // Optional: approval conditions
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "approvalId": 45,
    "status": "approved",
    "approvedBy": {
      "id": 10,
      "username": "manager.it",
      "name": "IT Manager"
    },
    "approvedAt": "2024-01-16T09:15:00Z",
    "nextApprovalLevel": 2,
    "workflowStatus": "pending_next_level"
  },
  "message": "Invoice approved successfully"
}
```

**Required Permissions:** `approvals:approve`

### POST /api/v1/approvals/{approvalId}/reject
Reject an invoice.

**Path Parameters:**
- `approvalId` (integer): Approval ID

**Request Body:**
```json
{
  "decision": "rejected",
  "reason": "insufficient_documentation",
  "comments": "Missing purchase order approval",
  "returnToVendor": false
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "approvalId": 45,
    "status": "rejected",
    "rejectedBy": {
      "id": 10,
      "username": "manager.it"
    },
    "rejectedAt": "2024-01-16T09:15:00Z",
    "workflowStatus": "rejected"
  },
  "message": "Invoice rejected"
}
```

**Required Permissions:** `approvals:approve`

### POST /api/v1/approvals/{approvalId}/delegate
Delegate approval to another user.

**Request Body:**
```json
{
  "delegatedTo": 15,
  "reason": "On vacation until next week",
  "dueDate": "2024-01-20T17:00:00Z"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "approvalId": 45,
    "delegatedTo": {
      "id": 15,
      "username": "deputy.manager",
      "name": "Deputy Manager"
    },
    "delegationReason": "On vacation until next week"
  },
  "message": "Approval delegated successfully"
}
```

**Required Permissions:** `approvals:delegate`

---

## Vendor Management Endpoints

### GET /api/v1/vendors
Retrieve paginated list of vendors.

**Query Parameters:**
- `page` (integer, default: 0): Page number
- `size` (integer, default: 20): Page size
- `search` (string): Search by name, code, or tax ID
- `category` (string): Filter by vendor category
- `active` (boolean): Filter by active status

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 10,
        "vendorCode": "OS001",
        "vendorName": "Office Supplies Inc",
        "legalName": "Office Supplies Incorporated",
        "taxId": "12-3456789",
        "category": "Office Supplies",
        "paymentTerms": "Net 30",
        "currency": "USD",
        "isActive": true,
        "contact": {
          "primaryEmail": "accounts@officesupplies.com",
          "primaryPhone": "+1-555-0123"
        },
        "address": {
          "line1": "123 Business St",
          "city": "Business City",
          "state": "CA",
          "postalCode": "90210",
          "country": "USA"
        }
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 25,
      "totalPages": 2
    }
  }
}
```

**Required Permissions:** `vendors:read`

### GET /api/v1/vendors/{id}
Get detailed vendor information.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "vendorCode": "OS001",
    "vendorName": "Office Supplies Inc",
    "legalName": "Office Supplies Incorporated",
    "taxId": "12-3456789",
    "vatNumber": "VAT123456",
    "category": "Office Supplies",
    "paymentTerms": "Net 30",
    "currency": "USD",
    "creditLimit": 50000.00,
    "isActive": true,
    "isApproved": true,
    "contact": {
      "primaryContactName": "John Smith",
      "primaryEmail": "accounts@officesupplies.com",
      "primaryPhone": "+1-555-0123",
      "website": "https://officesupplies.com"
    },
    "address": {
      "line1": "123 Business St",
      "line2": "Suite 456",
      "city": "Business City",
      "state": "CA",
      "postalCode": "90210",
      "country": "USA"
    },
    "aliases": [
      {
        "id": 1,
        "aliasName": "Office Supplies",
        "aliasType": "ABBREVIATION"
      }
    ],
    "statistics": {
      "totalInvoices": 45,
      "totalAmount": 125000.00,
      "averageAmount": 2777.78,
      "lastInvoiceDate": "2024-01-15"
    },
    "audit": {
      "createdAt": "2023-06-15T10:00:00Z",
      "updatedAt": "2024-01-10T14:30:00Z",
      "createdBy": {
        "id": 1,
        "username": "admin"
      }
    }
  }
}
```

**Required Permissions:** `vendors:read`

### POST /api/v1/vendors
Create a new vendor.

**Request Body:**
```json
{
  "vendorCode": "NS001",
  "vendorName": "New Supplier LLC",
  "legalName": "New Supplier Limited Liability Company",
  "taxId": "98-7654321",
  "category": "Professional Services",
  "paymentTerms": "Net 30",
  "currency": "USD",
  "contact": {
    "primaryContactName": "Sarah Johnson",
    "primaryEmail": "billing@newsupplier.com",
    "primaryPhone": "+1-555-9876"
  },
  "address": {
    "line1": "456 Commerce Ave",
    "city": "Commerce City",
    "state": "NY",
    "postalCode": "10001",
    "country": "USA"
  }
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 25,
    "vendorCode": "NS001",
    "vendorName": "New Supplier LLC",
    "isActive": true,
    "isApproved": false,
    "createdAt": "2024-01-16T10:30:00Z"
  },
  "message": "Vendor created successfully"
}
```

**Required Permissions:** `vendors:create`

### PUT /api/v1/vendors/{id}
Update vendor information.

**Request Body:** (Same structure as POST, all fields optional)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 25,
    "vendorName": "New Supplier LLC",
    "updatedAt": "2024-01-16T11:00:00Z",
    "updatedBy": {
      "id": 5,
      "username": "jane.clerk"
    }
  },
  "message": "Vendor updated successfully"
}
```

**Required Permissions:** `vendors:update`

---

## Validation and Error Management Endpoints

### GET /api/v1/invoices/{id}/validation-errors
Get validation errors for an invoice.

**Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": 123,
      "fieldName": "totalAmount",
      "errorType": "invalid_format",
      "errorMessage": "Amount format is invalid",
      "originalValue": "1,250.OO",
      "suggestedValue": "1250.00",
      "status": "open",
      "detectedAt": "2024-01-15T09:35:00Z"
    },
    {
      "id": 124,
      "fieldName": "vendorName",
      "errorType": "vendor_not_found",
      "errorMessage": "Vendor not found in master data",
      "originalValue": "Office Suplies Inc",
      "suggestedVendors": [
        {
          "id": 10,
          "name": "Office Supplies Inc",
          "confidence": 0.95
        }
      ],
      "status": "open"
    }
  ]
}
```

**Required Permissions:** `invoices:read`

### POST /api/v1/validation-errors/{errorId}/resolve
Resolve a validation error.

**Request Body:**
```json
{
  "resolutionMethod": "manual_correction",
  "correctedValue": "1250.00",
  "notes": "Corrected OCR error in amount field"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "errorId": 123,
    "status": "resolved",
    "resolvedBy": {
      "id": 5,
      "username": "jane.clerk"
    },
    "resolvedAt": "2024-01-16T10:00:00Z"
  },
  "message": "Validation error resolved"
}
```

**Required Permissions:** `invoices:update`

---

## Reporting and Analytics Endpoints

### GET /api/v1/reports/dashboard
Get dashboard statistics and metrics.

**Query Parameters:**
- `period` (string): Time period (today, week, month, quarter, year)
- `department` (string): Filter by department

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "summary": {
      "totalInvoices": 1247,
      "totalAmount": 2456789.50,
      "averageAmount": 1970.12,
      "processingAccuracy": 0.94
    },
    "statusBreakdown": {
      "pending": 45,
      "processing": 12,
      "processed": 156,
      "approved": 980,
      "rejected": 32,
      "paid": 890
    },
    "approvalMetrics": {
      "averageApprovalTime": "2.3 days",
      "overdueApprovals": 8,
      "automatedApprovals": 156
    },
    "topVendors": [
      {
        "vendorId": 10,
        "vendorName": "Office Supplies Inc",
        "invoiceCount": 45,
        "totalAmount": 125000.00
      }
    ],
    "trends": {
      "dailyVolume": [
        {"date": "2024-01-15", "count": 23, "amount": 45000.00},
        {"date": "2024-01-14", "count": 18, "amount": 32000.00}
      ]
    }
  }
}
```

**Required Permissions:** `reports:read`

### GET /api/v1/reports/export
Export invoice data in various formats.

**Query Parameters:**
- `format` (string): Export format (csv, excel, pdf)
- `dateFrom` (date): Start date for export
- `dateTo` (date): End date for export
- `status` (string): Filter by status
- `department` (string): Filter by department

**Response (200 OK):**
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="invoices_2024-01-16.xlsx"

[Excel file binary content]
```

**Required Permissions:** `reports:export`

---

## System Administration Endpoints

### GET /api/v1/admin/users
Manage system users (admin only).

**Query Parameters:**
- `page`, `size`: Pagination
- `search`: Search by username, email, or name
- `role`: Filter by role
- `active`: Filter by active status
- `department`: Filter by department

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 5,
        "username": "jane.clerk",
        "email": "jane.clerk@company.com",
        "firstName": "Jane",
        "lastName": "Clerk",
        "department": "Finance",
        "roles": ["AP_CLERK"],
        "isActive": true,
        "lastLoginAt": "2024-01-16T08:30:00Z",
        "createdAt": "2023-12-01T10:00:00Z"
      }
    ],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 15,
      "totalPages": 1
    }
  }
}
```

**Required Permissions:** `admin:users:read`

### GET /api/v1/admin/system-health
Get system health and performance metrics.

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "status": "healthy",
    "version": "1.0.0",
    "uptime": "15 days, 6 hours",
    "database": {
      "status": "connected",
      "connectionPool": {
        "active": 8,
        "idle": 12,
        "max": 20
      }
    },
    "services": {
      "emailProcessor": {
        "status": "running",
        "lastCheck": "2024-01-16T10:25:00Z",
        "processed": 45,
        "errors": 2
      },
      "ocrEngine": {
        "status": "running",
        "queueSize": 3,
        "averageProcessingTime": "15.2 seconds"
      }
    },
    "storage": {
      "totalSpace": "500 GB",
      "usedSpace": "125 GB",
      "freeSpace": "375 GB",
      "usagePercentage": 25
    }
  }
}
```

**Required Permissions:** `admin:system:read`

---

## WebSocket Endpoints

### Real-time Processing Updates
For real-time updates on invoice processing status.

**WebSocket URL:** `ws://localhost:8080/ws/processing`

**Authentication:** Include JWT token in connection header

**Message Types:**

#### Invoice Processing Started
```json
{
  "type": "processing_started",
  "invoiceId": 123,
  "timestamp": "2024-01-16T10:30:00Z",
  "estimatedTime": 30
}
```

#### Invoice Processing Completed
```json
{
  "type": "processing_completed",
  "invoiceId": 123,
  "status": "processed",
  "confidenceScore": 0.95,
  "timestamp": "2024-01-16T10:30:45Z"
}
```

#### Approval Notification
```json
{
  "type": "approval_required",
  "approvalId": 45,
  "invoiceId": 123,
  "assignedTo": 10,
  "urgency": "normal",
  "timestamp": "2024-01-16T10:31:00Z"
}
```

---

## Error Codes and Status Codes

### HTTP Status Codes
- `200 OK`: Successful request
- `201 Created`: Resource created successfully
- `202 Accepted`: Request accepted for processing
- `400 Bad Request`: Invalid request format or parameters
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Resource conflict (duplicate)
- `413 Payload Too Large`: File size exceeds limit
- `422 Unprocessable Entity`: Validation errors
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

### Custom Error Codes
- `VALIDATION_ERROR`: Request validation failed
- `AUTHENTICATION_FAILED`: Invalid credentials
- `INSUFFICIENT_PERMISSIONS`: Access denied
- `RESOURCE_NOT_FOUND`: Requested resource doesn't exist
- `DUPLICATE_RESOURCE`: Resource already exists
- `FILE_TOO_LARGE`: File exceeds size limit
- `INVALID_FILE_FORMAT`: Unsupported file format
- `PROCESSING_FAILED`: Invoice processing failed
- `OCR_ERROR`: OCR extraction failed
- `VENDOR_NOT_FOUND`: Vendor matching failed
- `WORKFLOW_ERROR`: Approval workflow error
- `SYSTEM_ERROR`: Internal system error

---

## Rate Limiting

### Rate Limits by Endpoint Type
- **Authentication**: 5 requests per minute per IP
- **File Upload**: 10 requests per hour per user
- **General API**: 1000 requests per hour per user
- **Reporting**: 100 requests per hour per user
- **Admin**: 500 requests per hour per user

### Rate Limit Headers
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 995
X-RateLimit-Reset: 1642348800
Retry-After: 60
```

---

## API Security

### Security Headers
```
Content-Security-Policy: default-src 'self'
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
```

### Input Validation
- **SQL Injection**: Parameterized queries and ORM
- **XSS Protection**: Input sanitization and output encoding
- **File Upload**: File type validation and virus scanning
- **Size Limits**: Request body and file size limits
- **Data Validation**: Schema validation for all inputs

### Audit Logging
All API calls are logged with:
- User ID and IP address
- Endpoint and HTTP method
- Request/response payloads (sensitive data masked)
- Timestamp and response time
- Success/failure status

---

**API Specifications Status**: ✅ COMPLETE  
**Total Endpoints**: 35+ RESTful endpoints  
**Authentication**: JWT-based with RBAC  
**Real-time**: WebSocket support for processing updates  
**Security**: Comprehensive validation and audit logging  
**Documentation**: Complete with examples and error codes  

*This comprehensive API specification provides a robust foundation for the Invoice Automation System with full CRUD operations, workflow management, and real-time processing capabilities.*
