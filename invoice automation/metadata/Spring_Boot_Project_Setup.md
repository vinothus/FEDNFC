# Invoice Automation System - Spring Boot Project Setup

## Document Information
- **Task**: 2.1.1A - Create Spring Boot project structure and core setup
- **Status**: ✅ COMPLETED
- **Created**: [Current Date]

---

## Project Overview

### Spring Boot Application Structure
**Project Name**: `invoice-automation-system`  
**Base Package**: `com.company.invoice`  
**Spring Boot Version**: 3.2.x  
**Java Version**: 17+  
**Build Tool**: Maven  

### Multi-Module Maven Structure

```
invoice-automation-system/
├── pom.xml                              (Parent POM)
├── README.md
├── .gitignore
├── docker-compose.yml                   (Development services)
├── invoice-api/                         (REST Controllers & Web Layer)
│   ├── pom.xml
│   ├── src/main/java/com/company/invoice/api/
│   │   ├── InvoiceApiApplication.java
│   │   ├── config/
│   │   │   ├── WebConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── SwaggerConfig.java
│   │   ├── controller/
│   │   │   ├── InvoiceController.java
│   │   │   ├── AuthController.java
│   │   │   ├── UserController.java
│   │   │   ├── VendorController.java
│   │   │   └── ReportController.java
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── ApiException.java
│   │   └── security/
│   │       ├── JwtAuthenticationFilter.java
│   │       ├── JwtTokenProvider.java
│   │       └── UserPrincipal.java
│   └── src/main/resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── static/ & templates/
├── invoice-service/                     (Business Logic Layer)
│   ├── pom.xml
│   └── src/main/java/com/company/invoice/service/
│       ├── InvoiceService.java
│       ├── AuthenticationService.java
│       ├── UserService.java
│       ├── VendorService.java
│       ├── ApprovalWorkflowService.java
│       ├── NotificationService.java
│       └── impl/
├── invoice-ocr/                         (OCR & PDF Processing)
│   ├── pom.xml
│   └── src/main/java/com/company/invoice/ocr/
│       ├── service/
│       │   ├── PDFProcessingService.java
│       │   ├── TikaTextExtractor.java
│       │   ├── ITextPDFProcessor.java
│       │   ├── TesseractOCRService.java
│       │   └── HybridExtractionService.java
│       ├── template/
│       │   ├── InvoiceExtractionTemplate.java
│       │   ├── QuickBooksTemplate.java
│       │   └── SAPTemplate.java
│       ├── confidence/
│       │   └── ExtractionConfidenceCalculator.java
│       └── validation/
│           └── ExtractionValidator.java
├── invoice-data/                        (Data Access Layer)
│   ├── pom.xml
│   └── src/main/java/com/company/invoice/data/
│       ├── entity/
│       │   ├── Invoice.java
│       │   ├── User.java
│       │   ├── Vendor.java
│       │   ├── LineItem.java
│       │   ├── ApprovalWorkflow.java
│       │   └── AuditLog.java
│       ├── repository/
│       │   ├── InvoiceRepository.java
│       │   ├── UserRepository.java
│       │   ├── VendorRepository.java
│       │   └── ApprovalWorkflowRepository.java
│       ├── specification/
│       │   └── InvoiceSpecification.java
│       └── dto/
│           └── projections/
├── invoice-email/                       (Email Integration)
│   ├── pom.xml
│   └── src/main/java/com/company/invoice/email/
│       ├── service/
│       │   ├── EmailMonitoringService.java
│       │   ├── AttachmentExtractorService.java
│       │   └── EmailProcessingService.java
│       ├── config/
│       │   └── EmailConfig.java
│       └── scheduler/
│           └── EmailPollingScheduler.java
├── invoice-common/                      (Shared Utilities)
│   ├── pom.xml
│   └── src/main/java/com/company/invoice/common/
│       ├── constants/
│       ├── enums/
│       ├── exception/
│       ├── util/
│       └── annotation/
└── invoice-test/                        (Integration Tests)
    ├── pom.xml
    └── src/test/java/com/company/invoice/
        ├── integration/
        ├── testcontainers/
        └── fixtures/
```

---

## Parent POM Configuration

### Root `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.company</groupId>
    <artifactId>invoice-automation-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Invoice Automation System</name>
    <description>Automated invoice processing system with OCR capabilities</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <modules>
        <module>invoice-common</module>
        <module>invoice-data</module>
        <module>invoice-ocr</module>
        <module>invoice-email</module>
        <module>invoice-service</module>
        <module>invoice-api</module>
        <module>invoice-test</module>
    </modules>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Dependency Versions -->
        <tika.version>2.9.1</tika.version>
        <itext.version>8.0.2</itext.version>
        <tesseract4j.version>5.8.0</tesseract4j.version>
        <flyway.version>9.22.3</flyway.version>
        <springdoc.version>2.2.0</springdoc.version>
        <jwt.version>4.4.0</jwt.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <testcontainers.version>1.19.0</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Internal Modules -->
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>invoice-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>invoice-data</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>invoice-ocr</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>invoice-email</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.company</groupId>
                <artifactId>invoice-service</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- OCR & PDF Processing -->
            <dependency>
                <groupId>org.apache.tika</groupId>
                <artifactId>tika-core</artifactId>
                <version>${tika.version}</version>
            </dependency>
            <dependency>
                <groupId>org.apache.tika</groupId>
                <artifactId>tika-parsers-standard-package</artifactId>
                <version>${tika.version}</version>
            </dependency>
            <dependency>
                <groupId>com.itextpdf</groupId>
                <artifactId>itext-core</artifactId>
                <version>${itext.version}</version>
                <type>pom</type>
            </dependency>
            <dependency>
                <groupId>net.sourceforge.tess4j</groupId>
                <artifactId>tess4j</artifactId>
                <version>${tesseract4j.version}</version>
            </dependency>

            <!-- Database Migration -->
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-core</artifactId>
                <version>${flyway.version}</version>
            </dependency>
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-database-postgresql</artifactId>
                <version>${flyway.version}</version>
            </dependency>

            <!-- JWT Authentication -->
            <dependency>
                <groupId>com.auth0</groupId>
                <artifactId>java-jwt</artifactId>
                <version>${jwt.version}</version>
            </dependency>

            <!-- API Documentation -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>

            <!-- Object Mapping -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>

            <!-- Test Containers -->
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>17</source>
                        <target>17</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.flywaydb</groupId>
                    <artifactId>flyway-maven-plugin</artifactId>
                    <version>${flyway.version}</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <profiles>
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <spring.profiles.active>dev</spring.profiles.active>
            </properties>
        </profile>
        <profile>
            <id>prod</id>
            <properties>
                <spring.profiles.active>prod</spring.profiles.active>
            </properties>
        </profile>
    </profiles>
</project>
```

---

## API Module Configuration

### `invoice-api/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.company</groupId>
        <artifactId>invoice-automation-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>invoice-api</artifactId>
    <packaging>jar</packaging>

    <name>Invoice API</name>
    <description>REST API layer for Invoice Automation System</description>

    <dependencies>
        <!-- Internal Dependencies -->
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>invoice-service</artifactId>
        </dependency>
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>invoice-common</artifactId>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- JWT Authentication -->
        <dependency>
            <groupId>com.auth0</groupId>
            <artifactId>java-jwt</artifactId>
        </dependency>

        <!-- API Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <!-- Object Mapping -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct-processor</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Main Application Class

### `InvoiceApiApplication.java`

```java
package com.company.invoice.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.company.invoice")
@EntityScan("com.company.invoice.data.entity")
@EnableJpaRepositories("com.company.invoice.data.repository")
@EnableAsync
@EnableScheduling
public class InvoiceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceApiApplication.class, args);
    }
}
```

---

## Core Configuration Classes

### 1. **Web Configuration**

```java
package com.company.invoice.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
```

### 2. **Security Configuration**

```java
package com.company.invoice.api.config;

import com.company.invoice.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/approver/**").hasAnyRole("APPROVER", "ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

### 3. **Swagger API Documentation Configuration**

```java
package com.company.invoice.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI invoiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Invoice Automation System API")
                .description("REST API for automated invoice processing with OCR capabilities")
                .version("v1.0.0")
                .license(new License().name("MIT").url("http://springdoc.org")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearerAuth", 
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### 4. **Async Configuration**

```java
package com.company.invoice.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "ocrExecutor")
    public Executor ocrExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("OCR-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(3);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Email-");
        executor.initialize();
        return executor;
    }
}
```

---

## Exception Handling

### Global Exception Handler

```java
package com.company.invoice.api.exception;

import com.company.invoice.common.exception.BusinessException;
import com.company.invoice.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException e) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Resource Not Found")
            .message(e.getMessage())
            .path(getCurrentPath())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Business Logic Error")
            .message(e.getMessage())
            .path(getCurrentPath())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> validationErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Validation Failed")
            .message("Input validation failed")
            .validationErrors(validationErrors)
            .path(getCurrentPath())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Access Denied")
            .message("You don't have permission to access this resource")
            .path(getCurrentPath())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        
        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .path(getCurrentPath())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getCurrentPath() {
        // Implementation to get current request path
        return "/api"; // Placeholder
    }
}
```

---

## Development Tools Configuration

### 1. **Docker Compose for Development Services**

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres-dev:
    image: postgres:15-alpine
    container_name: invoice-postgres-dev
    environment:
      POSTGRES_DB: invoice_automation
      POSTGRES_USER: invoice_user
      POSTGRES_PASSWORD: invoice_pass
    ports:
      - "5433:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql

  redis-dev:
    image: redis:7-alpine
    container_name: invoice-redis-dev
    ports:
      - "6379:6379"

  mailhog:
    image: mailhog/mailhog:latest
    container_name: invoice-mailhog
    ports:
      - "8025:8025"  # Web UI
      - "1025:1025"  # SMTP

volumes:
  postgres_data:
```

### 2. **Git Configuration**

```gitignore
# .gitignore
HELP.md
target/
!.mvn/wrapper/maven-wrapper.jar
!**/src/main/**/target/
!**/src/test/**/target/

### STS ###
.apt_generated
.classpath
.factorypath
.project
.settings
.springBeans
.sts4-cache

### IntelliJ IDEA ###
.idea
*.iws
*.iml
*.ipr

### NetBeans ###
/nbproject/private/
/nbbuild/
/dist/
/nbdist/
/.nb-gradle/
build/
!**/src/main/**/build/
!**/src/test/**/build/

### VS Code ###
.vscode/

### Application Specific ###
/uploads/
/pdf-storage/
/tesseract-data/
logs/
*.log

### Database ###
*.db
*.sqlite

### Environment ###
.env
.env.local
.env.prod
```

---

## Build and Development Scripts

### 1. **Maven Wrapper Scripts**

```bash
#!/bin/bash
# scripts/build.sh

echo "Building Invoice Automation System..."

# Clean and compile
./mvnw clean compile

# Run tests
./mvnw test

# Package application
./mvnw package -DskipTests

echo "Build completed successfully!"
```

### 2. **Development Startup Script**

```bash
#!/bin/bash
# scripts/dev-start.sh

echo "Starting development environment..."

# Start Docker services
docker-compose up -d

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Run database migrations
./mvnw flyway:migrate -Pdev

# Start the application
./mvnw spring-boot:run -Pdev

echo "Development environment started!"
echo "Application: http://localhost:8080"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "H2 Console: http://localhost:8080/h2-console"
echo "MailHog UI: http://localhost:8025"
```

---

## Development Environment Setup Checklist

### Prerequisites
- [ ] Java 17+ JDK installed
- [ ] Maven 3.8+ installed
- [ ] Docker and Docker Compose installed
- [ ] IDE (IntelliJ IDEA recommended) with plugins:
  - [ ] Lombok plugin
  - [ ] MapStruct plugin
  - [ ] Spring Tools

### Project Setup Steps
- [ ] Clone repository
- [ ] Run `./mvnw clean install` to build all modules
- [ ] Start development services: `docker-compose up -d`
- [ ] Run database migrations: `./mvnw flyway:migrate -Pdev`
- [ ] Start application: `./mvnw spring-boot:run -Pdev`
- [ ] Verify endpoints: http://localhost:8080/actuator/health

### IDE Configuration
- [ ] Import as Maven project
- [ ] Enable annotation processing
- [ ] Configure code style (Google Java Format recommended)
- [ ] Set up run configurations for different profiles

---

**Spring Boot Project Setup Status**: ✅ COMPLETE  
**Project Structure**: Multi-module Maven with 7 modules  
**Dependencies**: All core libraries configured  
**Configuration**: Environment-specific profiles ready  
**Development Tools**: Docker Compose, scripts, and IDE setup  

*This comprehensive Spring Boot setup provides a solid foundation for the Invoice Automation System development with proper separation of concerns, security, monitoring, and development productivity features.*
