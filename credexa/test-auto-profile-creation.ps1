# Test automatic customer profile creation during registration
# This should create both user account AND customer profile

$registerUrl = "http://localhost:8080/api/login/register"

$registerData = @{
    username = "testuser10"
    password = "Test@1234"
    email = "testuser10@example.com"
    mobileNumber = "9876543210"
    fullName = "Test User Ten"
    panNumber = "ABCDE1234F"
    aadharNumber = "123456789012"
    dateOfBirth = "1990-05-15"
    gender = "MALE"
    classification = "REGULAR"
    addressLine1 = "123 Test Street"
    addressLine2 = "Apt 4B"
    city = "Bangalore"
    state = "Karnataka"
    pincode = "560001"
    country = "India"
    preferredLanguage = "en"
    preferredCurrency = "INR"
    emailNotifications = $true
    smsNotifications = $true
} | ConvertTo-Json

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 1: REGISTERING NEW USER" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "POST $registerUrl" -ForegroundColor Yellow
Write-Host "Data:" -ForegroundColor Yellow
Write-Host $registerData
Write-Host ""

try {
    $registerResponse = Invoke-RestMethod -Uri $registerUrl -Method Post -Body $registerData -ContentType "application/json"
    Write-Host "✓ Registration Response:" -ForegroundColor Green
    $registerResponse | ConvertTo-Json -Depth 10
    Write-Host ""
} catch {
    Write-Host "✗ Registration Failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    Write-Host "Response:" $_.ErrorDetails.Message
    exit 1
}

# Wait a moment for profile creation to complete
Start-Sleep -Seconds 2

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 2: LOGIN WITH NEW USER" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$loginUrl = "http://localhost:8080/api/login/login"
$loginData = @{
    usernameOrEmailOrMobile = "testuser10"
    password = "Test@1234"
} | ConvertTo-Json

Write-Host "POST $loginUrl" -ForegroundColor Yellow
Write-Host ""

try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginData -ContentType "application/json"
    Write-Host "✓ Login Response:" -ForegroundColor Green
    $loginResponse | ConvertTo-Json -Depth 10
    Write-Host ""
    
    $token = $loginResponse.data.token
    Write-Host "JWT Token: $token" -ForegroundColor Cyan
    Write-Host ""
} catch {
    Write-Host "✗ Login Failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    Write-Host "Response:" $_.ErrorDetails.Message
    exit 1
}

# Wait a moment
Start-Sleep -Seconds 1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "STEP 3: GET CUSTOMER PROFILE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$profileUrl = "http://localhost:8080/api/customer/profile"
$headers = @{
    "Authorization" = "Bearer $token"
}

Write-Host "GET $profileUrl" -ForegroundColor Yellow
Write-Host "Authorization: Bearer $token" -ForegroundColor Yellow
Write-Host ""

try {
    $profileResponse = Invoke-RestMethod -Uri $profileUrl -Method Get -Headers $headers
    Write-Host "✓ Profile Retrieved Successfully!" -ForegroundColor Green
    $profileResponse | ConvertTo-Json -Depth 10
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "SUCCESS! AUTOMATIC PROFILE CREATION WORKS!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} catch {
    Write-Host "✗ Profile Fetch Failed:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.ErrorDetails) {
        Write-Host "Response:" $_.ErrorDetails.Message
    }
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "FAILED! PROFILE WAS NOT AUTO-CREATED" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}
