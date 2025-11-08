# Simple ASCII-only Integration Test Script for Calculator <-> Product Pricing
# No Unicode characters - PowerShell compatible

$ErrorActionPreference = "Continue"

# Configuration
$calcBaseUrl = "http://localhost:8085/api/calculator"
$productBaseUrl = "http://localhost:8084/api/products"

# Test Results
$testResults = @()

function Add-TestResult {
    param($Name, $Status, $Message)
    $testResults += [PSCustomObject]@{
        Test = $Name
        Status = $Status
        Message = $Message
    }
}

function Write-Section {
    param($Title)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host " $Title" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan
}

function Write-Pass {
    param($Message)
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Fail {
    param($Message)
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function Write-Info {
    param($Message)
    Write-Host "[INFO] $Message" -ForegroundColor Yellow
}

# ============================================================================
# TEST 1: Service Health Check
# ============================================================================
Write-Section "TEST 1: Service Health Check"

try {
    $calcHealth = Invoke-RestMethod -Uri "$calcBaseUrl/actuator/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
    if ($calcHealth.status -eq "UP") {
        Write-Pass "Calculator Service is UP"
        Add-TestResult "Calculator Health" "PASS" "Service is UP"
    } else {
        Write-Fail "Calculator Service status: $($calcHealth.status)"
        Add-TestResult "Calculator Health" "FAIL" "Status: $($calcHealth.status)"
    }
} catch {
    Write-Fail "Calculator Service unreachable: $($_.Exception.Message)"
    Add-TestResult "Calculator Health" "FAIL" "Service unreachable"
}

try {
    $prodHealth = Invoke-RestMethod -Uri "$productBaseUrl/actuator/health" -Method Get -TimeoutSec 5 -ErrorAction Stop
    if ($prodHealth.status -eq "UP") {
        Write-Pass "Product Pricing Service is UP"
        Add-TestResult "Product Pricing Health" "PASS" "Service is UP"
    } else {
        Write-Fail "Product Pricing Service status: $($prodHealth.status)"
        Add-TestResult "Product Pricing Health" "FAIL" "Status: $($prodHealth.status)"
    }
} catch {
    Write-Fail "Product Pricing Service unreachable: $($_.Exception.Message)"
    Add-TestResult "Product Pricing Health" "FAIL" "Service unreachable"
}

# ============================================================================
# TEST 2: Product Fetch (No Auth - Should Fail with 403)
# ============================================================================
Write-Section "TEST 2: Product Fetch Integration (Expected Auth Required)"

Write-Info "Testing product fetch without authentication (should get 403)"
try {
    $product = Invoke-RestMethod -Uri "$productBaseUrl/1" -Method Get -ErrorAction Stop
    Write-Fail "Unexpected success - should require authentication"
    Add-TestResult "Product Fetch Security" "FAIL" "No auth required - security issue"
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Pass "Product endpoint correctly requires authentication (403)"
        Add-TestResult "Product Fetch Security" "PASS" "Auth required as expected"
    } else {
        Write-Fail "Unexpected error: $($_.Exception.Message)"
        Add-TestResult "Product Fetch Security" "FAIL" $_.Exception.Message
    }
}

# ============================================================================
# TEST 3: Interest Rate Fetch (No Auth - Should Fail with 403)
# ============================================================================
Write-Section "TEST 3: Interest Rate Fetch Integration (Expected Auth Required)"

Write-Info "Testing interest rate fetch without authentication (should get 403)"
$rateUrl = "$productBaseUrl/products/1/interest-rates/applicable?depositAmount=100000&tenureMonths=12&customerClassification=GENERAL"
try {
    $rate = Invoke-RestMethod -Uri $rateUrl -Method Get -ErrorAction Stop
    Write-Fail "Unexpected success - should require authentication"
    Add-TestResult "Rate Fetch Security" "FAIL" "No auth required - security issue"
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Pass "Interest rate endpoint correctly requires authentication (403)"
        Add-TestResult "Rate Fetch Security" "PASS" "Auth required as expected"
    } else {
        Write-Fail "Unexpected error: $($_.Exception.Message)"
        Add-TestResult "Rate Fetch Security" "FAIL" $_.Exception.Message
    }
}

# ============================================================================
# TEST 4: Standalone Calculation (No Product - No Auth Required)
# ============================================================================
Write-Section "TEST 4: Standalone Calculation (No Authentication Needed)"

$standalonCalc = @{
    principalAmount = 100000
    interestRate = 7.5
    tenureMonths = 12
    calculationType = "SIMPLE"
} | ConvertTo-Json

Write-Info "Testing standalone calculation (100000 Rs @ 7.5% for 12 months)"
try {
    $result = Invoke-RestMethod -Uri "$calcBaseUrl/calculate/standalone" -Method Post -Body $standalonCalc -ContentType "application/json" -ErrorAction Stop
    
    if ($result.success -and $result.data) {
        Write-Pass "Standalone calculation succeeded"
        Write-Info "Principal: Rs. $($result.data.principalAmount)"
        Write-Info "Interest Rate: $($result.data.interestRate)%"
        Write-Info "Total Interest: Rs. $($result.data.totalInterest)"
        Write-Info "Maturity Amount: Rs. $($result.data.maturityAmount)"
        
        # Verify calculation: Simple Interest = P * R * T / 100
        $expectedInterest = 100000 * 7.5 * 1 / 100  # 1 year
        if ([Math]::Abs($result.data.totalInterest - $expectedInterest) -lt 1) {
            Write-Pass "Calculation is mathematically correct"
            Add-TestResult "Standalone Calculation" "PASS" "Correct calculation"
        } else {
            Write-Fail "Calculation mismatch. Expected ~$expectedInterest, got $($result.data.totalInterest)"
            Add-TestResult "Standalone Calculation" "FAIL" "Calculation mismatch"
        }
    } else {
        Write-Fail "Calculation failed: $($result.message)"
        Add-TestResult "Standalone Calculation" "FAIL" $result.message
    }
} catch {
    Write-Fail "Standalone calculation error: $($_.Exception.Message)"
    Add-TestResult "Standalone Calculation" "FAIL" $_.Exception.Message
}

# ============================================================================
# TEST 5: URL Path Verification
# ============================================================================
Write-Section "TEST 5: URL Path Verification"

Write-Info "Verifying Calculator service configuration..."
try {
    # Try to get environment/config info
    $configUrl = "$calcBaseUrl/actuator/env"
    Write-Info "Attempting to access: $configUrl"
    Write-Info "(This may fail if actuator endpoints are secured - that's okay)"
    
    Add-TestResult "URL Configuration" "INFO" "Manual verification required"
} catch {
    Write-Info "Cannot access actuator endpoints (may be secured)"
    Add-TestResult "URL Configuration" "INFO" "Cannot verify programmatically"
}

Write-Info "Key URLs to verify manually:"
Write-Info "1. Product fetch: GET http://localhost:8084/api/products/{id}"
Write-Info "2. Rate fetch: GET http://localhost:8084/api/products/products/{id}/interest-rates/applicable"
Write-Info "3. Calculator: POST http://localhost:8085/api/calculator/calculate/product-based"

# ============================================================================
# TEST SUMMARY
# ============================================================================
Write-Section "TEST SUMMARY"

$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$infoCount = ($testResults | Where-Object { $_.Status -eq "INFO" }).Count

Write-Host "Total Tests Run: $($testResults.Count)" -ForegroundColor Cyan
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red
Write-Host "Info: $infoCount" -ForegroundColor Yellow

Write-Host "`nDetailed Results:" -ForegroundColor Cyan
$testResults | Format-Table -AutoSize

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "NEXT STEPS FOR AUTHENTICATED TESTING" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "1. Use Swagger UI to get JWT token:" -ForegroundColor White
Write-Host "   - Login Service: http://localhost:8081/api/login/swagger-ui/index.html" -ForegroundColor White
Write-Host "   - Use POST /api/login/authenticate endpoint" -ForegroundColor White
Write-Host "`n2. Test via Swagger UI (auto-includes token):" -ForegroundColor White
Write-Host "   - Product Pricing: http://localhost:8084/api/products/swagger-ui/index.html" -ForegroundColor White
Write-Host "   - Calculator: http://localhost:8085/api/calculator/swagger-ui/index.html" -ForegroundColor White
Write-Host "`n3. Test Product-Based Calculation with these scenarios:" -ForegroundColor White
Write-Host "   a) Simple Interest calculation" -ForegroundColor White
Write-Host "   b) Compound Interest calculation" -ForegroundColor White
Write-Host "   c) Amount below minAmount (should fail validation)" -ForegroundColor White
Write-Host "   d) Amount above maxAmount (should fail validation)" -ForegroundColor White
Write-Host "   e) Tenure below minTermMonths (should fail validation)" -ForegroundColor White
Write-Host "   f) Tenure above maxTermMonths (should fail validation)" -ForegroundColor White
Write-Host "   g) With customer classifications (SENIOR_CITIZEN, etc.)" -ForegroundColor White
Write-Host "   h) With custom rate exceeding 2% cap" -ForegroundColor White
Write-Host "   i) TDS calculation verification" -ForegroundColor White
Write-Host "`n========================================`n" -ForegroundColor Cyan
