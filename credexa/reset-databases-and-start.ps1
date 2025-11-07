# Script to drop all existing databases and restart the application
# This will allow the application to create fresh databases

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Fixed Deposits Application - Database Reset & Start" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# MySQL connection parameters (modify if needed)
$MYSQL_EXE = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
$MYSQL_HOST = "localhost"
$MYSQL_PORT = "3306"
$MYSQL_USER = "root"

# Prompt for MySQL password
$MYSQL_PASS = Read-Host "Enter MySQL root password"

# List of databases to drop
$databases = @(
    "login_db",
    "customer_db",
    "product_db",
    "calculator_db",
    "fd_account_db"
)

Write-Host ""
Write-Host "Step 1: Dropping existing databases..." -ForegroundColor Yellow
Write-Host ""

foreach ($db in $databases) {
    Write-Host "  Dropping database: $db" -ForegroundColor White
    
    $dropCommand = "DROP DATABASE IF EXISTS $db;"
    
    # Execute MySQL command
    try {
        $result = & $MYSQL_EXE -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER --password=$MYSQL_PASS -e $dropCommand 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "    Successfully dropped $db" -ForegroundColor Green
        } else {
            Write-Host "    Error dropping $db" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "    Error dropping $db : $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Step 2: Databases have been dropped" -ForegroundColor Green
Write-Host ""

Write-Host "Step 3: Starting all microservices..." -ForegroundColor Yellow
Write-Host "  The application will create fresh databases automatically" -ForegroundColor Cyan
Write-Host ""

# Check if start-all-services.bat exists
if (Test-Path ".\start-all-services.bat") {
    Write-Host "  Executing start-all-services.bat..." -ForegroundColor White
    Write-Host ""
    & .\start-all-services.bat
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Services are starting up..." -ForegroundColor Green
    Write-Host "The application will create fresh databases automatically" -ForegroundColor Green
    Write-Host "Please wait 30-60 seconds for all services to initialize" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host "  Warning: start-all-services.bat not found!" -ForegroundColor Red
    Write-Host "  Please start services manually or run from correct directory" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Done! Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
