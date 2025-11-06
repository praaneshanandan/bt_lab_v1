# ============================================
# Lab L8: Complete Integration Test Script
# ============================================
# This script tests the complete integration flow across all microservices
# Prerequisites: All services must be running (Login, Customer, Product, Calculator)

$ErrorActionPreference = "Continue"
$baseUrls = @{
    login = "http://localhost:8081/api/auth"
    customer = "http://localhost:8083/api/customer"
    product = "http://localhost:8084/api/products"
    calculator = "http://localhost:8085/api/calculator"
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Lab L8: Integration Testing Suite" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test 1: Registration and Login Flow
Write-Host "Test 1: User Registration and Login" -ForegroundColor Yellow

$username = "lab8test_$(Get-Date -Format 'HHmmss')"
$registerBody = @{
    username = $username
    password = "Test@123"
    email = "$username@test.com"
    mobileNumber = "98765432$(Get-Random -Minimum 10 -Maximum 99)"
} | ConvertTo-Json

try {
    $regResponse = Invoke-RestMethod -Uri "$($baseUrls.login)/register" `
        -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "  ✓ User registered: $username" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

$loginBody = @{
    usernameOrEmailOrMobile = $username
    password = "Test@123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$($baseUrls.login)/login" `
        -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.data.token
    $userId = $loginResponse.data.userId
    Write-Host "  ✓ Login successful" -ForegroundColor Green
    Write-Host "    Token: $($token.Substring(0, 30))..." -ForegroundColor Gray
} catch {
    Write-Host "  ✗ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# Test 2: Customer Profile Creation
Write-Host "`nTest 2: Customer Profile Management" -ForegroundColor Yellow

$headers = @{Authorization = "Bearer $token"}
$customerBody = @{
    username = $username
    fullName = "Lab L8 Test User"
    mobileNumber = "9876543210"
    email = "$username@test.com"
    dateOfBirth = "1995-05-15"
    gender = "MALE"
    addressLine1 = "Test Address Line 1"
    city = "Mumbai"
    state = "Maharashtra"
    pincode = "400001"
    country = "India"
} | ConvertTo-Json

try {
    $customerResponse = Invoke-RestMethod -Uri "$($baseUrls.customer)" `
        -Method POST -Body $customerBody -Headers $headers -ContentType "application/json"
    $customerId = $customerResponse.id
    Write-Host "  ✓ Customer profile created (ID: $customerId)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Customer creation failed: $($_.Exception.Message)" -ForegroundColor Red
}

try {
    $profileResponse = Invoke-RestMethod -Uri "$($baseUrls.customer)/profile" `
        -Method GET -Headers $headers
    Write-Host "  ✓ Profile retrieved successfully" -ForegroundColor Green
    Write-Host "    Name: $($profileResponse.fullName)" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ Profile retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 3: Product Retrieval
Write-Host "`nTest 3: Product Service Integration" -ForegroundColor Yellow

try {
    $products = Invoke-RestMethod -Uri "$($baseUrls.product)/active" `
        -Method GET -Headers $headers
    Write-Host "  ✓ Retrieved $($products.Count) active products" -ForegroundColor Green
    
    if ($products.Count -gt 0) {
        $testProduct = $products[0]
        Write-Host "    Using product: $($testProduct.productCode) ($($testProduct.productName))" -ForegroundColor Gray
        Write-Host "    Base Rate: $($testProduct.interestRate)%" -ForegroundColor Gray
    }
} catch {
    Write-Host "  ✗ Product retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
    $testProduct = $null
}

# Test 4: FD Calculation (Standalone)
Write-Host "`nTest 4: FD Calculator - Standalone Mode" -ForegroundColor Yellow

$calcBody = @{
    principalAmount = 100000
    interestRate = 6.5
    tenureInMonths = 12
    interestCalculationType = "SIMPLE"
} | ConvertTo-Json

try {
    $calcResponse = Invoke-RestMethod -Uri "$($baseUrls.calculator)/calculate/standalone" `
        -Method POST -Body $calcBody -Headers $headers -ContentType "application/json"
    Write-Host "  ✓ Standalone calculation successful" -ForegroundColor Green
    Write-Host "    Principal: ₹$($calcResponse.principalAmount)" -ForegroundColor Gray
    Write-Host "    Interest: ₹$($calcResponse.interestAmount)" -ForegroundColor Gray
    Write-Host "    Maturity: ₹$($calcResponse.maturityAmount)" -ForegroundColor Gray
} catch {
    Write-Host "  ✗ Calculation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 5: FD Calculation (Product-Based)
if ($testProduct) {
    Write-Host "`nTest 5: FD Calculator - Product Integration" -ForegroundColor Yellow
    
    $prodCalcBody = @{
        productCode = $testProduct.productCode
        principalAmount = 100000
        tenureInMonths = 24
    } | ConvertTo-Json
    
    try {
        $prodCalcResponse = Invoke-RestMethod -Uri "$($baseUrls.calculator)/fd/calculate" `
            -Method POST -Body $prodCalcBody -Headers $headers -ContentType "application/json"
        Write-Host "  ✓ Product-based calculation successful" -ForegroundColor Green
        Write-Host "    Product: $($prodCalcResponse.productCode)" -ForegroundColor Gray
        Write-Host "    Rate: $($prodCalcResponse.interestRate)%" -ForegroundColor Gray
        Write-Host "    Maturity: ₹$($prodCalcResponse.maturityAmount)" -ForegroundColor Gray
    } catch {
        Write-Host "  ✗ Product calculation failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 6: Invalid Access Scenarios
Write-Host "`nTest 6: Security - Invalid Access Scenarios" -ForegroundColor Yellow

# Missing Token
try {
    Invoke-RestMethod -Uri "$($baseUrls.customer)/all" -Method GET
    Write-Host "  ✗ FAILED: Should reject missing token" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401 -or $_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "  ✓ Missing token rejected (401/403)" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Wrong status code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

# Invalid Token
$invalidHeaders = @{Authorization = "Bearer INVALID_TOKEN_12345"}
try {
    Invoke-RestMethod -Uri "$($baseUrls.customer)/profile" -Method GET -Headers $invalidHeaders
    Write-Host "  ✗ FAILED: Should reject invalid token" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 401) {
        Write-Host "  ✓ Invalid token rejected (401)" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Wrong status code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    }
}

# Unauthorized Role (USER trying to access ADMIN endpoint)
try {
    Invoke-RestMethod -Uri "$($baseUrls.customer)/all" -Method GET -Headers $headers
    Write-Host "  ✗ FAILED: USER should not access /all" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 403) {
        Write-Host "  ✓ Unauthorized role rejected (403)" -ForegroundColor Green
    } else {
        Write-Host "  ? Status code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    }
}

# Test 7: Admin Flow
Write-Host "`nTest 7: Admin Access - Full Privileges" -ForegroundColor Yellow

$adminLoginBody = @{
    usernameOrEmailOrMobile = "admin"
    password = "Admin@123"
} | ConvertTo-Json

try {
    $adminResponse = Invoke-RestMethod -Uri "$($baseUrls.login)/login" `
        -Method POST -Body $adminLoginBody -ContentType "application/json"
    $adminToken = $adminResponse.data.token
    $adminHeaders = @{Authorization = "Bearer $adminToken"}
    Write-Host "  ✓ Admin login successful" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Admin login failed: $($_.Exception.Message)" -ForegroundColor Red
    $adminHeaders = $null
}

if ($adminHeaders) {
    try {
        $allCustomers = Invoke-RestMethod -Uri "$($baseUrls.customer)/all" `
            -Method GET -Headers $adminHeaders
        Write-Host "  ✓ Admin accessed all customers ($($allCustomers.Count) records)" -ForegroundColor Green
    } catch {
        Write-Host "  ✗ Admin access failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Integration Test Suite Completed" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "Summary:" -ForegroundColor White
Write-Host "  - User Registration & Login: Tested" -ForegroundColor Gray
Write-Host "  - Customer Profile Management: Tested" -ForegroundColor Gray
Write-Host "  - Product Service Integration: Tested" -ForegroundColor Gray
Write-Host "  - FD Calculator (Standalone): Tested" -ForegroundColor Gray
Write-Host "  - FD Calculator (Product-based): Tested" -ForegroundColor Gray
Write-Host "  - Security (Invalid Access): Tested" -ForegroundColor Gray
Write-Host "  - Admin Privileges: Tested" -ForegroundColor Gray

Write-Host "`nNext Steps:" -ForegroundColor Yellow
Write-Host "  1. Review test results above" -ForegroundColor Gray
Write-Host "  2. Check service logs for detailed event tracking" -ForegroundColor Gray
Write-Host "  3. Explore Swagger UIs for interactive testing" -ForegroundColor Gray
Write-Host "     - Login: http://localhost:8081/api/auth/swagger-ui/index.html" -ForegroundColor Gray
Write-Host "     - Customer: http://localhost:8083/api/customer/swagger-ui/index.html" -ForegroundColor Gray
Write-Host "     - Product: http://localhost:8084/api/products/swagger-ui/index.html" -ForegroundColor Gray
Write-Host "     - Calculator: http://localhost:8085/api/calculator/swagger-ui/index.html" -ForegroundColor Gray
