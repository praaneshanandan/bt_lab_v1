# Test with explicit header debugging

# Login first
$loginJson = '{"usernameOrEmailOrMobile":"customer1","password":"Pass1234"}'
$loginResult = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -Body $loginJson -ContentType "application/json"
$token = $loginResult.data.token

Write-Host "Token obtained: $($token.Substring(0,50))..." -ForegroundColor Green

# Test 1: GET request (we know this works)
Write-Host "`n=== Test 1: GET request (should work) ===" -ForegroundColor Cyan
try {
    $headers = @{}
    $headers.Add("Authorization", "Bearer $token")

    Write-Host "Headers for GET:" -ForegroundColor Yellow
    $headers.GetEnumerator() | ForEach-Object { Write-Host "  $($_.Key): $($_.Value.Substring(0,60))..." }

    $result = Invoke-RestMethod -Uri "http://localhost:8083/api/customer/user/2" -Method Get -Headers $headers
    Write-Host "GET request: Customer not found (404) - expected" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Write-Host "GET request: 404 Not Found - JWT is working!" -ForegroundColor Green
    } else {
        Write-Host "GET request failed: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

# Test 2: POST request without body (to test if body is the issue)
Write-Host "`n=== Test 2: POST to logout (no body) ===" -ForegroundColor Cyan
try {
    $headers = @{}
    $headers.Add("Authorization", "Bearer $token")

    $result = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/logout" -Method Post -Headers $headers
    Write-Host "POST without body: SUCCESS" -ForegroundColor Green
} catch {
    Write-Host "POST without body failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Login again since we just logged out
$loginResult = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -Body $loginJson -ContentType "application/json"
$token = $loginResult.data.token

# Test 3: POST request with body using different method
Write-Host "`n=== Test 3: POST with body (customer creation) ===" -ForegroundColor Cyan

$profileJson = '{"fullName":"John Doe","mobileNumber":"9876543210","email":"customer1@test.com","panNumber":"ABCDE1234F","aadharNumber":"123456789012","dateOfBirth":"1990-01-15","gender":"MALE","classification":"REGULAR","addressLine1":"123 Main Street","city":"Mumbai","state":"Maharashtra","pincode":"400001","country":"India","preferredLanguage":"en","preferredCurrency":"INR","emailNotifications":true,"smsNotifications":true}'

try {
    # Method 1: Separate headers
    $headers = @{}
    $headers.Add("Authorization", "Bearer $token")

    Write-Host "Headers for POST:" -ForegroundColor Yellow
    $headers.GetEnumerator() | ForEach-Object { Write-Host "  $($_.Key): $($_.Value.Substring(0,60))..." }
    Write-Host "Body length: $($profileJson.Length) characters" -ForegroundColor Yellow

    $result = Invoke-RestMethod -Uri "http://localhost:8083/api/customer" -Method Post -Body $profileJson -ContentType "application/json" -Headers $headers
    Write-Host "POST with body: SUCCESS!" -ForegroundColor Green
    Write-Host "Customer ID: $($result.data.id)" -ForegroundColor Cyan
} catch {
    Write-Host "POST with body FAILED: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nCheck the customer-service logs NOW for the Authorization header debug line" -ForegroundColor Yellow
