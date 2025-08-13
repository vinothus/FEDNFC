# Invoice Automation System

Automated invoice processing system with OCR capabilities using Spring Boot and Java-based PDF processing libraries.

## Features

- **Automated Invoice Processing**: Email monitoring and PDF extraction
- **OCR Integration**: Apache Tika, iText 7, and Tesseract4J for text extraction
- **Approval Workflow**: Role-based approval system
- **RESTful API**: Comprehensive REST API with JWT authentication
- **Multi-Environment**: H2 for development, PostgreSQL for production
- **Local Storage**: PDF files stored locally with database BLOB backup

## Technology Stack

- **Backend**: Spring Boot 3.2.x, Java 17+
- **Database**: H2 (dev), PostgreSQL (prod)
- **OCR Libraries**: Apache Tika, iText 7, Tesseract4J
- **Authentication**: JWT with Spring Security
- **Build Tool**: Maven
- **API Documentation**: OpenAPI 3 / Swagger

## Project Structure

```
invoice-automation-system/
├── invoice-api/          # REST API layer
├── invoice-service/      # Business logic
├── invoice-data/         # Data access layer
├── invoice-ocr/          # OCR and PDF processing
├── invoice-email/        # Email integration
└── invoice-common/       # Shared utilities
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker and Docker Compose

### Development Setup

1. **Clone and Build**
   ```bash
   git clone <repository-url>
   cd invoice-automation-system
   ./mvnw clean install
   ```

2. **Start Development Services**
   ```bash
   docker-compose up -d
   ```

3. **Run the Application**
   ```bash
   ./mvnw spring-boot:run -pl invoice-api -Pdev
   ```

4. **Access the Application**
   - API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html
   - H2 Console: http://localhost:8080/h2-console
   - MailHog UI: http://localhost:8025

### Build Scripts

- **Full Build**: `./mvnw clean install`
- **Run Tests**: `./mvnw test`
- **Run Application**: `./mvnw spring-boot:run -pl invoice-api`
- **Start Dev Environment**: `./scripts/dev-start.sh`

## Configuration

### Development Profile (application-dev.yml)
- H2 in-memory database
- Local file storage
- MailHog for email testing
- Debug logging enabled

### Production Profile (application-prod.yml)
- PostgreSQL database
- Environment-based configuration
- Production logging levels

## API Documentation

Once the application is running, visit http://localhost:8080/swagger-ui.html for interactive API documentation.

## Module Dependencies

```
invoice-api
├── invoice-service
│   ├── invoice-data
│   ├── invoice-ocr
│   │   ├── invoice-data
│   │   └── invoice-common
│   ├── invoice-email
│   │   ├── invoice-data
│   │   └── invoice-ocr
│   └── invoice-common
└── invoice-common
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License
