# Comprehensive Service Verification Script
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  CREDEXA MICROSERVICES STATUS CHECK" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan

$services = @(
    @{Port=8086; Name="FD Account Service (Lab L21)"; Context="/api/fd-accounts"; Priority="HIGH"},
    @{Port=8081; Name="Login Service"; Context="/api/auth"; Priority="HIGH"},
    @{Port=8083; Name="Customer Service"; Context="/api/customer"; Priority="MEDIUM"},
    @{Port=8084; Name="Product Pricing Service"; Context="/api/products"; Priority="MEDIUM"},
    @{Port=8085; Name="FD Calculator Service"; Context="/api/calculator"; Priority="MEDIUM"},
    @{Port=8080; Name="API Gateway"; Context=""; Priority="LOW"}
)

$workingUrls = @()
$totalServices = $services.Count
$runningServices = 0

foreach ($svc in $services) {
    $healthUrl = "http://localhost:$($svc.Port)$($svc.Context)/actuator/health"
    if ($svc.Context -eq "") {
        $healthUrl = "http://localhost:$($svc.Port)/actuator/health"
    }
    
    $swaggerUrl = "http://localhost:$($svc.Port)$($svc.Context)/swagger-ui/index.html"
    
    try {
        $response = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2 -ErrorAction Stop
        if ($response.status -eq "UP") {
            $runningServices++
            Write-Host "✅ " -ForegroundColor Green -NoNewline
            Write-Host "$($svc.Name) " -ForegroundColor White -NoNewline
            Write-Host "(Port $($svc.Port))" -ForegroundColor Gray
            Write-Host "   Swagger: " -NoNewline
            Write-Host $swaggerUrl -ForegroundColor Cyan
            $workingUrls += $swaggerUrl
            
            if ($svc.Name -eq "FD Account Service (Lab L21)") {
                Write-Host "   📊 Lab L21 Reporting Endpoints:" -ForegroundColor Yellow
                Write-Host "      - GET /report/fd-summary" -ForegroundColor Gray
                Write-Host "      - GET /report/customer-portfolio" -ForegroundColor Gray
                Write-Host "      - GET /report/maturity-summary" -ForegroundColor Gray
            }
            Write-Host ""
        }
    } catch {
        Write-Host "❌ " -ForegroundColor Red -NoNewline
        Write-Host "$($svc.Name) " -ForegroundColor White -NoNewline
        Write-Host "(Port $($svc.Port)) " -ForegroundColor Gray -NoNewline
        Write-Host "- NOT RUNNING" -ForegroundColor Red
        
        if ($svc.Priority -eq "HIGH") {
            Write-Host "   ⚠️  This is a HIGH priority service!" -ForegroundColor Yellow
        }
        Write-Host ""
    }
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SUMMARY" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Running: $runningServices/$totalServices services" -ForegroundColor $(if ($runningServices -eq $totalServices) { "Green" } elseif ($runningServices -gt 0) { "Yellow" } else { "Red" })
Write-Host ""

if ($workingUrls.Count -gt 0) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  WORKING SWAGGER UI LINKS" -ForegroundColor White
    Write-Host "========================================" -ForegroundColor Green
    foreach ($url in $workingUrls) {
        Write-Host $url -ForegroundColor White
    }
    Write-Host ""
}

if ($runningServices -lt $totalServices) {
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "  TO START MISSING SERVICES" -ForegroundColor White
    Write-Host "========================================" -ForegroundColor Yellow
    Write-Host "Run: .\start-all-services.bat" -ForegroundColor White
    Write-Host "Or start individually from service directories" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
