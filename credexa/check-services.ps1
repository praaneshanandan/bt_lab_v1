# Quick Service Status Checker for Credexa Microservices
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  CREDEXA SERVICES STATUS CHECK" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan

$services = @(
    @{Port=8086; Name="FD Account Service (Lab L21)"; Context="/api/fd-accounts"},
    @{Port=8081; Name="Login Service"; Context="/api/auth"},
    @{Port=8083; Name="Customer Service"; Context="/api/customer"},
    @{Port=8084; Name="Product Pricing Service"; Context="/api/products"},
    @{Port=8085; Name="FD Calculator Service"; Context="/api/calculator"},
    @{Port=8080; Name="API Gateway"; Context=""}
)

$upCount = 0
$workingUrls = @()

foreach ($svc in $services) {
    $url = "http://localhost:$($svc.Port)$($svc.Context)/actuator/health"
    if ($svc.Context -eq "") {
        $url = "http://localhost:$($svc.Port)/actuator/health"
    }
    
    try {
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 2 -ErrorAction Stop
        if ($response.status -eq "UP") {
            Write-Host "  ✅ $($svc.Name) (Port $($svc.Port))" -ForegroundColor Green
            if ($svc.Context -ne "") {
                $swaggerUrl = "http://localhost:$($svc.Port)$($svc.Context)/swagger-ui/index.html"
                Write-Host "     Swagger: $swaggerUrl" -ForegroundColor Cyan
                $workingUrls += $swaggerUrl
            }
            $upCount++
        }
    } catch {
        Write-Host "  ❌ $($svc.Name) (Port $($svc.Port)) - NOT RUNNING" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Summary: $upCount/6 services running" -ForegroundColor $(if ($upCount -eq 6) { "Green" } elseif ($upCount -gt 0) { "Yellow" } else { "Red" })
Write-Host "========================================" -ForegroundColor Cyan

if ($workingUrls.Count -gt 0) {
    Write-Host "`n✅ WORKING SWAGGER UI LINKS:" -ForegroundColor Green
    foreach ($url in $workingUrls) {
        Write-Host "   $url" -ForegroundColor White
    }
}

Write-Host ""
if ($upCount -lt 6) {
    Write-Host "⚠️  Some services not running. If just started, wait 2-3 minutes." -ForegroundColor Yellow
    Write-Host "   To start all: .\start-all-services.bat" -ForegroundColor White
}
Write-Host ""
