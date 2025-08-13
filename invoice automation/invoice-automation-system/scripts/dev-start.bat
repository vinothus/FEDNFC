@echo off
echo Starting development environment...

REM Set Maven path
set PATH=C:\Users\vipaul\Downloads\apache-maven-3.9.10\bin;%PATH%
cd ..
REM Clean and compile
call mvn clean compile -DskipTests
if errorlevel 1 (
    echo Build failed during compilation
    exit /b 1
)
 REM Start the application
echo Starting Invoice Automation System...
call mvn spring-boot:run -pl invoice-api -Pdev

echo Development environment started!
echo Application: http://localhost:8080
echo Swagger UI: http://localhost:8080/swagger-ui.html
echo H2 Console: http://localhost:8080/h2-console
echo MailHog UI: http://localhost:8025
