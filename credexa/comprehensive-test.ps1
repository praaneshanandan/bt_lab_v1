# Comprehensive Testing Script for Credexa Services
# This script tests Login and Customer services with different roles

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "CREDEXA COMPREHENSIVE TESTING SCRIPT" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Variables to store tokens
$adminToken = ""
$customer1Token = ""
$customer2Token = ""
$manager1Token = ""

# Test counters
$totalTests = 0
$passedTests = 0
$failedTests = 0

function Test-Endpoint {
    param(
        [string]$TestName,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = "",
        [string]$Token = "",
        [string]$ExpectedStatus = "200"
    )

    $global:totalTests++
    Write-Host "`n--- Test: $TestName ---" -ForegroundColor Yellow

    try {
        $headers = @{
            "Content-Type" = "application/json"
        }

        if ($Token -ne "") {
            $headers["Authorization"] = "Bearer $Token"
        }

        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            UseBasicParsing = $true
        }

        if ($Body -ne "") {
            $params["Body"] = $Body
        }

        $response = Invoke-WebRequest @params

        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host "PASS: Status $($response.StatusCode)" -ForegroundColor Green
            $global:passedTests++
            return $response.Content | ConvertFrom-Json
        } else {
            Write-Host "FAIL: Expected $ExpectedStatus but got $($response.StatusCode)" -ForegroundColor Red
            $global:failedTests++
            return $null
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "PASS: Status $statusCode (Expected Error)" -ForegroundColor Green
            $global:passedTests++
        } else {
            Write-Host "FAIL: $($_.Exception.Message)" -ForegroundColor Red
            $global:failedTests++

            if ($_.Exception.Response) {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $reader.BaseStream.Position = 0
                $responseBody = $reader.ReadToEnd()
                Write-Host "Response: $responseBody" -ForegroundColor Red
            }
        }
        return $null
    }
}

# ========================================
# 1. HEALTH CHECKS
# ========================================
Write-Host "`n======== HEALTH CHECKS ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Login Service Health" -Url "http://localhost:8081/api/auth/health"
Test-Endpoint -TestName "Customer Service Health" -Url "http://localhost:8083/api/customer/health"

# ========================================
# 2. ADMIN LOGIN
# ========================================
Write-Host "`n======== ADMIN LOGIN ========" -ForegroundColor Magenta

$loginBody = @{
    usernameOrEmailOrMobile = "admin"
    password = "Admin@123"
} | ConvertTo-Json

$loginResponse = Test-Endpoint -TestName "Admin Login" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginBody

if ($loginResponse -and $loginResponse.success) {
    $adminToken = $loginResponse.data.token
    Write-Host "Admin Token: $adminToken" -ForegroundColor Green
}

# ========================================
# 3. REGISTER TEST USERS
# ========================================
Write-Host "`n======== REGISTER TEST USERS ========" -ForegroundColor Magenta

# Register customer1
$registerCustomer1 = @{
    username = "customer1"
    password = "Pass1234"
    email = "customer1@test.com"
    mobileNumber = "1234567890"
} | ConvertTo-Json

$regResponse1 = Test-Endpoint -TestName "Register customer1" -Url "http://localhost:8081/api/auth/register" -Method "POST" -Body $registerCustomer1

# Register customer2
$registerCustomer2 = @{
    username = "customer2"
    password = "Pass1234"
    email = "customer2@test.com"
    mobileNumber = "1234567891"
} | ConvertTo-Json

$regResponse2 = Test-Endpoint -TestName "Register customer2" -Url "http://localhost:8081/api/auth/register" -Method "POST" -Body $registerCustomer2

# Register manager1 (will be assigned MANAGER role later)
$registerManager1 = @{
    username = "manager1"
    password = "Pass1234"
    email = "manager1@test.com"
    mobileNumber = "1234567892"
} | ConvertTo-Json

$regResponse3 = Test-Endpoint -TestName "Register manager1" -Url "http://localhost:8081/api/auth/register" -Method "POST" -Body $registerManager1

# ========================================
# 4. LOGIN AS NEW USERS
# ========================================
Write-Host "`n======== LOGIN AS TEST USERS ========" -ForegroundColor Magenta

# Login customer1
$loginCustomer1 = @{
    usernameOrEmailOrMobile = "customer1"
    password = "Pass1234"
} | ConvertTo-Json

$loginResp1 = Test-Endpoint -TestName "Login as customer1" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginCustomer1

if ($loginResp1 -and $loginResp1.success) {
    $customer1Token = $loginResp1.data.token
}

# Login customer2
$loginCustomer2 = @{
    usernameOrEmailOrMobile = "customer2"
    password = "Pass1234"
} | ConvertTo-Json

$loginResp2 = Test-Endpoint -TestName "Login as customer2" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginCustomer2

if ($loginResp2 -and $loginResp2.success) {
    $customer2Token = $loginResp2.data.token
}

# Login manager1
$loginManager1 = @{
    usernameOrEmailOrMobile = "manager1"
    password = "Pass1234"
} | ConvertTo-Json

$loginResp3 = Test-Endpoint -TestName "Login as manager1" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginManager1

if ($loginResp3 -and $loginResp3.success) {
    $manager1Token = $loginResp3.data.token
}

# ========================================
# 5. VALIDATE TOKENS
# ========================================
Write-Host "`n======== VALIDATE TOKENS ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Validate Admin Token" -Url "http://localhost:8081/api/auth/validate-token?token=$adminToken" -Token $adminToken
Test-Endpoint -TestName "Validate Customer1 Token" -Url "http://localhost:8081/api/auth/validate-token?token=$customer1Token" -Token $customer1Token

# ========================================
# 6. GET USER PROFILE
# ========================================
Write-Host "`n======== GET USER PROFILE ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Admin - Get Own Profile" -Url "http://localhost:8081/api/auth/profile" -Token $adminToken
Test-Endpoint -TestName "Customer1 - Get Own Profile" -Url "http://localhost:8081/api/auth/profile" -Token $customer1Token

# ========================================
# 7. GET ACTIVE SESSIONS
# ========================================
Write-Host "`n======== GET ACTIVE SESSIONS ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Admin - Get Active Sessions" -Url "http://localhost:8081/api/auth/sessions" -Token $adminToken
Test-Endpoint -TestName "Customer1 - Get Active Sessions" -Url "http://localhost:8081/api/auth/sessions" -Token $customer1Token

# ========================================
# 8. CUSTOMER SERVICE - CREATE CUSTOMER PROFILES
# ========================================
Write-Host "`n======== CREATE CUSTOMER PROFILES ========" -ForegroundColor Magenta

# Customer1 creates own profile
$customer1Profile = @{
    userId = $loginResp1.data.userId
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

$custProfile1 = Test-Endpoint -TestName "Customer1 - Create Own Profile" -Url "http://localhost:8083/api/customer" -Method "POST" -Body $customer1Profile -Token $customer1Token

# Customer2 creates own profile
$customer2Profile = @{
    userId = $loginResp2.data.userId
    fullName = "Jane Smith"
    dateOfBirth = "1985-05-20"
    gender = "FEMALE"
    addressLine1 = "456 Oak Ave"
    city = "Los Angeles"
    state = "CA"
    postalCode = "90001"
    country = "USA"
    occupation = "Business Owner"
    annualIncome = 120000.0
    identificationType = "PAN"
    identificationNumber = "ABCDE1234F"
} | ConvertTo-Json

$custProfile2 = Test-Endpoint -TestName "Customer2 - Create Own Profile" -Url "http://localhost:8083/api/customer" -Method "POST" -Body $customer2Profile -Token $customer2Token

# ========================================
# 9. CUSTOMER SERVICE - GET CUSTOMER PROFILES
# ========================================
Write-Host "`n======== GET CUSTOMER PROFILES ========" -ForegroundColor Magenta

# Get all customers (should fail for CUSTOMER role)
Test-Endpoint -TestName "Customer1 - Get All Customers (Should Fail)" -Url "http://localhost:8083/api/customer/all" -Token $customer1Token -ExpectedStatus "403"

# Get all customers as admin (should succeed)
Test-Endpoint -TestName "Admin - Get All Customers" -Url "http://localhost:8083/api/customer/all" -Token $adminToken

# Get customer by userId
if ($custProfile1 -and $custProfile1.success) {
    $customer1Id = $custProfile1.data.id
    Test-Endpoint -TestName "Customer1 - Get Own Profile by User ID" -Url "http://localhost:8083/api/customer/user/$($loginResp1.data.userId)" -Token $customer1Token

    # Try to access as different customer (should fail due to business logic)
    Test-Endpoint -TestName "Customer2 - Get Customer1 Profile (Should Fail)" -Url "http://localhost:8083/api/customer/$customer1Id" -Token $customer2Token -ExpectedStatus "403"

    # Get classification
    Test-Endpoint -TestName "Customer1 - Get Own Classification" -Url "http://localhost:8083/api/customer/$customer1Id/classification" -Token $customer1Token
}

# ========================================
# 10. CUSTOMER SERVICE - UPDATE PROFILE
# ========================================
Write-Host "`n======== UPDATE CUSTOMER PROFILES ========" -ForegroundColor Magenta

if ($custProfile1 -and $custProfile1.success) {
    $updateProfile = @{
        userId = $loginResp1.data.userId
        fullName = "John Doe Updated"
        dateOfBirth = "1990-01-15"
        gender = "MALE"
        addressLine1 = "123 Main St Apt 5"
        city = "New York"
        state = "NY"
        postalCode = "10001"
        country = "USA"
        occupation = "Senior Software Engineer"
        annualIncome = 95000.0
        identificationType = "AADHAAR"
        identificationNumber = "123456789012"
    } | ConvertTo-Json

    Test-Endpoint -TestName "Customer1 - Update Own Profile" -Url "http://localhost:8083/api/customer/$customer1Id" -Method "PUT" -Body $updateProfile -Token $customer1Token
}

# ========================================
# 11. LOGOUT TESTS
# ========================================
Write-Host "`n======== LOGOUT TESTS ========" -ForegroundColor Magenta

# Logout customer2
Test-Endpoint -TestName "Customer2 - Logout" -Url "http://localhost:8081/api/auth/logout" -Method "POST" -Token $customer2Token

# Try to use customer2 token after logout (should fail)
Test-Endpoint -TestName "Customer2 - Access After Logout (Should Fail)" -Url "http://localhost:8081/api/auth/profile" -Token $customer2Token -ExpectedStatus "401"

# ========================================
# SUMMARY
# ========================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $passedTests" -ForegroundColor Green
Write-Host "Failed: $failedTests" -ForegroundColor Red
$successRate = [math]::Round(($passedTests / $totalTests) * 100, 2)
Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } else { "Yellow" })
Write-Host "========================================`n" -ForegroundColor Cyan

# ========================================
# SAVE TEST CREDENTIALS
# ========================================
$credentials = @"
# TEST CREDENTIALS FOR CREDEXA

## Admin User
- **Username**: admin
- **Password**: Admin@123
- **Email**: admin@credexa.com
- **Mobile**: 9999999999
- **Role**: ROLE_ADMIN
- **Token**: $adminToken

## Customer 1
- **Username**: customer1
- **Password**: Pass1234
- **Email**: customer1@test.com
- **Mobile**: 1234567890
- **Role**: ROLE_CUSTOMER
- **Token**: $customer1Token
- **Customer ID**: $customer1Id

## Customer 2
- **Username**: customer2
- **Password**: Pass1234
- **Email**: customer2@test.com
- **Mobile**: 1234567891
- **Role**: ROLE_CUSTOMER
- **Token**: $customer2Token
- **Status**: Logged Out

## Manager 1
- **Username**: manager1
- **Password**: Pass1234
- **Email**: manager1@test.com
- **Mobile**: 1234567892
- **Role**: ROLE_CUSTOMER (needs manual upgrade to ROLE_MANAGER)
- **Token**: $manager1Token

---
**Generated**: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
"@

$credentials | Out-File -FilePath "c:\Users\dhruv\Coding\bt_lab_v1\credexa\proper_documentation\test-credentials.md" -Encoding UTF8

Write-Host "Credentials saved to: proper_documentation\test-credentials.md" -ForegroundColor Green
