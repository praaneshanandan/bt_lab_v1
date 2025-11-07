# Debug Customer Creation Issue

# First login as customer1
$loginBody = @{
    usernameOrEmailOrMobile = "customer1"
    password = "Pass1234"
} | ConvertTo-Json

Write-Host "Logging in as customer1..." -ForegroundColor Yellow
$loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -UseBasicParsing
$loginData = $loginResponse.Content | ConvertFrom-Json

Write-Host "Login successful!" -ForegroundColor Green
Write-Host "User ID: $($loginData.data.userId)" -ForegroundColor Cyan
Write-Host "Roles: $($loginData.data.roles)" -ForegroundColor Cyan
Write-Host "Token: $($loginData.data.token)" -ForegroundColor Cyan

$token = $loginData.data.token
$userId = $loginData.data.userId

# Now try to create customer profile
$customerProfile = @{
    userId = $userId
    fullName = "John Doe"
    dateOfBirth = "1990-01-15"
    gender = "MALE"
    addressLine1 = "123 Main St"
    city = "New York"
    state = "NY"
    postalCode = "10001"
    country = "USA"
    occupation = "Software Engineer"
    annualIncome = 75000.0
    identificationType = "AADHAAR"
    identificationNumber = "123456789012"
} | ConvertTo-Json

Write-Host "`nCreating customer profile..." -ForegroundColor Yellow
Write-Host "Request Body:" -ForegroundColor Cyan
Write-Host $customerProfile -ForegroundColor White

try {
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }

    Write-Host "`nAuthorization Header: Bearer $token" -ForegroundColor Cyan

    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/customer" -Method POST -Body $customerProfile -Headers $headers -UseBasicParsing
    Write-Host "`nSUCCESS! Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    Write-Host $response.Content -ForegroundColor White
} catch {
    Write-Host "`nFAILED! Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red

    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody" -ForegroundColor Red
    }
}
