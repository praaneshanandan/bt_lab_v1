# Check which services are running
Write-Host "Checking service status..." -ForegroundColor Cyan
Write-Host ""

$ports = @{
    "8083" = "Customer Service"
    "8084" = "Product Pricing Service"
    "8085" = "FD Calculator Service"
    "8086" = "FD Account Service"
    "8087" = "Login Service"
    "8080" = "API Gateway"
}

$listening = netstat -ano | Select-String "LISTENING"

foreach ($port in $ports.Keys) {
    $found = $listening | Select-String ":$port "
    if ($found) {
        Write-Host "[RUNNING]  $($ports[$port]) on port $port" -ForegroundColor Green
    } else {
        Write-Host "[NOT RUNNING] $($ports[$port]) on port $port" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
