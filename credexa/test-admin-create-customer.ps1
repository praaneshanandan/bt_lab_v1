# Test Creating Customer Profile as Admin

# Login as admin
$loginBody = @{
    usernameOrEmailOrMobile = "admin"
    password = "Admin@123"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -UseBasicParsing
$loginData = $loginResponse.Content | ConvertFrom-Json
$adminToken = $loginData.data.token
$adminUserId = $loginData.data.userId

Write-Host "Admin logged in. User ID: $adminUserId" -ForegroundColor Green

# Also login as customer1 to get their userId
$customerLoginBody = @{
    usernameOrEmailOrMobile = "customer1"
    password = "Pass1234"
} | ConvertTo-Json

$customerLoginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $customerLoginBody -ContentType "application/json" -UseBasicParsing
$customerLoginData = $customerLoginResponse.Content | ConvertFrom-Json
$customer1UserId = $customerLoginData.data.userId
$customer1Token = $customerLoginData.data.token

Write-Host "Customer1 logged in. User ID: $customer1UserId" -ForegroundColor Green

# Try creating customer profile for customer1 using ADMIN token
Write-Host "`n--- Test 1: Admin creates profile for customer1 ---" -ForegroundColor Yellow

$customerProfile = @{
    userId = $customer1UserId
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

try {
    $headers = @{
        "Authorization" = "Bearer $adminToken"
        "Content-Type" = "application/json"
    }

    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/customer" -Method POST -Body $customerProfile -Headers $headers -UseBasicParsing
    Write-Host "SUCCESS with ADMIN token! Status: $($response.StatusCode)" -ForegroundColor Green
    $createdProfile = $response.Content | ConvertFrom-Json
    Write-Host "Customer ID: $($createdProfile.data.id)" -ForegroundColor Cyan
} catch {
    Write-Host "FAILED with ADMIN token! Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}

# Now try with customer1's own token
Write-Host "`n--- Test 2: Customer1 creates own profile ---" -ForegroundColor Yellow

try {
    $headers = @{
        "Authorization" = "Bearer $customer1Token"
        "Content-Type" = "application/json"
    }

    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/customer" -Method POST -Body $customerProfile -Headers $headers -UseBasicParsing
    Write-Host "SUCCESS with CUSTOMER token! Status: $($response.StatusCode)" -ForegroundColor Green
    $createdProfile = $response.Content | ConvertFrom-Json
    Write-Host "Customer ID: $($createdProfile.data.id)" -ForegroundColor Cyan
} catch {
    Write-Host "FAILED with CUSTOMER token! Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response: $responseBody" -ForegroundColor Red
    }
}
