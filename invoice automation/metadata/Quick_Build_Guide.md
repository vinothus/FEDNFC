# Quick Build & Run Guide

## Prerequisites
1. **Java 21** ✅ (Already installed)
2. **Maven 3.9.10** ✅ (Located at: `C:\Users\vipaul\Downloads\apache-maven-3.9.10\bin`)
3. **Gmail App Password** ✅ (Already configured)

## Step-by-Step Commands

### 1. Navigate to Project Directory
```powershell
cd "C:\NFC\invoice automation\invoice-automation-system"
```

### 2. Set Maven Path (Required each new terminal session)
```powershell
$env:PATH += ";C:\Users\vipaul\Downloads\apache-maven-3.9.10\bin"
```

### 3. Verify Setup
```powershell
mvn --version
```
Should show:
- Apache Maven 3.9.10
- Java version: 21.0.6

### 4. Build Project
```powershell
# Clean build (recommended)
mvn clean install -DskipTests

# Quick build (if no major changes)
mvn install -DskipTests

# Build specific modules only
mvn clean install -DskipTests -pl invoice-email,invoice-api
```

### 5. Run Application
```powershell
# Run in foreground (to see logs)
mvn spring-boot:run -pl invoice-api -Pdev

# Run in background
mvn spring-boot:run -pl invoice-api -Pdev &
```

## Application URLs (when running)
- **Health Check**: http://localhost:8080/api/actuator/health
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **H2 Database Console**: http://localhost:8080/api/h2-console
- **Application Base**: http://localhost:8080/api

## Current Status: ✅ RUNNING
Your application is now starting with enhanced email polling diagnostics!

## What to Watch For
Look for these new log messages every 2 minutes:

### Successful Email Polling
```
🔄 Starting email polling cycle for invoice PDFs
✅ Connected to email server successfully
📬 Found X emails with PDF attachments to process
🎉 Email polling cycle completed successfully! Processed X/X emails (took XXXms)
⏱️ Next email polling cycle will start in 120 seconds (current cycle took XXXms)
```

### No Emails Found
```
🔄 Starting email polling cycle for invoice PDFs
📧 No new emails with PDF attachments found
✅ Email polling cycle completed - no emails to process (took XXXms)
```

### Error Scenarios
```
❌ Failed to connect to email server. Skipping this polling cycle.
❌ Error processing email: [Email Subject]
💥 Critical error during email polling cycle (took XXXms)
```

## Troubleshooting

### If Build Fails
1. Check Java version: `java --version`
2. Check Maven path: `mvn --version`
3. Clean and retry: `mvn clean install -DskipTests`

### If App Won't Start
1. Check port 8080 is free: `netstat -an | findstr :8080`
2. Check H2 database permissions
3. Verify Gmail credentials in `application-dev.yml`

### If Email Polling Stops
1. Watch for error messages with ❌ emoji
2. Check Gmail app password validity
3. Monitor timing - cycles should happen every 2 minutes
4. Look for "Next email polling cycle will start" messages

## Email Configuration
Current settings in `application-dev.yml`:
- **Host**: imap.gmail.com:993
- **Poll Interval**: 2 minutes (120 seconds)  
- **Days to Check**: Last 2 days
- **Email**: uspanigai@gmail.com

## Build Output Summary
✅ **All 7 modules built successfully:**
1. Invoice Automation System (parent)
2. Invoice Common  
3. Invoice Data
4. Invoice OCR
5. Invoice Email ⭐ (with enhanced diagnostics)
6. Invoice Service
7. Invoice API ⭐ (main application)

Total build time: ~15 seconds
