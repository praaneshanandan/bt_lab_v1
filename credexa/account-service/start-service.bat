@echo off
echo ========================================
echo Starting Account Service
echo ========================================
echo.

cd /d "%~dp0"

echo Cleaning and building project...
call mvnw.cmd clean install -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed!
    pause
    exit /b 1
)

echo.
echo Starting Account Service on port 8087...
echo Swagger UI: http://localhost:8087/swagger-ui.html
echo Context Path: /api/accounts
echo.

call mvnw.cmd spring-boot:run

pause
