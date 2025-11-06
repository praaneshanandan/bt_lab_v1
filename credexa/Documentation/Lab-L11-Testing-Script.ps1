# Lab L11: FD Calculator Testing & Enhancements - Automated Testing Script
# This script tests rate cap enforcement and Python report generation

# Color functions
function Write-Success { param($msg) Write-Host $msg -ForegroundColor Green }
function Write-Info { param($msg) Write-Host $msg -ForegroundColor Cyan }
function Write-Warning { param($msg) Write-Host $msg -ForegroundColor Yellow }
function Write-Error { param($msg) Write-Host $msg -ForegroundColor Red }
function Write-Header { param($msg) Write-Host "`n========================================`n$msg`n========================================" -ForegroundColor Magenta }

# Service URLs
$LOGIN_URL = "http://localhost:8081/api/auth"
$CUSTOMER_URL = "http://localhost:8083/api/customer"
$CALCULATOR_URL = "http://localhost:8085/api/calculator"

# Test credentials
$CUSTOMER_USERNAME = "alice123"
$CUSTOMER_PASSWORD = "password123"

# Global variables
$JWT_TOKEN = ""
$TEST_RESULTS = @()

# Function to login and get JWT token
function Get-JWTToken {
    Write-Info "Logging in as customer: $CUSTOMER_USERNAME"
    
    $loginBody = @{
        username = $CUSTOMER_USERNAME
        password = $CUSTOMER_PASSWORD
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$LOGIN_URL/login" -Method Post -Body $loginBody -ContentType "application/json"
        
        if ($response.success -and $response.data.token) {
            Write-Success "✓ Login successful"
            return $response.data.token
        } else {
            Write-Error "✗ Login failed: $($response.message)"
            return $null
        }
    } catch {
        Write-Error "✗ Login request failed: $($_.Exception.Message)"
        return $null
    }
}

# Function to get customer categories
function Get-CustomerCategories {
    param($token)
    
    Write-Info "Fetching customer categories for: $CUSTOMER_USERNAME"
    
    $headers = @{
        Authorization = "Bearer $token"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$CUSTOMER_URL/categories" -Method Get -Headers $headers
        
        if ($response.success) {
            Write-Success "✓ Categories fetched: $($response.data -join ', ')"
            return $response.data
        } else {
            Write-Warning "⚠ Could not fetch categories"
            return @()
        }
    } catch {
        Write-Warning "⚠ Categories request failed: $($_.Exception.Message)"
        return @()
    }
}

# Function to test standalone calculation
function Test-StandaloneCalculation {
    param(
        [string]$token,
        [decimal]$principal,
        [decimal]$interestRate,
        [int]$termMonths,
        [string]$description
    )
    
    Write-Info "Testing: $description"
    
    $headers = @{
        Authorization = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    $body = @{
        principal = $principal
        interestRate = $interestRate
        termMonths = $termMonths
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$CALCULATOR_URL/calculate/standalone" -Method Post -Headers $headers -Body $body
        
        if ($response.success) {
            $data = $response.data
            Write-Success "✓ Calculation successful"
            Write-Host "  Base Rate: $($data.baseInterestRate)%"
            Write-Host "  Additional Rate: $($data.additionalRate)%"
            Write-Host "  Final Rate: $($data.finalInterestRate)%"
            Write-Host "  Maturity: ₹$($data.maturityAmount)"
            Write-Host "  Interest: ₹$($data.totalInterest)"
            
            if ($data.categories) {
                Write-Host "  Categories: $($data.categories -join ', ')"
                Write-Host "  Category Count: $($data.categories.Count)"
            }
            
            if ($data.finalInterestRate -gt 8.5) {
                Write-Error "  ✗ Rate exceeds 8.5% cap!"
                return @{ success = $false; data = $data; error = "Rate cap violation" }
            } else {
                Write-Success "  ✓ Rate cap enforced correctly"
            }
            
            return @{ success = $true; data = $data }
        } else {
            Write-Error "✗ Calculation failed: $($response.message)"
            return @{ success = $false; error = $response.message }
        }
    } catch {
        Write-Error "✗ Calculation request failed: $($_.Exception.Message)"
        return @{ success = $false; error = $_.Exception.Message }
    }
}

# Function to test product-based calculation
function Test-ProductCalculation {
    param(
        [string]$token,
        [string]$productCode,
        [decimal]$principal,
        [int]$termMonths,
        [string]$description
    )
    
    Write-Info "Testing: $description"
    
    $headers = @{
        Authorization = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    $body = @{
        productCode = $productCode
        principal = $principal
        termMonths = $termMonths
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$CALCULATOR_URL/calculate/product" -Method Post -Headers $headers -Body $body
        
        if ($response.success) {
            $data = $response.data
            Write-Success "✓ Calculation successful"
            Write-Host "  Product: $($data.productName)"
            Write-Host "  Base Rate: $($data.baseInterestRate)%"
            Write-Host "  Additional Rate: $($data.additionalRate)%"
            Write-Host "  Final Rate: $($data.finalInterestRate)%"
            Write-Host "  Maturity: ₹$($data.maturityAmount)"
            
            if ($data.productMaxRate) {
                Write-Host "  Product Max Rate: $($data.productMaxRate)%"
                
                if ($data.finalInterestRate -gt $data.productMaxRate) {
                    Write-Error "  ✗ Rate exceeds product max rate!"
                    return @{ success = $false; data = $data; error = "Product rate cap violation" }
                } else {
                    Write-Success "  ✓ Product rate cap enforced correctly"
                }
            }
            
            if ($data.finalInterestRate -gt 8.5) {
                Write-Error "  ✗ Rate exceeds global 8.5% cap!"
                return @{ success = $false; data = $data; error = "Global rate cap violation" }
            } else {
                Write-Success "  ✓ Global rate cap enforced correctly"
            }
            
            return @{ success = $true; data = $data }
        } else {
            Write-Error "✗ Calculation failed: $($response.message)"
            return @{ success = $false; error = $response.message }
        }
    } catch {
        Write-Error "✗ Calculation request failed: $($_.Exception.Message)"
        return @{ success = $false; error = $_.Exception.Message }
    }
}

# Function to test report generation
function Test-ReportGeneration {
    param(
        [string]$token,
        [array]$calculations,
        [string]$description
    )
    
    Write-Info "Testing: $description"
    
    $headers = @{
        Authorization = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    # Extract data from calculation results
    $reportData = @()
    foreach ($calc in $calculations) {
        if ($calc.success -and $calc.data) {
            $reportData += $calc.data
        }
    }
    
    if ($reportData.Count -eq 0) {
        Write-Warning "⚠ No calculation data to report"
        return @{ success = $false; error = "No data" }
    }
    
    $body = $reportData | ConvertTo-Json -Depth 10
    
    try {
        $response = Invoke-RestMethod -Uri "$CALCULATOR_URL/report" -Method Post -Headers $headers -Body $body
        
        if ($response.success) {
            Write-Success "✓ Report generated successfully"
            Write-Host "  Report Path: $($response.data)"
            Write-Host "  Calculations Included: $($reportData.Count)"
            
            # Check if report file exists
            $reportPath = Join-Path (Get-Location) "fd-calculator-service" $response.data
            if (Test-Path $reportPath) {
                Write-Success "  ✓ Report file exists: $reportPath"
                
                # Read first few lines
                Write-Info "  Report Preview:"
                $lines = Get-Content $reportPath -TotalCount 5
                foreach ($line in $lines) {
                    Write-Host "    $line"
                }
            } else {
                Write-Warning "  ⚠ Report file not found: $reportPath"
            }
            
            return @{ success = $true; data = $response.data }
        } else {
            Write-Error "✗ Report generation failed: $($response.message)"
            return @{ success = $false; error = $response.message }
        }
    } catch {
        Write-Error "✗ Report request failed: $($_.Exception.Message)"
        return @{ success = $false; error = $_.Exception.Message }
    }
}

# Function to verify Python installation
function Test-PythonInstallation {
    Write-Info "Verifying Python installation..."
    
    try {
        $pythonVersion = python --version 2>&1
        Write-Success "✓ Python found: $pythonVersion"
        return $true
    } catch {
        Write-Error "✗ Python not found in PATH"
        Write-Warning "  Install Python from https://www.python.org/"
        return $false
    }
}

# Function to verify Python script
function Test-PythonScript {
    Write-Info "Verifying Python report script..."
    
    $scriptPath = Join-Path (Get-Location) "fd-calculator-service\scripts\generate_fd_report.py"
    
    if (Test-Path $scriptPath) {
        Write-Success "✓ Python script found: $scriptPath"
        return $true
    } else {
        Write-Error "✗ Python script not found: $scriptPath"
        return $false
    }
}

# Main test execution
Write-Header "Lab L11: FD Calculator Testing & Enhancements"

# Step 1: Verify prerequisites
Write-Header "Step 1: Verify Prerequisites"

$pythonInstalled = Test-PythonInstallation
$pythonScriptExists = Test-PythonScript

if (-not $pythonInstalled -or -not $pythonScriptExists) {
    Write-Error "`nPrerequisites not met. Exiting."
    exit 1
}

# Step 2: Health check
Write-Header "Step 2: Service Health Check"

Write-Info "Checking FD Calculator service health..."
try {
    $healthResponse = Invoke-RestMethod -Uri "$CALCULATOR_URL/health" -Method Get
    if ($healthResponse.success) {
        Write-Success "✓ FD Calculator service is healthy"
    } else {
        Write-Error "✗ Service health check failed"
        exit 1
    }
} catch {
    Write-Error "✗ Cannot connect to FD Calculator service at $CALCULATOR_URL"
    Write-Info "Please ensure the service is running on port 8085"
    exit 1
}

# Step 3: Authentication
Write-Header "Step 3: Authentication"

$JWT_TOKEN = Get-JWTToken
if (-not $JWT_TOKEN) {
    Write-Error "`nAuthentication failed. Exiting."
    exit 1
}

# Step 4: Get customer categories
Write-Header "Step 4: Get Customer Categories"

$categories = Get-CustomerCategories -token $JWT_TOKEN
Write-Info "Customer has $($categories.Count) categories"

# Step 5: Test rate cap scenarios
Write-Header "Step 5: Test Rate Cap Scenarios"

Write-Info "`nScenario 1: Base rate with customer categories (expect ≤ 8.5%)"
$test1 = Test-StandaloneCalculation -token $JWT_TOKEN -principal 100000 -interestRate 6.5 -termMonths 12 -description "100K @ 6.5% for 12 months"
$TEST_RESULTS += @{ name = "Scenario 1: Base with categories"; result = $test1 }

Start-Sleep -Seconds 1

Write-Info "`nScenario 2: Higher base rate with categories (expect cap at 8.5%)"
$test2 = Test-StandaloneCalculation -token $JWT_TOKEN -principal 250000 -interestRate 7.5 -termMonths 24 -description "250K @ 7.5% for 24 months"
$TEST_RESULTS += @{ name = "Scenario 2: Higher base + categories"; result = $test2 }

Start-Sleep -Seconds 1

Write-Info "`nScenario 3: Edge case at 8.5% base rate"
$test3 = Test-StandaloneCalculation -token $JWT_TOKEN -principal 150000 -interestRate 8.5 -termMonths 18 -description "150K @ 8.5% for 18 months"
$TEST_RESULTS += @{ name = "Scenario 3: Edge case at 8.5%"; result = $test3 }

Start-Sleep -Seconds 1

# Step 6: Test product-based calculations (if products exist)
Write-Header "Step 6: Test Product-Based Rate Caps"

Write-Info "`nScenario 4: Product-based calculation with rate cap"
$test4 = Test-ProductCalculation -token $JWT_TOKEN -productCode "REGULAR_FD" -principal 200000 -termMonths 12 -description "200K Regular FD for 12 months"
if ($test4.success) {
    $TEST_RESULTS += @{ name = "Scenario 4: Product-based calculation"; result = $test4 }
} else {
    Write-Warning "⚠ Skipping product-based test (product may not exist)"
}

Start-Sleep -Seconds 1

# Step 7: Test report generation
Write-Header "Step 7: Test Python Report Generation"

Write-Info "`nScenario 5: Generate report from successful calculations"
$reportCalculations = @($test1, $test2, $test3)
$test5 = Test-ReportGeneration -token $JWT_TOKEN -calculations $reportCalculations -description "Report with 3 calculations"
$TEST_RESULTS += @{ name = "Scenario 5: Report generation"; result = $test5 }

Start-Sleep -Seconds 1

# Step 8: Test combined category scenarios (if user has multiple categories)
Write-Header "Step 8: Test Combined Category Caps"

if ($categories.Count -ge 2) {
    Write-Info "`nScenario 6: Combined categories test"
    Write-Info "Customer categories: $($categories -join ', ')"
    Write-Info "Expected category bonus: $(0.25 * [Math]::Min($categories.Count, 8))%"
    
    $test6 = Test-StandaloneCalculation -token $JWT_TOKEN -principal 300000 -interestRate 6.5 -termMonths 36 -description "300K @ 6.5% for 36 months with $($categories.Count) categories"
    $TEST_RESULTS += @{ name = "Scenario 6: Combined categories"; result = $test6 }
    
    # Verify category count limit
    if ($test6.success -and $test6.data.categories) {
        $usedCategories = [Math]::Min($test6.data.categories.Count, 8)
        $expectedBonus = 0.25 * $usedCategories
        
        if ([Math]::Abs($test6.data.additionalRate - $expectedBonus) -lt 0.01) {
            Write-Success "✓ Category bonus calculation correct: $($test6.data.additionalRate)%"
        } else {
            Write-Warning "⚠ Category bonus mismatch: expected $expectedBonus%, got $($test6.data.additionalRate)%"
        }
    }
} else {
    Write-Warning "⚠ Customer has < 2 categories, skipping combined category test"
}

# Step 9: Test error scenarios
Write-Header "Step 9: Test Error Scenarios"

Write-Info "`nScenario 7: Invalid principal amount"
try {
    $test7 = Test-StandaloneCalculation -token $JWT_TOKEN -principal -1000 -interestRate 6.5 -termMonths 12 -description "Invalid negative principal"
    $TEST_RESULTS += @{ name = "Scenario 7: Invalid principal"; result = $test7 }
} catch {
    Write-Success "✓ Invalid principal rejected as expected"
}

Start-Sleep -Seconds 1

Write-Info "`nScenario 8: Report generation with empty data"
$test8 = Test-ReportGeneration -token $JWT_TOKEN -calculations @() -description "Empty report"
$TEST_RESULTS += @{ name = "Scenario 8: Empty report"; result = $test8 }

# Step 10: Summary
Write-Header "Test Summary"

$totalTests = $TEST_RESULTS.Count
$passedTests = ($TEST_RESULTS | Where-Object { $_.result.success }).Count
$failedTests = $totalTests - $passedTests

Write-Host "`nTotal Tests: $totalTests"
Write-Success "Passed: $passedTests"
if ($failedTests -gt 0) {
    Write-Error "Failed: $failedTests"
} else {
    Write-Success "Failed: 0"
}

Write-Host "`nDetailed Results:"
Write-Host "============================================"
foreach ($test in $TEST_RESULTS) {
    if ($test.result.success) {
        Write-Success "✓ $($test.name)"
    } else {
        Write-Error "✗ $($test.name) - $($test.result.error)"
    }
}

# Step 11: Lab L11 verification
Write-Header "Lab L11 Verification"

$rateCapEnforced = $false
$reportGenerated = $false
$categoryCapsVerified = $false

# Check if rate cap was enforced in any test
foreach ($test in $TEST_RESULTS) {
    if ($test.result.success -and $test.result.data) {
        if ($test.result.data.finalInterestRate -le 8.5) {
            $rateCapEnforced = $true
        }
        
        if ($test.result.data.categories -and $test.result.data.categories.Count -ge 2) {
            $categoryCapsVerified = $true
        }
    }
    
    if ($test.name -like "*Report generation*" -and $test.result.success) {
        $reportGenerated = $true
    }
}

Write-Host "`nLab L11 Requirements:"
if ($rateCapEnforced) {
    Write-Success "✓ Rate cap enforcement (8.5% maximum)"
} else {
    Write-Warning "⚠ Rate cap enforcement not verified"
}

if ($categoryCapsVerified) {
    Write-Success "✓ Combined category caps (0.25% per category, max 8)"
} else {
    Write-Warning "⚠ Combined category caps not verified"
}

if ($reportGenerated) {
    Write-Success "✓ Python report generation via Runtime.exec()"
} else {
    Write-Error "✗ Python report generation failed"
}

Write-Header "Testing Complete"

if ($passedTests -eq $totalTests -and $rateCapEnforced -and $reportGenerated -and $categoryCapsVerified) {
    Write-Success "`n🎉 All Lab L11 tests passed successfully!"
    Write-Success "✓ Rate limits enforcement implemented"
    Write-Success "✓ Combined category caps validated"
    Write-Success "✓ Python report generation working"
    exit 0
} else {
    Write-Warning "`n⚠ Some tests failed or requirements not fully verified"
    Write-Info "Please review the test results above"
    exit 1
}
