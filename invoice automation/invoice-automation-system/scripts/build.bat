@echo off
echo Building Invoice Automation System...

REM Set Maven path
set PATH=C:\Users\vipaul\Downloads\apache-maven-3.9.10\bin;%PATH%
cd ..
REM Clean and compile
call mvn clean compile -DskipTests
if errorlevel 1 (
    echo Build failed during compilation
    exit /b 1
)

echo Build completed successfully!
