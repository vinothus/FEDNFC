# Invoice Automation System - System Architecture Design

## Document Information
- **Created**: [Current Date]
- **Phase**: Phase 1 - Requirements and Design
- **Task**: 1.2.1 - Design system architecture diagram
- **Status**: ✅ COMPLETED

---

## Architecture Overview

The Invoice Automation System follows a **microservices architecture** with specialized layers for email processing, PDF text extraction, and workflow management. The system is designed to handle high-volume invoice processing with Java-based PDF libraries and robust approval workflows.

### Architecture Principles

1. **Event-Driven Processing**: Asynchronous email and PDF processing
2. **Microservices**: Independent, scalable service components
3. **Java-Native PDF Processing**: Local libraries for cost-effective text extraction
4. **Workflow-Centric**: Built-in approval and validation workflows
5. **Security by Design**: Authentication and authorization at every layer
6. **Self-Contained**: Local storage with database-centric file management
7. **High Availability**: Fault-tolerant design with redundancy

---

## System Components

### 📧 **Email Processing Layer**

#### Email Monitor Service
- **Technology**: Java Spring Boot with @Scheduled tasks
- **Functionality**: 
  - Polls IMAP/POP3 servers every 5 minutes
  - Detects new emails with PDF attachments
  - Implements retry logic for connection failures
  - Manages processed email state
- **Configuration**:
  ```yaml
  email:
    server: imap.company.com
    port: 993
    username: invoices@company.com
    poll-interval: 300000  # 5 minutes
    max-attachment-size: 52428800  # 50MB
  ```

#### Email Processor Service
- **Technology**: Java Spring Boot + Apache Commons Email
- **Features**:
  - PDF attachment extraction and validation
  - Email metadata capture (sender, subject, timestamp)
  - Duplicate email detection using message-ID
  - Automatic email archival to processed folders
  - Error handling for malformed emails

#### Email Processing Workflow
```
1. Connect to Email Server (IMAP/POP3)
2. Scan for New Emails with Attachments
3. Extract PDF Attachments (≤50MB)
4. Validate PDF Format and Integrity
5. Store PDF in Database BLOB and File System
6. Queue PDF for Processing
7. Move Email to Processed Folder
8. Log Processing Results
```

### 📄 **PDF Processing Engine**

#### PDF Type Detector
- **Technology**: Java + Apache Tika Detection
- **Logic**:
  ```java
  public enum PDFType {
      DIGITAL,    // Text-selectable PDF
      SCANNED,    // Image-based PDF requiring OCR
      HYBRID,     // Mixed content PDF
      CORRUPTED   // Invalid or unreadable PDF
  }
  
  public PDFType detectPDFType(byte[] pdfBytes) {
      // Implementation using Tika detection APIs
  }
  ```

#### Apache Tika Integration
- **Purpose**: Primary text extraction for digital PDFs
- **Features**:
  - Content detection and extraction
  - Metadata extraction (creation date, author, etc.)
  - Format validation
  - Language detection
- **Configuration**:
  ```java
  @Configuration
  public class TikaConfig {
      @Bean
      public Tika tika() {
          return new Tika();
      }
      
      @Value("${tika.max-string-length:100000}")
      private int maxStringLength;
  }
  ```

#### iText 7 Integration
- **Purpose**: Advanced PDF processing and fallback extraction
- **Features**:
  - Detailed PDF structure analysis
  - Table and form data extraction
  - Page-level text extraction
  - Font and formatting information
- **Use Cases**:
  - Complex PDF layouts that Tika cannot handle
  - Structured data extraction (tables, forms)
  - PDF validation and repair

#### Tesseract4J OCR Integration
- **Purpose**: Text extraction from scanned/image-based PDFs
- **Configuration**:
  ```java
  @Service
  public class TesseractService {
      @Value("${tesseract.data-path:/usr/share/tesseract-ocr/tessdata}")
      private String tessDataPath;
      
      @Value("${tesseract.language:eng}")
      private String language;
      
      @Value("${tesseract.dpi:300}")
      private int dpi;
  }
  ```
- **Features**:
  - Multi-language support (English, Spanish, French, etc.)
  - Confidence scoring for extracted text
  - Image preprocessing for better accuracy
  - Parallel processing for multi-page documents

#### Text Processing and Data Extraction
- **Technology**: Java + Regular Expressions + NLP libraries
- **Extraction Logic**:
  ```java
  @Component
  public class InvoiceDataExtractor {
      
      public InvoiceData extractInvoiceData(String text, PDFType type) {
          InvoiceData data = new InvoiceData();
          
          // Extract invoice number
          data.setInvoiceNumber(extractInvoiceNumber(text));
          
          // Extract dates
          data.setInvoiceDate(extractInvoiceDate(text));
          data.setDueDate(extractDueDate(text));
          
          // Extract amounts
          data.setTotalAmount(extractTotalAmount(text));
          data.setTaxAmount(extractTaxAmount(text));
          
          // Extract vendor information
          data.setVendorInfo(extractVendorInfo(text));
          
          // Extract line items
          data.setLineItems(extractLineItems(text));
          
          return data;
      }
  }
  ```

### 🏗️ **Application Services Layer**

#### Invoice Processing Service
- **Technology**: Java Spring Boot + Spring Data JPA
- **Responsibilities**:
  - Coordinate PDF processing workflow
  - Manage invoice lifecycle states
  - Implement business validation rules
  - Handle duplicate detection
  - Manage processing queues

**Core Workflow**:
```java
@Service
@Transactional
public class InvoiceProcessingService {
    
    public ProcessingResult processInvoice(String emailId, byte[] pdfBytes) {
        // 1. Store PDF in database and local file system
        String filePath = fileStorageService.storePDF(pdfBytes);
        Long pdfBlobId = databaseService.storePDFBlob(pdfBytes);
        
        // 2. Detect PDF type and extract text
        PDFType type = pdfDetector.detectType(pdfBytes);
        String text = textExtractor.extractText(pdfBytes, type);
        
        // 3. Parse invoice data
        InvoiceData data = dataExtractor.extractInvoiceData(text, type);
        
        // 4. Validate extracted data
        ValidationResult validation = validator.validate(data);
        
        // 5. Match vendor
        Vendor vendor = vendorService.matchVendor(data.getVendorInfo());
        
        // 6. Create invoice record
        Invoice invoice = invoiceRepository.save(
            createInvoice(data, vendor, filePath, pdfBlobId, validation)
        );
        
        // 7. Trigger approval workflow if valid
        if (validation.isValid()) {
            workflowService.initiateApproval(invoice);
        }
        
        return new ProcessingResult(invoice, validation);
    }
}
```

#### Vendor Management Service
- **Technology**: Java Spring Boot + Fuzzy String Matching
- **Features**:
  - Vendor master data management
  - Fuzzy matching algorithms for vendor identification
  - Vendor performance tracking
  - Duplicate vendor detection and merging

**Vendor Matching Algorithm**:
```java
@Service
public class VendorMatchingService {
    
    public VendorMatch findBestMatch(VendorInfo extractedInfo) {
        List<Vendor> candidates = vendorRepository.findByNameContaining(
            extractedInfo.getName()
        );
        
        VendorMatch bestMatch = null;
        double highestScore = 0.0;
        
        for (Vendor candidate : candidates) {
            double score = calculateMatchScore(extractedInfo, candidate);
            if (score > highestScore && score > 0.8) {
                highestScore = score;
                bestMatch = new VendorMatch(candidate, score);
            }
        }
        
        return bestMatch;
    }
    
    private double calculateMatchScore(VendorInfo info, Vendor vendor) {
        // Weighted scoring algorithm
        double nameScore = fuzzyStringMatcher.similarity(
            info.getName(), vendor.getName()
        ) * 0.6;
        
        double taxIdScore = Objects.equals(
            info.getTaxId(), vendor.getTaxId()
        ) ? 0.4 : 0.0;
        
        return nameScore + taxIdScore;
    }
}
```

#### Approval Workflow Service
- **Technology**: Java Spring Boot + State Machine
- **Features**:
  - Configurable approval rules by amount and department
  - Multi-level approval chains
  - Delegation and escalation handling
  - SLA monitoring and enforcement

**Workflow Configuration**:
```java
@Entity
public class ApprovalRule {
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private String department;
    private List<String> approverRoles;
    private Integer requiredApprovals;
    private Integer escalationDays;
}

@Service
public class WorkflowService {
    
    public void initiateApproval(Invoice invoice) {
        ApprovalRule rule = findApplicableRule(
            invoice.getTotalAmount(), 
            invoice.getDepartment()
        );
        
        ApprovalProcess process = new ApprovalProcess();
        process.setInvoice(invoice);
        process.setRule(rule);
        process.setStatus(ApprovalStatus.PENDING);
        process.setApprovers(findApprovers(rule));
        
        approvalRepository.save(process);
        notificationService.sendApprovalRequest(process);
    }
}
```

#### Notification Service
- **Technology**: Java Spring Boot + Template Engine
- **Features**:
  - Multi-channel notifications (email, in-app, SMS)
  - Template-based messaging
  - Notification preferences management
  - Delivery tracking and retry logic

#### File Storage Service
- **Technology**: Java Spring Boot + JPA for BLOB handling
- **Features**:
  - Dual storage strategy (database BLOBs + local files)
  - PDF integrity verification and validation
  - Secure file access with permission checks
  - File archival and retention management
  - Backup synchronization between storage methods

**File Storage Implementation**:
```java
@Service
public class FileStorageService {
    
    @Value("${app.file-storage.base-path:/opt/invoices/pdfs}")
    private String basePath;
    
    public FileStorageResult storePDF(byte[] pdfBytes, String invoiceNumber) {
        // 1. Generate file path with date/vendor structure
        String filePath = generateFilePath(invoiceNumber);
        
        // 2. Store in local file system
        File file = new File(basePath + "/" + filePath);
        Files.write(file.toPath(), pdfBytes);
        
        // 3. Store BLOB in database for fast access
        PDFBlob blob = new PDFBlob();
        blob.setData(pdfBytes);
        blob.setFilePath(filePath);
        blob.setFileSize(pdfBytes.length);
        blob.setChecksum(calculateChecksum(pdfBytes));
        
        pdfBlobRepository.save(blob);
        
        return new FileStorageResult(filePath, blob.getId());
    }
    
    public byte[] retrievePDF(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId);
        
        // Try database BLOB first for performance
        if (invoice.getPdfBlob() != null) {
            return invoice.getPdfBlob();
        }
        
        // Fallback to file system
        return Files.readAllBytes(Paths.get(basePath + "/" + invoice.getPdfFilePath()));
    }
}
```

### 💻 **Web Application Layer**

#### React.js Frontend Application
- **Technology**: React 18 + TypeScript + Redux Toolkit
- **Key Features**:
  - Invoice dashboard with real-time updates
  - PDF viewer with annotation capabilities
  - Data correction and validation interface
  - Approval workflow management
  - Responsive design for mobile and desktop

**Component Architecture**:
```typescript
// Main application structure
src/
├── components/
│   ├── Dashboard/
│   ├── InvoiceList/
│   ├── InvoiceDetail/
│   ├── PDFViewer/
│   ├── ApprovalWorkflow/
│   └── VendorManagement/
├── store/
│   ├── invoiceSlice.ts
│   ├── vendorSlice.ts
│   └── userSlice.ts
├── services/
│   ├── api.ts
│   └── websocket.ts
└── utils/
    ├── validation.ts
    └── formatting.ts
```

#### API Gateway
- **Technology**: Spring Cloud Gateway or Kong
- **Features**:
  - Request routing and load balancing
  - Authentication and authorization
  - Rate limiting and throttling
  - API versioning support
  - Request/response transformation

**Gateway Configuration**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: invoice-service
          uri: lb://invoice-service
          predicates:
            - Path=/api/v1/invoices/**
          filters:
            - AuthenticationFilter
            - RateLimitFilter
        - id: vendor-service
          uri: lb://vendor-service
          predicates:
            - Path=/api/v1/vendors/**
```

### 💾 **Data Storage Layer**

#### Database Strategy (Multi-Environment)
- **Development**: H2 embedded database for rapid development and testing
- **Production**: PostgreSQL for scalable, production-grade data storage
- **Purpose**: Unified schema supporting invoices, vendors, metadata, and PDF BLOBs

#### H2 Database (Development)
- **Configuration**: Embedded mode with file persistence
- **Features**:
  - Zero configuration setup for developers
  - BLOB support for PDF storage (up to 2GB per file)
  - PostgreSQL compatibility mode for seamless migration
  - Web console for development debugging
  - Automatic schema creation and sample data loading

**H2 Configuration**:
```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:file:./data/invoices;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
```

#### PostgreSQL Database (Production)
- **Purpose**: Primary data storage for invoices, vendors, metadata, and PDF BLOBs
- **Schema Design**:
  ```sql
  -- Core invoice table
  CREATE TABLE invoices (
      id SERIAL PRIMARY KEY,
      invoice_number VARCHAR(100) NOT NULL,
      vendor_id INTEGER REFERENCES vendors(id),
      total_amount DECIMAL(15,2) NOT NULL,
      currency VARCHAR(3) DEFAULT 'USD',
      invoice_date DATE NOT NULL,
      due_date DATE,
      pdf_file_path VARCHAR(500) NOT NULL,
      pdf_blob BYTEA,
      file_size BIGINT,
      processing_status VARCHAR(50) DEFAULT 'pending',
      confidence_score DECIMAL(3,2),
      extraction_method VARCHAR(50),
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      processed_at TIMESTAMP,
      approved_at TIMESTAMP,
      approved_by INTEGER REFERENCES users(id)
  );
  
  -- Vendor master data
  CREATE TABLE vendors (
      id SERIAL PRIMARY KEY,
      vendor_name VARCHAR(255) NOT NULL,
      tax_id VARCHAR(50),
      address TEXT,
      email VARCHAR(255),
      phone VARCHAR(20),
      payment_terms VARCHAR(100),
      is_active BOOLEAN DEFAULT true
  );
  
  -- Invoice line items
  CREATE TABLE invoice_line_items (
      id SERIAL PRIMARY KEY,
      invoice_id INTEGER REFERENCES invoices(id) ON DELETE CASCADE,
      line_number INTEGER,
      description TEXT,
      quantity DECIMAL(10,2),
      unit_price DECIMAL(10,2),
      total_price DECIMAL(10,2)
  );
  ```

#### Database Migration Strategy
- **Technology**: Flyway for schema versioning and migration
- **Features**:
  - Cross-database compatibility (H2 ↔ PostgreSQL)
  - Version-controlled schema changes
  - Automatic migration on application startup
  - Rollback capabilities for production deployments

**Migration Configuration**:
```yaml
# Flyway configuration
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

**Sample Migration Script** (`V1__Initial_Schema.sql`):
```sql
-- Compatible with both H2 and PostgreSQL
CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- H2 syntax
    -- id BIGSERIAL PRIMARY KEY,            -- PostgreSQL syntax
    invoice_number VARCHAR(100) NOT NULL,
    vendor_id BIGINT,
    total_amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    invoice_date DATE NOT NULL,
    due_date DATE,
    pdf_file_path VARCHAR(500) NOT NULL,
    pdf_blob BLOB,                         -- H2: BLOB, PostgreSQL: BYTEA
    file_size BIGINT,
    processing_status VARCHAR(50) DEFAULT 'pending',
    confidence_score DECIMAL(3,2),
    extraction_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by BIGINT
);

CREATE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_vendor_id ON invoices(vendor_id);
CREATE INDEX idx_processing_status ON invoices(processing_status);
```

#### Redis Cache
- **Purpose**: Session storage and performance optimization
- **Use Cases**:
  - User session management
  - API response caching
  - Processing queue management
  - Real-time dashboard data

#### Local File System Storage
- **Purpose**: PDF file storage with database BLOB backup
- **Features**:
  - Organized directory structure for PDF archival
  - Database BLOB storage for redundancy and fast access
  - File integrity verification and validation
  - Automated backup and retention policies

#### Elasticsearch
- **Purpose**: Advanced invoice search and analytics
- **Features**:
  - Full-text search across invoice content
  - Faceted search and filtering
  - Real-time indexing of new invoices
  - Analytics and reporting dashboards

### 🔗 **External Integration Layer**

#### ERP System Integration
- **Technology**: REST APIs + Message Queues
- **Data Flow**:
  ```java
  @Service
  public class ERPIntegrationService {
      
      @EventListener
      public void onInvoiceApproved(InvoiceApprovedEvent event) {
          Invoice invoice = event.getInvoice();
          ERPInvoiceData erpData = convertToERPFormat(invoice);
          
          try {
              erpClient.createInvoice(erpData);
              invoice.setERPStatus("EXPORTED");
          } catch (ERPException e) {
              invoice.setERPStatus("EXPORT_FAILED");
              retryService.scheduleRetry(invoice.getId());
          }
          
          invoiceRepository.save(invoice);
      }
  }
  ```

#### Accounting Software Integration
- **Supported Formats**: QuickBooks, SAP, Oracle Financials
- **Export Capabilities**:
  - Standard CSV/Excel formats
  - Custom XML schemas
  - Real-time API integration
  - Scheduled batch exports

---

## Non-Functional Architecture Requirements

### Performance Architecture

#### Scalability Design
- **Horizontal Scaling**: Stateless microservices with load balancing
- **Processing Capacity**: 100 invoices per hour baseline, scalable to 1000+
- **Concurrent Users**: Support for 1000 concurrent web users
- **Database Performance**: Connection pooling and read replicas

#### Performance Optimization
```java
// Async processing configuration
@EnableAsync
@Configuration
public class AsyncConfig {
    
    @Bean
    public TaskExecutor pdfProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pdf-processing-");
        return executor;
    }
}

// Caching configuration
@EnableCaching
@Configuration
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        return builder.build();
    }
}
```

### Security Architecture

#### Authentication and Authorization
- **JWT Tokens**: Stateless authentication with 8-hour expiration
- **Role-Based Access Control**: Fine-grained permissions by functionality
- **API Security**: Rate limiting, input validation, SQL injection prevention

#### Data Security
- **Encryption**: TLS 1.3 in transit, AES-256 at rest
- **PDF Security**: Secure local storage with access-controlled file retrieval
- **Audit Logging**: Comprehensive activity tracking
- **Compliance**: GDPR and SOX compliance measures

### Reliability Architecture

#### High Availability Design
- **Service Redundancy**: Multiple instances of each service
- **Database Clustering**: PostgreSQL with automatic failover
- **Load Balancing**: Health checks and automatic routing
- **Circuit Breakers**: Protection against cascading failures

#### Disaster Recovery
- **RTO**: 4 hours (Recovery Time Objective)
- **RPO**: 15 minutes (Recovery Point Objective)
- **Backup Strategy**: Automated daily backups with 30-day retention
- **Geographic Distribution**: Multi-region deployment capability

---

## Deployment Architecture

### Traditional Server Deployment
The system is designed for deployment on traditional servers (physical or virtual machines) without containerization dependencies.

#### Server Requirements
- **Operating System**: Linux (Ubuntu 20.04+ or CentOS 8+) or Windows Server 2019+
- **Java Runtime**: OpenJDK 17 or higher
- **Memory**: Minimum 4GB RAM, Recommended 8GB+ for production
- **Storage**: SSD recommended for database and file storage
- **Network**: Standard HTTP/HTTPS ports (80/443) and database port access

#### Service Deployment Strategy
```bash
# Application deployment structure
/opt/invoice-automation/
├── bin/
│   ├── invoice-processor.jar
│   ├── start.sh
│   └── stop.sh
├── config/
│   ├── application.properties
│   ├── application-prod.properties
│   └── logback.xml
├── data/
│   ├── invoices/          # PDF file storage
│   └── h2/               # H2 database files (dev)
├── logs/
└── tessdata/             # Tesseract OCR language data
```

#### Installation Script
```bash
#!/bin/bash
# install.sh - Invoice Automation System Installation

# Install Java 17
sudo apt update
sudo apt install openjdk-17-jdk -y

# Install Tesseract OCR
sudo apt install tesseract-ocr tesseract-ocr-eng tesseract-ocr-spa -y

# Create application user
sudo useradd -m -s /bin/bash invoice-app

# Create application directories
sudo mkdir -p /opt/invoice-automation/{bin,config,data/invoices,logs,tessdata}
sudo chown -R invoice-app:invoice-app /opt/invoice-automation

# Copy application files
sudo cp invoice-processor.jar /opt/invoice-automation/bin/
sudo cp config/* /opt/invoice-automation/config/
sudo cp tessdata/* /opt/invoice-automation/tessdata/

# Set permissions
sudo chmod +x /opt/invoice-automation/bin/*.sh

# Create systemd service
sudo cp invoice-automation.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable invoice-automation
```

#### Systemd Service Configuration
```ini
# /etc/systemd/system/invoice-automation.service
[Unit]
Description=Invoice Automation System
After=network.target

[Service]
Type=simple
User=invoice-app
Group=invoice-app
WorkingDirectory=/opt/invoice-automation
ExecStart=/usr/bin/java -jar /opt/invoice-automation/bin/invoice-processor.jar
ExecReload=/bin/kill -HUP $MAINPID
KillMode=process
Restart=on-failure
RestartSec=42s

Environment=JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC"
Environment=SPRING_PROFILES_ACTIVE=production
Environment=SPRING_CONFIG_LOCATION=/opt/invoice-automation/config/

StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

#### Service Management Commands
```bash
# Start service
sudo systemctl start invoice-automation

# Stop service
sudo systemctl stop invoice-automation

# Check status
sudo systemctl status invoice-automation

# View logs
sudo journalctl -u invoice-automation -f
```

#### Load Balancer Configuration (nginx)
```nginx
# /etc/nginx/sites-available/invoice-automation
upstream invoice_backend {
    server 10.0.1.10:8080;  # Primary application server
    server 10.0.1.11:8080;  # Secondary application server
    server 10.0.1.12:8080 backup;  # Backup server
}

server {
    listen 80;
    server_name invoice.company.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name invoice.company.com;
    
    ssl_certificate /etc/ssl/certs/invoice-automation.crt;
    ssl_certificate_key /etc/ssl/private/invoice-automation.key;
    
    client_max_body_size 50M;  # Allow large PDF uploads
    
    location / {
        proxy_pass http://invoice_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # PDF download optimization
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # Health check endpoint
    location /health {
        proxy_pass http://invoice_backend/actuator/health;
        access_log off;
    }
}
```

### Environment Configuration
- **Development**: 
  - Single server with H2 embedded database
  - Local file-based PDF storage in development directory
  - H2 web console enabled for database inspection
  - IDE-based deployment or standalone JAR execution
- **Staging**: 
  - Single server with PostgreSQL database
  - Production-like configuration with reduced resources
  - Systemd service deployment for testing
  - Database migration validation from H2 schemas
- **Production**: 
  - Multiple servers with load balancer (nginx/Apache)
  - PostgreSQL database on dedicated server
  - Systemd service with automatic restart
  - Full monitoring and backup strategies

---

## Technology Stack Summary

### Backend Services
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **PDF Processing**: Apache Tika, iText 7, Tesseract4J
- **Database**: PostgreSQL 14+ with Redis caching
- **Search**: Elasticsearch 8.x
- **Message Queue**: Apache Kafka or RabbitMQ

### Frontend Application
- **Framework**: React 18 + TypeScript
- **State Management**: Redux Toolkit
- **UI Components**: Material-UI or Ant Design
- **PDF Viewing**: PDF.js or react-pdf

### Infrastructure
- **Deployment**: Traditional server deployment (physical or virtual machines)
- **Operating System**: Linux (Ubuntu/CentOS) or Windows Server
- **Storage**: Local file system for PDFs, standard server storage for databases
- **Load Balancing**: nginx or Apache HTTP Server
- **Monitoring**: Prometheus + Grafana or traditional monitoring tools
- **CI/CD**: GitHub Actions + Jenkins or similar build tools

---

## Implementation Phases

### Phase 1: Core Processing (Weeks 1-6)
- Email monitoring and PDF extraction
- Java PDF processing libraries integration
- Basic invoice data extraction
- Database schema implementation

### Phase 2: Web Interface (Weeks 7-9)
- React frontend development
- Invoice management interface
- User authentication and authorization
- Basic approval workflows

### Phase 3: Advanced Features (Weeks 10-12)
- Advanced search and filtering
- Vendor management and matching
- Notification system
- Error handling and recovery

### Phase 4: Integration & Deployment (Weeks 13-16)
- External system integration
- Performance optimization
- Security hardening
- Production deployment

---

**Architecture Status**: ✅ COMPLETE  
**Next Task**: 1.2.2 - Database Schema Design  
**Dependencies Resolved**: Requirements review, user stories  
**Technical Approach**: Java-based PDF processing with microservices architecture

*This architecture provides a robust, scalable foundation for invoice automation with comprehensive PDF processing capabilities and workflow management.*
