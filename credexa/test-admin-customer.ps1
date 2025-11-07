# Test with Admin Token

# Login as admin
$loginBody = @{
    usernameOrEmailOrMobile = "admin"
    password = "Admin@123"
} | ConvertTo-Json

Write-Host "Logging in as admin..." -ForegroundColor Yellow
$loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -UseBasicParsing
$loginData = $loginResponse.Content | ConvertFrom-Json

Write-Host "Login successful!" -ForegroundColor Green
Write-Host "User ID: $($loginData.data.userId)" -ForegroundColor Cyan
Write-Host "Roles: $($loginData.data.roles)" -ForegroundColor Cyan

$token = $loginData.data.token

# Try to get all customers
Write-Host "`nGetting all customers..." -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }

    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/customer/all" -Method GET -Headers $headers -UseBasicParsing
    Write-Host "SUCCESS! Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "FAILED! Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}
