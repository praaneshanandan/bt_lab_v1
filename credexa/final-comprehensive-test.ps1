# Final Comprehensive Test Script for Credexa Login & Customer Services
# Updated with correct DTO fields

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "CREDEXA COMPREHENSIVE TESTING SCRIPT V2" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test counters
$totalTests = 0
$passedTests = 0
$failedTests = 0

# Store tokens and IDs
$adminToken = ""
$customer1Token = ""
$customer2Token = ""
$manager1Token = ""
$customer1Id = 0
$customer2Id = 0

function Test-Endpoint {
    param(
        [string]$TestName,
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = "",
        [string]$Token = "",
        [int]$ExpectedStatus = 200
    )

    $global:totalTests++
    Write-Host "`n--- Test $global:totalTests: $TestName ---" -ForegroundColor Yellow

    try {
        $params = @{
            Uri = $Url
            Method = $Method
            UseBasicParsing = $true
        }

        if ($Token -ne "") {
            $params["Headers"] = @{
                "Authorization" = "Bearer $Token"
                "Content-Type" = "application/json"
            }
        } else {
            $params["ContentType"] = "application/json"
        }

        if ($Body -ne "") {
            $params["Body"] = $Body
        }

        $response = Invoke-WebRequest @params

        if ($response.StatusCode -eq $ExpectedStatus) {
            Write-Host "PASS: Status $($response.StatusCode)" -ForegroundColor Green
            $global:passedTests++

            if ($response.Content) {
                return $response.Content | ConvertFrom-Json -ErrorAction SilentlyContinue
            }
            return $null
        } else {
            Write-Host "FAIL: Expected $ExpectedStatus but got $($response.StatusCode)" -ForegroundColor Red
            $global:failedTests++
            return $null
        }
    } catch {
        $statusCode = if ($_.Exception.Response.StatusCode.value__) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "PASS: Status $statusCode (Expected Error)" -ForegroundColor Green
            $global:passedTests++
        } else {
            Write-Host "FAIL: Expected $ExpectedStatus but got $statusCode" -ForegroundColor Red
            Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
            $global:failedTests++
        }
        return $null
    }
}


# ========================================
# 1. HEALTH CHECKS
# ========================================
Write-Host "`n======== 1. HEALTH CHECKS ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Login Service Health" -Url "http://localhost:8081/api/auth/health"
Test-Endpoint -TestName "Customer Service Health" -Url "http://localhost:8083/api/customer/health"

# ========================================
# 2. USER REGISTRATION
# ========================================
Write-Host "`n======== 2. USER REGISTRATION ========" -ForegroundColor Magenta

$registerCustomer1 = @{
    username = "customer1"
    password = "Pass1234"
    email = "customer1@test.com"
    mobileNumber = "9876543210"
} | ConvertTo-Json

$regResp1 = Test-Endpoint -TestName "Register customer1" -Url "http://localhost:8081/api/auth/register" -Method "POST" -Body $registerCustomer1 -ExpectedStatus 201

$registerCustomer2 = @{
    username = "customer2"
    password = "Pass1234"
    email = "customer2@test.com"
    mobileNumber = "9876543211"
} | ConvertTo-Json

$regResp2 = Test-Endpoint -TestName "Register customer2" -Url "http://localhost:8081/api/auth/register" -Method "POST" -Body $registerCustomer2 -ExpectedStatus 201

$registerManager1 = @{
    username = "manager1"
    password = "Pass1234"
    email = "manager1@test.com"
    mobileNumber = "9876543212"
} | ConvertTo-Json

$regResp3 = Test-Endpoint -TestName "Register manager1" -Url "http://localhost:8081/api/auth/register" -Method "POST" -Body $registerManager1 -ExpectedStatus 201

# ========================================
# 3. USER LOGIN
# ========================================
Write-Host "`n======== 3. USER LOGIN ========" -ForegroundColor Magenta

$loginAdmin = @{
    usernameOrEmailOrMobile = "admin"
    password = "Admin@123"
} | ConvertTo-Json

$adminResp = Test-Endpoint -TestName "Login as admin" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginAdmin

if ($adminResp -and $adminResp.success) {
    $adminToken = $adminResp.data.token
    Write-Host "  Admin Token: $($adminToken.Substring(0,50))..." -ForegroundColor Cyan
}

$loginCustomer1 = @{
    usernameOrEmailOrMobile = "customer1"
    password = "Pass1234"
} | ConvertTo-Json

$c1Resp = Test-Endpoint -TestName "Login as customer1" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginCustomer1

if ($c1Resp -and $c1Resp.success) {
    $customer1Token = $c1Resp.data.token
    Write-Host "  Customer1 User ID: $($c1Resp.data.userId)" -ForegroundColor Cyan
}

$loginCustomer2 = @{
    usernameOrEmailOrMobile = "customer2"
    password = "Pass1234"
} | ConvertTo-Json

$c2Resp = Test-Endpoint -TestName "Login as customer2" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginCustomer2

if ($c2Resp -and $c2Resp.success) {
    $customer2Token = $c2Resp.data.token
}

$loginManager1 = @{
    usernameOrEmailOrMobile = "manager1"
    password = "Pass1234"
} | ConvertTo-Json

$m1Resp = Test-Endpoint -TestName "Login as manager1" -Url "http://localhost:8081/api/auth/login" -Method "POST" -Body $loginManager1

if ($m1Resp -and $m1Resp.success) {
    $manager1Token = $m1Resp.data.token
}

# ========================================
# 4. TOKEN VALIDATION
# ========================================
Write-Host "`n======== 4. TOKEN VALIDATION ========" -ForegroundColor Magenta

$validateBody = @{
    token = $adminToken
} | ConvertTo-Json

Test-Endpoint -TestName "Validate admin token" -Url "http://localhost:8081/api/auth/validate-token" -Method "POST" -Body $validateBody

# ========================================
# 5. BANK CONFIGURATION
# ========================================
Write-Host "`n======== 5. BANK CONFIGURATION ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Get bank configuration" -Url "http://localhost:8081/api/auth/bank-config"

# ========================================
# 6. CREATE CUSTOMER PROFILES
# ========================================
Write-Host "`n======== 6. CREATE CUSTOMER PROFILES ========" -ForegroundColor Magenta

$customer1Profile = @{
    fullName = "John Doe"
    mobileNumber = "9876543210"
    email = "customer1@test.com"
    panNumber = "ABCDE1234F"
    aadharNumber = "123456789012"
    dateOfBirth = "1990-01-15"
    gender = "MALE"
    classification = "REGULAR"
    addressLine1 = "123 Main Street"
    addressLine2 = "Apt 5B"
    city = "Mumbai"
    state = "Maharashtra"
    pincode = "400001"
    country = "India"
    accountNumber = "ACC1234567890"
    ifscCode = "HDFC0001234"
    preferredLanguage = "en"
    preferredCurrency = "INR"
    emailNotifications = $true
    smsNotifications = $true
} | ConvertTo-Json

$custResp1 = Test-Endpoint -TestName "Customer1 creates own profile" -Url "http://localhost:8083/api/customer" -Method "POST" -Body $customer1Profile -Token $customer1Token -ExpectedStatus 201

if ($custResp1 -and $custResp1.data) {
    $customer1Id = $custResp1.data.id
    Write-Host "  Customer1 Profile ID: $customer1Id" -ForegroundColor Cyan
}

$customer2Profile = @{
    fullName = "Jane Smith"
    mobileNumber = "9876543211"
    email = "customer2@test.com"
    panNumber = "FGHIJ5678K"
    aadharNumber = "987654321098"
    dateOfBirth = "1985-05-20"
    gender = "FEMALE"
    classification = "PREMIUM"
    addressLine1 = "456 Oak Avenue"
    city = "Delhi"
    state = "Delhi"
    pincode = "110001"
    country = "India"
    accountNumber = "ACC9876543210"
    ifscCode = "ICIC0005678"
    preferredLanguage = "en"
    preferredCurrency = "INR"
    emailNotifications = $false
    smsNotifications = $true
} | ConvertTo-Json

$custResp2 = Test-Endpoint -TestName "Customer2 creates own profile" -Url "http://localhost:8083/api/customer" -Method "POST" -Body $customer2Profile -Token $customer2Token -ExpectedStatus 201

if ($custResp2 -and $custResp2.data) {
    $customer2Id = $custResp2.data.id
    Write-Host "  Customer2 Profile ID: $customer2Id" -ForegroundColor Cyan
}

# ========================================
# 7. GET CUSTOMER PROFILES
# ========================================
Write-Host "`n======== 7. GET CUSTOMER PROFILES ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Admin gets all customers" -Url "http://localhost:8083/api/customer/all" -Token $adminToken

Test-Endpoint -TestName "Customer1 tries to get all (should fail)" -Url "http://localhost:8083/api/customer/all" -Token $customer1Token -ExpectedStatus 403

if ($customer1Id -gt 0) {
    Test-Endpoint -TestName "Customer1 gets own profile by ID" -Url "http://localhost:8083/api/customer/$customer1Id" -Token $customer1Token

    Test-Endpoint -TestName "Customer2 tries to get Customer1 profile (should fail)" -Url "http://localhost:8083/api/customer/$customer1Id" -Token $customer2Token -ExpectedStatus 403

    Test-Endpoint -TestName "Admin gets Customer1 profile" -Url "http://localhost:8083/api/customer/$customer1Id" -Token $adminToken
}

# ========================================
# 8. UPDATE CUSTOMER PROFILE
# ========================================
Write-Host "`n======== 8. UPDATE CUSTOMER PROFILE ========" -ForegroundColor Magenta

if ($customer1Id -gt 0) {
    $updateProfile = @{
        fullName = "John Doe Updated"
        mobileNumber = "9876543210"
        email = "customer1@test.com"
        panNumber = "ABCDE1234F"
        aadharNumber = "123456789012"
        dateOfBirth = "1990-01-15"
        gender = "MALE"
        classification = "PREMIUM"
        addressLine1 = "123 Main Street Updated"
        addressLine2 = "Apt 5B"
        city = "Mumbai"
        state = "Maharashtra"
        pincode = "400001"
        country = "India"
        accountNumber = "ACC1234567890"
        ifscCode = "HDFC0001234"
        preferredLanguage = "en"
        preferredCurrency = "USD"
        emailNotifications = $true
        smsNotifications = $false
    } | ConvertTo-Json

    Test-Endpoint -TestName "Customer1 updates own profile" -Url "http://localhost:8083/api/customer/$customer1Id" -Method "PUT" -Body $updateProfile -Token $customer1Token
}

# ========================================
# 9. GET CUSTOMER CLASSIFICATION
# ========================================
Write-Host "`n======== 9. CUSTOMER CLASSIFICATION ========" -ForegroundColor Magenta

if ($customer1Id -gt 0) {
    Test-Endpoint -TestName "Get Customer1 classification" -Url "http://localhost:8083/api/customer/$customer1Id/classification" -Token $customer1Token
}

# ========================================
# 10. LOGOUT
# ========================================
Write-Host "`n======== 10. LOGOUT ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Customer2 logout" -Url "http://localhost:8081/api/auth/logout" -Method "POST" -Token $customer2Token

Test-Endpoint -TestName "Customer2 access after logout (should fail)" -Url "http://localhost:8083/api/customer/all" -Token $customer2Token -ExpectedStatus 401

# ========================================
# 11. GET USER BY USERNAME (Inter-service)
# ========================================
Write-Host "`n======== 11. INTER-SERVICE ENDPOINTS ========" -ForegroundColor Magenta

Test-Endpoint -TestName "Get user by username (customer1)" -Url "http://localhost:8081/api/auth/user/customer1"

# ========================================
# SUMMARY
# ========================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Total Tests: $totalTests" -ForegroundColor White
Write-Host "Passed: $passedTests" -ForegroundColor Green
Write-Host "Failed: $failedTests" -ForegroundColor Red

if ($totalTests -gt 0) {
    $successRate = [math]::Round(($passedTests / $totalTests) * 100, 2)
    Write-Host "Success Rate: $successRate%" -ForegroundColor $(if ($successRate -ge 80) { "Green" } elseif ($successRate -ge 60) { "Yellow" } else { "Red" })
}

Write-Host "========================================`n" -ForegroundColor Cyan

# ========================================
# SAVE TEST CREDENTIALS
# ========================================
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"

$credentials = @"
# TEST CREDENTIALS FOR CREDEXA

**Generated:** $timestamp
**Test Status:** $passedTests/$totalTests tests passed ($successRate%)

---

## Admin User

- **Username:** admin
- **Password:** Admin@123
- **Email:** admin@credexa.com
- **Mobile:** 9999999999
- **Role:** ROLE_ADMIN
- **User ID:** 1
- **Token:** ``$adminToken``

---

## Customer 1

- **Username:** customer1
- **Password:** Pass1234
- **Email:** customer1@test.com
- **Mobile:** 9876543210
- **Role:** ROLE_CUSTOMER
- **Customer Profile ID:** $customer1Id
- **Token:** ``$customer1Token``

**Profile Details:**
- Full Name: John Doe (Updated to John Doe Updated)
- PAN: ABCDE1234F
- Aadhar: 123456789012
- DOB: 1990-01-15
- Gender: MALE
- Classification: PREMIUM (upgraded from REGULAR)
- City: Mumbai, Maharashtra
- Account: ACC1234567890, IFSC: HDFC0001234

---

## Customer 2

- **Username:** customer2
- **Password:** Pass1234
- **Email:** customer2@test.com
- **Mobile:** 9876543211
- **Role:** ROLE_CUSTOMER
- **Customer Profile ID:** $customer2Id
- **Token:** ``$customer2Token`` *(logged out during testing)*
- **Status:** LOGGED OUT

**Profile Details:**
- Full Name: Jane Smith
- PAN: FGHIJ5678K
- Aadhar: 987654321098
- DOB: 1985-05-20
- Gender: FEMALE
- Classification: PREMIUM
- City: Delhi, Delhi
- Account: ACC9876543210, IFSC: ICIC0005678

---

## Manager 1

- **Username:** manager1
- **Password:** Pass1234
- **Email:** manager1@test.com
- **Mobile:** 9876543212
- **Role:** ROLE_CUSTOMER (registered, not yet upgraded to MANAGER)
- **Token:** ``$manager1Token``

**Note:** To test MANAGER role, this user needs to be manually assigned ROLE_MANAGER in the database.

---

## SQL Commands to Upgrade manager1 to MANAGER Role

``````sql
-- Check current roles
SELECT u.id, u.username, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1';

-- Get role IDs
SELECT id, name FROM roles;

-- Add MANAGER role to manager1 (assuming ROLE_MANAGER has id=2)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'manager1' AND r.name = 'ROLE_MANAGER';

-- Remove CUSTOMER role if needed
DELETE ur FROM user_roles ur
JOIN users u ON ur.user_id = u.id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1' AND r.name = 'ROLE_CUSTOMER';

-- Verify
SELECT u.id, u.username, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1';
``````

---

## Quick Test Commands

### Login as Admin
``````bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmailOrMobile":"admin","password":"Admin@123"}'
``````

### Login as Customer1
``````bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmailOrMobile":"customer1","password":"Pass1234"}'
``````

### Get All Customers (Admin)
``````bash
curl -X GET http://localhost:8083/api/customer/all \
  -H "Authorization: Bearer <admin-token>"
``````

---

**End of Test Credentials**
"@

$credentials | Out-File -FilePath "c:\Users\dhruv\Coding\bt_lab_v1\credexa\proper_documentation\test-credentials.md" -Encoding UTF8

Write-Host "✓ Test credentials saved to: proper_documentation\test-credentials.md" -ForegroundColor Green
Write-Host "✓ Test results can be reviewed above" -ForegroundColor Green
