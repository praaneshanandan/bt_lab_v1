# Test Creating Customer with Correct DTO Fields

# Login as customer1
$loginBody = @{
    usernameOrEmailOrMobile = "customer1"
    password = "Pass1234"
} | ConvertTo-Json

$loginResponse = Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -UseBasicParsing
$loginData = $loginResponse.Content | ConvertFrom-Json
$token = $loginData.data.token

Write-Host "Customer1 logged in. Token obtained." -ForegroundColor Green

# Create customer profile with CORRECT fields based on CreateCustomerRequest.java
$customerProfile = @{
    fullName = "John Doe"
    mobileNumber = "9876543210"  # 10 digits
    email = "customer1@test.com"
    panNumber = "ABCDE1234F"  # Optional but correct format
    aadharNumber = "123456789012"  # 12 digits
    dateOfBirth = "1990-01-15"
    gender = "MALE"
    classification = "REGULAR"  # REQUIRED!
    addressLine1 = "123 Main Street"
    addressLine2 = "Apt 5B"
    city = "Mumbai"
    state = "Maharashtra"
    pincode = "400001"  # 6 digits, not postalCode
    country = "India"
    accountNumber = "1234567890"
    ifscCode = "HDFC0001234"
    preferredLanguage = "en"
    preferredCurrency = "INR"
    emailNotifications = $true
    smsNotifications = $true
} | ConvertTo-Json

Write-Host "`nCreating customer profile..." -ForegroundColor Yellow
Write-Host "Request Body:" -ForegroundColor Cyan
Write-Host $customerProfile

try {
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }

    $response = Invoke-WebRequest -Uri "http://localhost:8083/api/customer" -Method POST -Body $customerProfile -Headers $headers -UseBasicParsing
    Write-Host "`nSUCCESS! Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Cyan
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
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
