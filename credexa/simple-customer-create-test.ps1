# Simple Direct Test for Customer Creation

# Step 1: Login
Write-Host "Step 1: Logging in..." -ForegroundColor Cyan
$loginJson = '{"usernameOrEmailOrMobile":"customer1","password":"Pass1234"}'
$loginResult = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method Post -Body $loginJson -ContentType "application/json"

if ($loginResult.success) {
    Write-Host "Login successful!" -ForegroundColor Green
    $token = $loginResult.data.token
    Write-Host "Token (first 50 chars): $($token.Substring(0,50))..." -ForegroundColor Yellow
} else {
    Write-Host "Login failed!" -ForegroundColor Red
    exit
}

# Step 2: Create customer profile
Write-Host "`nStep 2: Creating customer profile..." -ForegroundColor Cyan

$profileJson = @"
{
    "fullName": "John Doe",
    "mobileNumber": "9876543210",
    "email": "customer1@test.com",
    "panNumber": "ABCDE1234F",
    "aadharNumber": "123456789012",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "classification": "REGULAR",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 5B",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001",
    "country": "India",
    "accountNumber": "ACC1234567890",
    "ifscCode": "HDFC0001234",
    "preferredLanguage": "en",
    "preferredCurrency": "INR",
    "emailNotifications": true,
    "smsNotifications": true
}
"@

try {
    $headers = @{
        "Authorization" = "Bearer $token"
    }

    Write-Host "Sending request with Authorization header..." -ForegroundColor Yellow

    $result = Invoke-RestMethod -Uri "http://localhost:8083/api/customer" -Method Post -Body $profileJson -ContentType "application/json" -Headers $headers

    Write-Host "`nSUCCESS!" -ForegroundColor Green
    Write-Host "Customer ID: $($result.data.id)" -ForegroundColor Cyan
    Write-Host "Full Response:" -ForegroundColor Yellow
    $result | ConvertTo-Json -Depth 5

} catch {
    Write-Host "`nFAILED!" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red

    # Try to get response body
    if ($_.ErrorDetails.Message) {
        Write-Host "Response Body: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}
