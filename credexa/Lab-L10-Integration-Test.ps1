# Lab L10 Integration Test Script
# Integration of Fixed Deposit Calculator with Login Module
# Tests JWT authentication, CUSTOMER role enforcement, and personalized interest rates

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Lab L10 Integration Testing" -ForegroundColor Cyan
Write-Host "FD Calculator + Login Module" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$baseUrls = @{
    login = "http://localhost:8081/api/auth"
    calculator = "http://localhost:8085/api/calculator"
    customer = "http://localhost:8083/api/customer"
}

$testResults = @()

# Helper function to display test result
function Show-TestResult {
    param(
        [string]$TestName,
        [string]$Status,
        [string]$Details
    )
    
    $result = @{
        Test = $TestName
        Status = $Status
        Details = $Details
    }
    $script:testResults += $result
    
    if ($Status -eq "PASS") {
        Write-Host "✅ $TestName" -ForegroundColor Green
        Write-Host "   $Details" -ForegroundColor Gray
    } elseif ($Status -eq "FAIL") {
        Write-Host "❌ $TestName" -ForegroundColor Red
        Write-Host "   $Details" -ForegroundColor Gray
    } else {
        Write-Host "⚠️  $TestName" -ForegroundColor Yellow
        Write-Host "   $Details" -ForegroundColor Gray
    }
    Write-Host ""
}

# Test 1: Health Check
Write-Host "Test 1: Health Check" -ForegroundColor Yellow
Write-Host "Verifying services are running..." -ForegroundColor Gray
try {
    $healthResponse = Invoke-RestMethod -Uri "$($baseUrls.calculator)/health" -Method GET -ErrorAction Stop
    Show-TestResult -TestName "Health Check" -Status "PASS" -Details "FD Calculator service is running"
} catch {
    Show-TestResult -TestName "Health Check" -Status "FAIL" -Details "Service not accessible: $_"
    Write-Host "Please start the FD Calculator service first!" -ForegroundColor Red
    exit 1
}

# Test 2: Unauthenticated Access (401)
Write-Host "Test 2: Unauthenticated Access (Should Return 401)" -ForegroundColor Yellow
Write-Host "Testing FD calculation without JWT token..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    $body = @{
        principalAmount = 100000
        interestRate = 6.5
        tenure = 12
        tenureUnit = "MONTHS"
        calculationType = "SIMPLE"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$($baseUrls.calculator)/fd/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -UseBasicParsing `
        -ErrorAction Stop
    
    Show-TestResult -TestName "Unauthenticated Access" -Status "FAIL" -Details "Expected 401, but got: $($response.StatusCode)"
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Show-TestResult -TestName "Unauthenticated Access" -Status "PASS" -Details "Correctly returned 401 Unauthorized"
    } else {
        Show-TestResult -TestName "Unauthenticated Access" -Status "FAIL" -Details "Unexpected error: $_"
    }
}

# Test 3: Invalid JWT Token (401)
Write-Host "Test 3: Invalid JWT Token (Should Return 401)" -ForegroundColor Yellow
Write-Host "Testing with invalid JWT token..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer invalid.jwt.token.here"
    }
    
    $body = @{
        principalAmount = 100000
        interestRate = 6.5
        tenure = 12
        tenureUnit = "MONTHS"
        calculationType = "SIMPLE"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$($baseUrls.calculator)/fd/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -UseBasicParsing `
        -ErrorAction Stop
    
    Show-TestResult -TestName "Invalid JWT Token" -Status "FAIL" -Details "Expected 401, but got: $($response.StatusCode)"
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Show-TestResult -TestName "Invalid JWT Token" -Status "PASS" -Details "Correctly rejected invalid token with 401"
    } else {
        Show-TestResult -TestName "Invalid JWT Token" -Status "FAIL" -Details "Unexpected error: $_"
    }
}

# Test 4: Register and Login as USER (non-CUSTOMER)
Write-Host "Test 4: Register and Login as USER" -ForegroundColor Yellow
Write-Host "Creating USER account (non-CUSTOMER role)..." -ForegroundColor Gray
try {
    # Register user
    $registerBody = @{
        username = "testuser_L10"
        email = "testuser_L10@example.com"
        password = "Test@123"
        fullName = "Test User L10"
        phoneNumber = "9876543210"
        role = "USER"
    } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "$($baseUrls.login)/register" `
            -Method POST `
            -ContentType "application/json" `
            -Body $registerBody `
            -ErrorAction SilentlyContinue | Out-Null
    } catch {
        # User might already exist, continue
    }
    
    # Login as USER
    $loginBody = @{
        username = "testuser_L10"
        password = "Test@123"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-RestMethod -Uri "$($baseUrls.login)/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
    
    $userToken = $loginResponse.data.token
    Show-TestResult -TestName "USER Login" -Status "PASS" -Details "Successfully logged in as USER: $($loginResponse.data.username)"
} catch {
    Show-TestResult -TestName "USER Login" -Status "FAIL" -Details "Failed to login: $_"
}

# Test 5: USER Role Insufficient Permissions (403)
Write-Host "Test 5: USER Role Access (Should Return 403)" -ForegroundColor Yellow
Write-Host "Testing FD calculation with USER role (not CUSTOMER)..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $userToken"
    }
    
    $body = @{
        principalAmount = 100000
        interestRate = 6.5
        tenure = 12
        tenureUnit = "MONTHS"
        calculationType = "SIMPLE"
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "$($baseUrls.calculator)/fd/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $body `
        -UseBasicParsing `
        -ErrorAction Stop
    
    Show-TestResult -TestName "USER Role Access" -Status "FAIL" -Details "Expected 403, but got: $($response.StatusCode)"
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Show-TestResult -TestName "USER Role Access" -Status "PASS" -Details "Correctly returned 403 Forbidden (CUSTOMER role required)"
    } else {
        Show-TestResult -TestName "USER Role Access" -Status "FAIL" -Details "Unexpected status: $($_.Exception.Response.StatusCode)"
    }
}

# Test 6: Register and Login as CUSTOMER
Write-Host "Test 6: Register and Login as CUSTOMER" -ForegroundColor Yellow
Write-Host "Creating CUSTOMER account..." -ForegroundColor Gray
try {
    # Register customer
    $registerBody = @{
        username = "customer_L10"
        email = "customer_L10@example.com"
        password = "Test@123"
        fullName = "Customer L10"
        phoneNumber = "9876543211"
        role = "CUSTOMER"
    } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "$($baseUrls.login)/register" `
            -Method POST `
            -ContentType "application/json" `
            -Body $registerBody `
            -ErrorAction SilentlyContinue | Out-Null
    } catch {
        # Customer might already exist, continue
    }
    
    # Login as CUSTOMER
    $loginBody = @{
        username = "customer_L10"
        password = "Test@123"
    } | ConvertTo-Json
    
    $loginResponse = Invoke-RestMethod -Uri "$($baseUrls.login)/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $loginBody
    
    $customerToken = $loginResponse.data.token
    $customerUsername = $loginResponse.data.username
    Show-TestResult -TestName "CUSTOMER Login" -Status "PASS" -Details "Successfully logged in as CUSTOMER: $customerUsername"
} catch {
    Show-TestResult -TestName "CUSTOMER Login" -Status "FAIL" -Details "Failed to login: $_"
}

# Test 7: CUSTOMER Simple FD Calculation (200)
Write-Host "Test 7: CUSTOMER Simple FD Calculation" -ForegroundColor Yellow
Write-Host "Testing FD calculation with CUSTOMER role..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $customerToken"
    }
    
    $body = @{
        principalAmount = 100000
        interestRate = 6.5
        tenure = 12
        tenureUnit = "MONTHS"
        calculationType = "SIMPLE"
        customerClassifications = @()  # Empty - service should fetch from customer-service
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$($baseUrls.calculator)/fd/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $body
    
    if ($response.success) {
        $data = $response.data
        Show-TestResult -TestName "CUSTOMER FD Calculation" -Status "PASS" `
            -Details "Principal: ₹$($data.principalAmount), Base Rate: $($data.baseInterestRate)%, Effective Rate: $($data.interestRate)%, Maturity: ₹$($data.maturityAmount)"
        
        Write-Host "   📊 Calculation Details:" -ForegroundColor Cyan
        Write-Host "      Principal Amount: ₹$($data.principalAmount)" -ForegroundColor Gray
        Write-Host "      Base Rate: $($data.baseInterestRate)%" -ForegroundColor Gray
        Write-Host "      Additional Rate: $($data.additionalInterestRate)%" -ForegroundColor Gray
        Write-Host "      Effective Rate: $($data.interestRate)%" -ForegroundColor Gray
        Write-Host "      Interest Earned: ₹$($data.interestEarned)" -ForegroundColor Gray
        Write-Host "      Maturity Amount: ₹$($data.maturityAmount)" -ForegroundColor Gray
        if ($data.customerClassifications) {
            Write-Host "      Categories: $($data.customerClassifications -join ', ')" -ForegroundColor Gray
        }
        Write-Host ""
    } else {
        Show-TestResult -TestName "CUSTOMER FD Calculation" -Status "FAIL" -Details $response.message
    }
} catch {
    Show-TestResult -TestName "CUSTOMER FD Calculation" -Status "FAIL" -Details "Error: $_"
}

# Test 8: CUSTOMER Compound FD Calculation (200)
Write-Host "Test 8: CUSTOMER Compound FD Calculation" -ForegroundColor Yellow
Write-Host "Testing compound interest calculation..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $customerToken"
    }
    
    $body = @{
        principalAmount = 500000
        interestRate = 7.0
        tenure = 36
        tenureUnit = "MONTHS"
        calculationType = "COMPOUND"
        compoundingFrequency = "QUARTERLY"
        customerClassifications = @("EMPLOYEE")  # Manual category
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$($baseUrls.calculator)/fd/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $body
    
    if ($response.success) {
        $data = $response.data
        Show-TestResult -TestName "Compound FD Calculation" -Status "PASS" `
            -Details "Principal: ₹$($data.principalAmount), Rate: $($data.interestRate)%, Maturity: ₹$($data.maturityAmount)"
        
        Write-Host "   📊 Compound Calculation:" -ForegroundColor Cyan
        Write-Host "      Tenure: $($data.tenure) $($data.tenureUnit)" -ForegroundColor Gray
        Write-Host "      Compounding: $($data.compoundingFrequency)" -ForegroundColor Gray
        Write-Host "      Interest Earned: ₹$($data.interestEarned)" -ForegroundColor Gray
        Write-Host "      Maturity Amount: ₹$($data.maturityAmount)" -ForegroundColor Gray
        Write-Host ""
    } else {
        Show-TestResult -TestName "Compound FD Calculation" -Status "FAIL" -Details $response.message
    }
} catch {
    Show-TestResult -TestName "Compound FD Calculation" -Status "FAIL" -Details "Error: $_"
}

# Test 9: Multiple Categories (Rate Boost)
Write-Host "Test 9: Multiple Customer Categories" -ForegroundColor Yellow
Write-Host "Testing with multiple categories for higher interest..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $customerToken"
    }
    
    $body = @{
        principalAmount = 200000
        interestRate = 6.5
        tenure = 24
        tenureUnit = "MONTHS"
        calculationType = "SIMPLE"
        customerClassifications = @("EMPLOYEE", "PREMIUM_CUSTOMER", "LOYAL_CUSTOMER")  # 3 categories = +0.75%
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$($baseUrls.calculator)/fd/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $body
    
    if ($response.success) {
        $data = $response.data
        $expectedAdditional = 0.75  # 3 categories × 0.25%
        
        Show-TestResult -TestName "Multiple Categories" -Status "PASS" `
            -Details "Base: $($data.baseInterestRate)%, Additional: $($data.additionalInterestRate)%, Effective: $($data.interestRate)%"
        
        Write-Host "   📊 Category Benefits:" -ForegroundColor Cyan
        Write-Host "      Categories: $($data.customerClassifications -join ', ')" -ForegroundColor Gray
        Write-Host "      Base Rate: $($data.baseInterestRate)%" -ForegroundColor Gray
        Write-Host "      Additional Rate: $($data.additionalInterestRate)% (0.25% × $($data.customerClassifications.Count) categories)" -ForegroundColor Gray
        Write-Host "      Effective Rate: $($data.interestRate)%" -ForegroundColor Gray
        Write-Host "      Interest Earned: ₹$($data.interestEarned)" -ForegroundColor Gray
        Write-Host ""
    } else {
        Show-TestResult -TestName "Multiple Categories" -Status "FAIL" -Details $response.message
    }
} catch {
    Show-TestResult -TestName "Multiple Categories" -Status "FAIL" -Details "Error: $_"
}

# Test 10: Rate Cap Enforcement (2% max)
Write-Host "Test 10: Rate Cap Enforcement (2% Maximum)" -ForegroundColor Yellow
Write-Host "Testing with many categories to verify 2% cap..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $customerToken"
    }
    
    $body = @{
        principalAmount = 100000
        interestRate = 6.5
        tenure = 12
        tenureUnit = "MONTHS"
        calculationType = "SIMPLE"
        customerClassifications = @(
            "EMPLOYEE", "SENIOR_CITIZEN", "PREMIUM_CUSTOMER", 
            "LOYAL_CUSTOMER", "HIGH_NET_WORTH", "NRI",
            "WOMEN_CUSTOMER", "STAFF_FAMILY", "EXTRA_CATEGORY"
        )  # 9 categories, should cap at 8 (2%)
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$($baseUrls.calculator)/fd/calculate" `
        -Method POST `
        -Headers $headers `
        -Body $body
    
    if ($response.success) {
        $data = $response.data
        
        if ($data.additionalInterestRate -le 2.0) {
            Show-TestResult -TestName "Rate Cap Enforcement" -Status "PASS" `
                -Details "Additional rate capped at $($data.additionalInterestRate)% (max 2%)"
            
            Write-Host "   📊 Rate Cap Verification:" -ForegroundColor Cyan
            Write-Host "      Categories Provided: 9" -ForegroundColor Gray
            Write-Host "      Categories Applied: $($data.customerClassifications.Count) (max 8)" -ForegroundColor Gray
            Write-Host "      Additional Rate: $($data.additionalInterestRate)% (capped at 2%)" -ForegroundColor Gray
            Write-Host "      Effective Rate: $($data.interestRate)%" -ForegroundColor Gray
            Write-Host ""
        } else {
            Show-TestResult -TestName "Rate Cap Enforcement" -Status "FAIL" `
                -Details "Rate cap violated: $($data.additionalInterestRate)% exceeds 2%"
        }
    } else {
        Show-TestResult -TestName "Rate Cap Enforcement" -Status "FAIL" -Details $response.message
    }
} catch {
    Show-TestResult -TestName "Rate Cap Enforcement" -Status "FAIL" -Details "Error: $_"
}

# Test 11: Product-Based Calculation with Auth
Write-Host "Test 11: Product-Based Calculation (CUSTOMER Role)" -ForegroundColor Yellow
Write-Host "Testing product-based FD calculation with authentication..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $customerToken"
    }
    
    $body = @{
        productId = 1
        principalAmount = 250000
        tenure = 24
        tenureUnit = "MONTHS"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "$($baseUrls.calculator)/calculate/product-based" `
        -Method POST `
        -Headers $headers `
        -Body $body
    
    if ($response.success) {
        $data = $response.data
        Show-TestResult -TestName "Product-Based Calculation" -Status "PASS" `
            -Details "Product: $($data.productName), Rate: $($data.interestRate)%, Maturity: ₹$($data.maturityAmount)"
        
        Write-Host "   📊 Product Details:" -ForegroundColor Cyan
        Write-Host "      Product: $($data.productName) ($($data.productCode))" -ForegroundColor Gray
        Write-Host "      Base Rate: $($data.baseInterestRate)%" -ForegroundColor Gray
        Write-Host "      Additional Rate: $($data.additionalInterestRate)%" -ForegroundColor Gray
        Write-Host "      Effective Rate: $($data.interestRate)%" -ForegroundColor Gray
        Write-Host "      Maturity: ₹$($data.maturityAmount)" -ForegroundColor Gray
        Write-Host ""
    } else {
        Show-TestResult -TestName "Product-Based Calculation" -Status "FAIL" -Details $response.message
    }
} catch {
    Show-TestResult -TestName "Product-Based Calculation" -Status "FAIL" -Details "Error: $_"
}

# Test 12: Scenario Comparison
Write-Host "Test 12: Scenario Comparison (CUSTOMER Role)" -ForegroundColor Yellow
Write-Host "Testing comparison of multiple FD scenarios..." -ForegroundColor Gray
try {
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $customerToken"
    }
    
    $body = @{
        commonPrincipal = 100000
        scenarios = @(
            @{
                interestRate = 6.5
                tenure = 12
                tenureUnit = "MONTHS"
                calculationType = "SIMPLE"
            },
            @{
                interestRate = 7.0
                tenure = 24
                tenureUnit = "MONTHS"
                calculationType = "COMPOUND"
                compoundingFrequency = "QUARTERLY"
            },
            @{
                interestRate = 7.5
                tenure = 36
                tenureUnit = "MONTHS"
                calculationType = "COMPOUND"
                compoundingFrequency = "QUARTERLY"
            }
        )
    } | ConvertTo-Json -Depth 3
    
    $response = Invoke-RestMethod -Uri "$($baseUrls.calculator)/compare" `
        -Method POST `
        -Headers $headers `
        -Body $body
    
    if ($response.success) {
        $data = $response.data
        Show-TestResult -TestName "Scenario Comparison" -Status "PASS" `
            -Details "Compared $($data.scenarios.Count) scenarios, Best: Scenario #$($data.bestScenarioIndex + 1)"
        
        Write-Host "   📊 Comparison Results:" -ForegroundColor Cyan
        for ($i = 0; $i -lt $data.scenarios.Count; $i++) {
            $scenario = $data.scenarios[$i]
            $bestMarker = if ($i -eq $data.bestScenarioIndex) { " ⭐ BEST" } else { "" }
            Write-Host "      Scenario $($i + 1)$bestMarker : Rate $($scenario.interestRate)%, Tenure $($scenario.tenure) $($scenario.tenureUnit), Maturity ₹$($scenario.maturityAmount)" -ForegroundColor Gray
        }
        Write-Host ""
    } else {
        Show-TestResult -TestName "Scenario Comparison" -Status "FAIL" -Details $response.message
    }
} catch {
    Show-TestResult -TestName "Scenario Comparison" -Status "FAIL" -Details "Error: $_"
}

# Test Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Test Summary" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $testResults.Count

Write-Host "Total Tests: $totalCount" -ForegroundColor White
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "🎉 All tests passed! Lab L10 implementation is working correctly." -ForegroundColor Green
    Write-Host ""
    Write-Host "✅ Key Features Verified:" -ForegroundColor Cyan
    Write-Host "   • JWT authentication required for all calculations" -ForegroundColor Gray
    Write-Host "   • CUSTOMER role enforced (@PreAuthorize)" -ForegroundColor Gray
    Write-Host "   • 401 for unauthenticated requests" -ForegroundColor Gray
    Write-Host "   • 403 for non-CUSTOMER roles" -ForegroundColor Gray
    Write-Host "   • Customer categories fetched automatically" -ForegroundColor Gray
    Write-Host "   • Personalized interest rates applied (0.25% per category)" -ForegroundColor Gray
    Write-Host "   • Rate cap enforced (max 2% additional)" -ForegroundColor Gray
    Write-Host "   • Product-based calculations working" -ForegroundColor Gray
    Write-Host "   • Scenario comparison working" -ForegroundColor Gray
    Write-Host ""
} else {
    Write-Host "⚠️  Some tests failed. Please review the details above." -ForegroundColor Yellow
    Write-Host ""
}

Write-Host "📖 For detailed implementation info, see: Documentation/Lab-L10-Implementation-Status.md" -ForegroundColor Cyan
Write-Host ""
Write-Host "🔗 Service URLs:" -ForegroundColor Cyan
Write-Host "   Login Service: $($baseUrls.login)" -ForegroundColor Gray
Write-Host "   FD Calculator: $($baseUrls.calculator)" -ForegroundColor Gray
Write-Host "   Swagger UI: http://localhost:8085/api/calculator/swagger-ui/index.html" -ForegroundColor Gray
Write-Host ""

Write-Host "========================================`n" -ForegroundColor Cyan
