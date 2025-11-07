# Check if customer profile already exists

# Login
$loginJson = '{"usernameOrEmailOrMobile":"customer1","password":"Pass1234"}'
$loginResult = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -Body $loginJson -ContentType "application/json"

$token = $loginResult.data.token
$userId = $loginResult.data.userId

Write-Host "Logged in as customer1, User ID: $userId" -ForegroundColor Green

# Try to get customer profile by user ID
try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }

    Write-Host "`nTrying to get customer profile for user ID $userId..." -ForegroundColor Yellow
    $result = Invoke-RestMethod -Uri "http://localhost:8083/api/customer/user/$userId" -Method Get -Headers $headers

    Write-Host "Profile EXISTS!" -ForegroundColor Cyan
    $result | ConvertTo-Json -Depth 5

} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "Status Code: $statusCode" -ForegroundColor Yellow

    if ($statusCode -eq 404) {
        Write-Host "Profile does NOT exist - this is expected for new users" -ForegroundColor Green
    } elseif ($statusCode -eq 403) {
        Write-Host "403 Forbidden - Authorization issue!" -ForegroundColor Red
    } else {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}
