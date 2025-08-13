@echo off
echo Starting development environment...

REM Set Maven path
set PATH=C:\Users\vipaul\Downloads\apache-maven-3.9.10\bin;%PATH%
cd ..
REM Clean and compile
call mvn clean install -DskipTests
if errorlevel 1 (
    echo Build failed during compilation
    exit /b 1
)
cd invoice-api
cd target
 REM Start the application
echo Starting Invoice Automation System...
REM You can override the base URL by setting APP_BASE_URL environment variable
REM Example: set APP_BASE_URL=https://your-domain.com
call  java -jar  invoice-api-1.0.0-SNAPSHOT.jar -Pdev

echo Development environment started!
echo Application: http://localhost:8080/invoice-automation
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo H2 Console: http://localhost:8080/h2-console
echo MailHog UI: http://localhost:8025
