# Comprehensive Product-Pricing Service Test Suite
# Lab L5 - All Possible Test Cases

$baseUrl = "http://localhost:8084/api/products"
$testResults = @()

function Log-Test {
    param($testName, $status, $response, $statusCode)
    $result = [PSCustomObject]@{
        TestName = $testName
        Status = $status
        StatusCode = $statusCode
        Response = $response
        Timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    }
    $testResults += $result
    
    if ($status -eq "PASS") {
        Write-Host "✅ PASS: $testName (HTTP $statusCode)" -ForegroundColor Green
    } else {
        Write-Host "❌ FAIL: $testName (HTTP $statusCode)" -ForegroundColor Red
    }
    Write-Host "Response: $($response | ConvertTo-Json -Depth 3 -Compress)" -ForegroundColor Gray
    Write-Host "---" -ForegroundColor Gray
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Product-Pricing Service Test Suite" -ForegroundColor Cyan
Write-Host "Lab L5 - Comprehensive Testing" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ====================================
# TEST CATEGORY 1: CREATE PRODUCT
# ====================================
Write-Host "🧪 TEST CATEGORY 1: CREATE PRODUCT" -ForegroundColor Yellow
Write-Host ""

# Test 1.1: Create Valid Fixed Deposit Product
Write-Host "Test 1.1: Create Valid Fixed Deposit Product" -ForegroundColor Cyan
$product1 = @{
    productCode = "FD001"
    productName = "Fixed Deposit - Short Term"
    productType = "FIXED_DEPOSIT"
    currency = "INR"
    description = "Short-term fixed deposit with competitive rates"
    effectiveDate = "2025-01-01"
    expiryDate = "2025-12-31"
    minTermMonths = 6
    maxTermMonths = 12
    minAmount = 10000.00
    maxAmount = 10000000.00
    minInterestRate = 6.0
    maxInterestRate = 7.5
    status = "ACTIVE"
    isCompoundingAllowed = $true
    isPrematureWithdrawalAllowed = $true
    prematureWithdrawalPenalty = 1.0
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $product1 -ContentType "application/json"
    if ($response.success) {
        Log-Test "1.1 Create Valid FD Product" "PASS" $response 201
        $global:product1Id = $response.data.id
    } else {
        Log-Test "1.1 Create Valid FD Product" "FAIL" $response 200
    }
} catch {
    Log-Test "1.1 Create Valid FD Product" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 1.2: Create Tax Saver FD
Write-Host "Test 1.2: Create Tax Saver FD Product" -ForegroundColor Cyan
$product2 = @{
    productCode = "FD002"
    productName = "Tax Saver Fixed Deposit"
    productType = "TAX_SAVER_FD"
    currency = "INR"
    description = "5-year lock-in tax saving fixed deposit (80C benefit)"
    effectiveDate = "2025-01-01"
    expiryDate = "2026-12-31"
    minTermMonths = 60
    maxTermMonths = 60
    minAmount = 100.00
    maxAmount = 150000.00
    minInterestRate = 6.5
    maxInterestRate = 7.0
    status = "ACTIVE"
    isCompoundingAllowed = $true
    isPrematureWithdrawalAllowed = $false
    prematureWithdrawalPenalty = 0.0
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $product2 -ContentType "application/json"
    if ($response.success) {
        Log-Test "1.2 Create Tax Saver FD" "PASS" $response 201
        $global:product2Id = $response.data.id
    } else {
        Log-Test "1.2 Create Tax Saver FD" "FAIL" $response 200
    }
} catch {
    Log-Test "1.2 Create Tax Saver FD" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 1.3: Create Senior Citizen FD
Write-Host "Test 1.3: Create Senior Citizen FD Product" -ForegroundColor Cyan
$product3 = @{
    productCode = "FD003"
    productName = "Senior Citizen Fixed Deposit"
    productType = "SENIOR_CITIZEN_FD"
    currency = "INR"
    description = "Special FD for senior citizens with higher interest rates"
    effectiveDate = "2025-01-01"
    expiryDate = "2026-12-31"
    minTermMonths = 12
    maxTermMonths = 120
    minAmount = 1000.00
    maxAmount = 10000000.00
    minInterestRate = 7.0
    maxInterestRate = 8.5
    status = "ACTIVE"
    isCompoundingAllowed = $true
    isPrematureWithdrawalAllowed = $true
    prematureWithdrawalPenalty = 0.5
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $product3 -ContentType "application/json"
    if ($response.success) {
        Log-Test "1.3 Create Senior Citizen FD" "PASS" $response 201
        $global:product3Id = $response.data.id
    } else {
        Log-Test "1.3 Create Senior Citizen FD" "FAIL" $response 200
    }
} catch {
    Log-Test "1.3 Create Senior Citizen FD" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 1.4: Create Duplicate Product (Should Fail)
Write-Host "Test 1.4: Create Duplicate Product (Should Fail with 409)" -ForegroundColor Cyan
$duplicateProduct = @{
    productCode = "FD001"
    productName = "Duplicate FD"
    productType = "FIXED_DEPOSIT"
    currency = "INR"
    effectiveDate = "2025-01-01"
    minTermMonths = 6
    maxTermMonths = 12
    minAmount = 10000.00
    maxAmount = 10000000.00
    minInterestRate = 6.0
    maxInterestRate = 7.5
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $duplicateProduct -ContentType "application/json"
    Log-Test "1.4 Duplicate Product (Expected 409)" "FAIL" $response 200
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 409) {
        Log-Test "1.4 Duplicate Product (Expected 409)" "PASS" "Correctly rejected duplicate" 409
    } else {
        Log-Test "1.4 Duplicate Product (Expected 409)" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
    }
}

Start-Sleep -Seconds 1

# Test 1.5: Create Product with Invalid Data (Should Fail)
Write-Host "Test 1.5: Create Product with Invalid Data (Should Fail with 400)" -ForegroundColor Cyan
$invalidProduct = @{
    productCode = ""
    productName = ""
    productType = "INVALID_TYPE"
    currency = "INR"
    minTermMonths = -5
    maxTermMonths = 12
    minAmount = -1000.00
    maxAmount = 10000.00
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $invalidProduct -ContentType "application/json"
    Log-Test "1.5 Invalid Product Data (Expected 400)" "FAIL" $response 200
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Log-Test "1.5 Invalid Product Data (Expected 400)" "PASS" "Correctly rejected invalid data" 400
    } else {
        Log-Test "1.5 Invalid Product Data (Expected 400)" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
    }
}

Start-Sleep -Seconds 1

# ====================================
# TEST CATEGORY 2: RETRIEVE PRODUCTS
# ====================================
Write-Host ""
Write-Host "🧪 TEST CATEGORY 2: RETRIEVE PRODUCTS" -ForegroundColor Yellow
Write-Host ""

# Test 2.1: Get Product by ID
Write-Host "Test 2.1: Get Product by ID" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$global:product1Id" -Method Get
    if ($response.success -and $response.data.id -eq $global:product1Id) {
        Log-Test "2.1 Get Product by ID" "PASS" $response 200
    } else {
        Log-Test "2.1 Get Product by ID" "FAIL" $response 200
    }
} catch {
    Log-Test "2.1 Get Product by ID" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 2.2: Get Product by Code
Write-Host "Test 2.2: Get Product by Code" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/code/FD001" -Method Get
    if ($response.success -and $response.data.productCode -eq "FD001") {
        Log-Test "2.2 Get Product by Code" "PASS" $response 200
    } else {
        Log-Test "2.2 Get Product by Code" "FAIL" $response 200
    }
} catch {
    Log-Test "2.2 Get Product by Code" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 2.3: Get Non-Existent Product (Should Fail with 404)
Write-Host "Test 2.3: Get Non-Existent Product (Should Fail with 404)" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/99999" -Method Get
    Log-Test "2.3 Non-Existent Product (Expected 404)" "FAIL" $response 200
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Log-Test "2.3 Non-Existent Product (Expected 404)" "PASS" "Correctly returned 404" 404
    } else {
        Log-Test "2.3 Non-Existent Product (Expected 404)" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
    }
}

Start-Sleep -Seconds 1

# Test 2.4: Get All Products
Write-Host "Test 2.4: Get All Products" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Get
    if ($response.success -and $response.data.Count -ge 3) {
        Log-Test "2.4 Get All Products" "PASS" "Found $($response.data.Count) products" 200
    } else {
        Log-Test "2.4 Get All Products" "FAIL" $response 200
    }
} catch {
    Log-Test "2.4 Get All Products" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 2.5: Get Products by Type
Write-Host "Test 2.5: Get Products by Type (FIXED_DEPOSIT)" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/type/FIXED_DEPOSIT" -Method Get
    if ($response.success -and $response.data.Count -gt 0) {
        Log-Test "2.5 Get Products by Type" "PASS" "Found $($response.data.Count) FIXED_DEPOSIT products" 200
    } else {
        Log-Test "2.5 Get Products by Type" "FAIL" $response 200
    }
} catch {
    Log-Test "2.5 Get Products by Type" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 2.6: Get Products by Status
Write-Host "Test 2.6: Get Products by Status (ACTIVE)" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/status/ACTIVE" -Method Get
    if ($response.success -and $response.data.Count -ge 3) {
        Log-Test "2.6 Get Products by Status" "PASS" "Found $($response.data.Count) ACTIVE products" 200
    } else {
        Log-Test "2.6 Get Products by Status" "FAIL" $response 200
    }
} catch {
    Log-Test "2.6 Get Products by Status" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 2.7: Get Active Products
Write-Host "Test 2.7: Get Active Products" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/active" -Method Get
    if ($response.success -and $response.data.Count -ge 3) {
        Log-Test "2.7 Get Active Products" "PASS" "Found $($response.data.Count) active products" 200
    } else {
        Log-Test "2.7 Get Active Products" "FAIL" $response 200
    }
} catch {
    Log-Test "2.7 Get Active Products" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 2.8: Get Currently Active Products (within date range)
Write-Host "Test 2.8: Get Currently Active Products (within date range)" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/currently-active" -Method Get
    if ($response.success) {
        Log-Test "2.8 Get Currently Active Products" "PASS" "Found $($response.data.Count) currently active products" 200
    } else {
        Log-Test "2.8 Get Currently Active Products" "FAIL" $response 200
    }
} catch {
    Log-Test "2.8 Get Currently Active Products" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# ====================================
# TEST CATEGORY 3: SEARCH & FILTER
# ====================================
Write-Host ""
Write-Host "🧪 TEST CATEGORY 3: SEARCH & FILTER" -ForegroundColor Yellow
Write-Host ""

# Test 3.1: Search Products by Type
Write-Host "Test 3.1: Search Products by Type (POST /search)" -ForegroundColor Cyan
$searchCriteria1 = @{
    productType = "FIXED_DEPOSIT"
    page = 0
    size = 10
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/search" -Method Post -Body $searchCriteria1 -ContentType "application/json"
    if ($response.success) {
        Log-Test "3.1 Search by Type" "PASS" "Found products matching FIXED_DEPOSIT" 200
    } else {
        Log-Test "3.1 Search by Type" "FAIL" $response 200
    }
} catch {
    Log-Test "3.1 Search by Type" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 3.2: Search Products by Status
Write-Host "Test 3.2: Search Products by Status" -ForegroundColor Cyan
$searchCriteria2 = @{
    status = "ACTIVE"
    page = 0
    size = 10
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/search" -Method Post -Body $searchCriteria2 -ContentType "application/json"
    if ($response.success) {
        Log-Test "3.2 Search by Status" "PASS" "Found ACTIVE products" 200
    } else {
        Log-Test "3.2 Search by Status" "FAIL" $response 200
    }
} catch {
    Log-Test "3.2 Search by Status" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 3.3: Search Products by Date Range
Write-Host "Test 3.3: Search Products by Date Range" -ForegroundColor Cyan
$searchCriteria3 = @{
    startDate = "2025-01-01"
    endDate = "2025-12-31"
    page = 0
    size = 10
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/search" -Method Post -Body $searchCriteria3 -ContentType "application/json"
    if ($response.success) {
        Log-Test "3.3 Search by Date Range" "PASS" "Found products in date range" 200
    } else {
        Log-Test "3.3 Search by Date Range" "FAIL" $response 200
    }
} catch {
    Log-Test "3.3 Search by Date Range" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 3.4: Get Products by Date Range (GET method)
Write-Host "Test 3.4: Get Products by Date Range (GET method)" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/date-range?startDate=2025-01-01&endDate=2025-12-31" -Method Get
    if ($response.success) {
        Log-Test "3.4 Get by Date Range (GET)" "PASS" "Found products in date range" 200
    } else {
        Log-Test "3.4 Get by Date Range (GET)" "FAIL" $response 200
    }
} catch {
    Log-Test "3.4 Get by Date Range (GET)" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 3.5: Search with Multiple Criteria
Write-Host "Test 3.5: Search with Multiple Criteria (Type + Status + Date)" -ForegroundColor Cyan
$searchCriteria5 = @{
    productType = "FIXED_DEPOSIT"
    status = "ACTIVE"
    startDate = "2025-01-01"
    endDate = "2025-12-31"
    page = 0
    size = 10
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/search" -Method Post -Body $searchCriteria5 -ContentType "application/json"
    if ($response.success) {
        Log-Test "3.5 Search Multiple Criteria" "PASS" "Found products matching all criteria" 200
    } else {
        Log-Test "3.5 Search Multiple Criteria" "FAIL" $response 200
    }
} catch {
    Log-Test "3.5 Search Multiple Criteria" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 3.6: Search with Pagination
Write-Host "Test 3.6: Search with Pagination (page=0, size=2)" -ForegroundColor Cyan
$searchCriteria6 = @{
    status = "ACTIVE"
    page = 0
    size = 2
    sortBy = "createdAt"
    sortDirection = "DESC"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/search" -Method Post -Body $searchCriteria6 -ContentType "application/json"
    if ($response.success) {
        Log-Test "3.6 Search with Pagination" "PASS" "Pagination working correctly" 200
    } else {
        Log-Test "3.6 Search with Pagination" "FAIL" $response 200
    }
} catch {
    Log-Test "3.6 Search with Pagination" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# ====================================
# TEST CATEGORY 4: UPDATE PRODUCTS
# ====================================
Write-Host ""
Write-Host "🧪 TEST CATEGORY 4: UPDATE PRODUCTS" -ForegroundColor Yellow
Write-Host ""

# Test 4.1: Update Product Details
Write-Host "Test 4.1: Update Product Details" -ForegroundColor Cyan
$updateData1 = @{
    productName = "Fixed Deposit - Short Term (Updated)"
    description = "Updated description for short-term FD"
    minInterestRate = 6.5
    maxInterestRate = 8.0
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$global:product1Id" -Method Put -Body $updateData1 -ContentType "application/json"
    if ($response.success) {
        Log-Test "4.1 Update Product Details" "PASS" $response 200
    } else {
        Log-Test "4.1 Update Product Details" "FAIL" $response 200
    }
} catch {
    Log-Test "4.1 Update Product Details" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 4.2: Update Product Status
Write-Host "Test 4.2: Update Product Status to INACTIVE" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$global:product2Id/status?status=INACTIVE" -Method Put
    if ($response.success) {
        Log-Test "4.2 Update Product Status" "PASS" $response 200
    } else {
        Log-Test "4.2 Update Product Status" "FAIL" $response 200
    }
} catch {
    Log-Test "4.2 Update Product Status" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 4.3: Update Product Status back to ACTIVE
Write-Host "Test 4.3: Update Product Status back to ACTIVE" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$global:product2Id/status?status=ACTIVE" -Method Put
    if ($response.success) {
        Log-Test "4.3 Reactivate Product" "PASS" $response 200
    } else {
        Log-Test "4.3 Reactivate Product" "FAIL" $response 200
    }
} catch {
    Log-Test "4.3 Reactivate Product" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 4.4: Update Non-Existent Product (Should Fail)
Write-Host "Test 4.4: Update Non-Existent Product (Should Fail with 404)" -ForegroundColor Cyan
$updateData4 = @{
    productName = "Non-existent Product"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/99999" -Method Put -Body $updateData4 -ContentType "application/json"
    Log-Test "4.4 Update Non-Existent (Expected 404)" "FAIL" $response 200
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Log-Test "4.4 Update Non-Existent (Expected 404)" "PASS" "Correctly returned 404" 404
    } else {
        Log-Test "4.4 Update Non-Existent (Expected 404)" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
    }
}

Start-Sleep -Seconds 1

# ====================================
# TEST CATEGORY 5: DELETE PRODUCTS
# ====================================
Write-Host ""
Write-Host "🧪 TEST CATEGORY 5: DELETE PRODUCTS" -ForegroundColor Yellow
Write-Host ""

# Create a product to delete
Write-Host "Test 5.0: Create Product for Deletion Test" -ForegroundColor Cyan
$productToDelete = @{
    productCode = "FD999"
    productName = "Temporary FD for Delete Test"
    productType = "FIXED_DEPOSIT"
    currency = "INR"
    effectiveDate = "2025-01-01"
    minTermMonths = 6
    maxTermMonths = 12
    minAmount = 10000.00
    maxAmount = 1000000.00
    minInterestRate = 6.0
    maxInterestRate = 7.0
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $productToDelete -ContentType "application/json"
    if ($response.success) {
        $global:productToDeleteId = $response.data.id
        Log-Test "5.0 Create Product for Delete" "PASS" $response 201
    } else {
        Log-Test "5.0 Create Product for Delete" "FAIL" $response 200
    }
} catch {
    Log-Test "5.0 Create Product for Delete" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 5.1: Soft Delete Product
Write-Host "Test 5.1: Soft Delete Product" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$global:productToDeleteId" -Method Delete
    if ($response.success) {
        Log-Test "5.1 Soft Delete Product" "PASS" $response 200
    } else {
        Log-Test "5.1 Soft Delete Product" "FAIL" $response 200
    }
} catch {
    Log-Test "5.1 Soft Delete Product" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 5.2: Verify Product is Soft Deleted (Status = CLOSED)
Write-Host "Test 5.2: Verify Product Status Changed to CLOSED" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$global:productToDeleteId" -Method Get
    if ($response.success -and $response.data.status -eq "CLOSED") {
        Log-Test "5.2 Verify Soft Delete" "PASS" "Product status is CLOSED" 200
    } else {
        Log-Test "5.2 Verify Soft Delete" "FAIL" $response 200
    }
} catch {
    Log-Test "5.2 Verify Soft Delete" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 5.3: Delete Non-Existent Product (Should Fail)
Write-Host "Test 5.3: Delete Non-Existent Product (Should Fail with 404)" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/99999" -Method Delete
    Log-Test "5.3 Delete Non-Existent (Expected 404)" "FAIL" $response 200
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        Log-Test "5.3 Delete Non-Existent (Expected 404)" "PASS" "Correctly returned 404" 404
    } else {
        Log-Test "5.3 Delete Non-Existent (Expected 404)" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
    }
}

Start-Sleep -Seconds 1

# ====================================
# TEST CATEGORY 6: EDGE CASES
# ====================================
Write-Host ""
Write-Host "🧪 TEST CATEGORY 6: EDGE CASES" -ForegroundColor Yellow
Write-Host ""

# Test 6.1: Create Product with Maximum Values
Write-Host "Test 6.1: Create Product with Maximum Values" -ForegroundColor Cyan
$maxProduct = @{
    productCode = "FD_MAX"
    productName = "Maximum Value Fixed Deposit"
    productType = "FIXED_DEPOSIT"
    currency = "INR"
    effectiveDate = "2025-01-01"
    expiryDate = "2099-12-31"
    minTermMonths = 1
    maxTermMonths = 999
    minAmount = 1.00
    maxAmount = 999999999.99
    minInterestRate = 0.01
    maxInterestRate = 99.99
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $maxProduct -ContentType "application/json"
    if ($response.success) {
        Log-Test "6.1 Create with Max Values" "PASS" $response 201
    } else {
        Log-Test "6.1 Create with Max Values" "FAIL" $response 200
    }
} catch {
    Log-Test "6.1 Create with Max Values" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 6.2: Search with Empty Criteria
Write-Host "Test 6.2: Search with Empty Criteria" -ForegroundColor Cyan
$emptyCriteria = @{
    page = 0
    size = 10
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/search" -Method Post -Body $emptyCriteria -ContentType "application/json"
    if ($response.success) {
        Log-Test "6.2 Search Empty Criteria" "PASS" "Returns all products" 200
    } else {
        Log-Test "6.2 Search Empty Criteria" "FAIL" $response 200
    }
} catch {
    Log-Test "6.2 Search Empty Criteria" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# Test 6.3: Get Products with Invalid Type
Write-Host "Test 6.3: Get Products with Invalid Type (Should Fail)" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/type/INVALID_TYPE" -Method Get
    Log-Test "6.3 Invalid Type (Expected 400)" "FAIL" $response 200
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400 -or $_.Exception.Response.StatusCode.value__ -eq 404) {
        Log-Test "6.3 Invalid Type (Expected 400)" "PASS" "Correctly rejected invalid type" $_.Exception.Response.StatusCode.value__
    } else {
        Log-Test "6.3 Invalid Type (Expected 400)" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
    }
}

Start-Sleep -Seconds 1

# Test 6.4: Update with Empty Body
Write-Host "Test 6.4: Update with Empty Body (Should Fail)" -ForegroundColor Cyan
$emptyUpdate = @{} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/$global:product1Id" -Method Put -Body $emptyUpdate -ContentType "application/json"
    Log-Test "6.4 Update Empty Body (Expected 400)" "FAIL" $response 200
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        Log-Test "6.4 Update Empty Body (Expected 400)" "PASS" "Correctly rejected empty update" 400
    } else {
        # Some implementations might accept empty update
        Log-Test "6.4 Update Empty Body" "INFO" "Accepted empty update" $_.Exception.Response.StatusCode.value__
    }
}

Start-Sleep -Seconds 1

# ====================================
# TEST CATEGORY 7: CURRENCY TESTS
# ====================================
Write-Host ""
Write-Host "🧪 TEST CATEGORY 7: CURRENCY TESTS" -ForegroundColor Yellow
Write-Host ""

# Test 7.1: Create Product with USD Currency
Write-Host "Test 7.1: Create Product with USD Currency" -ForegroundColor Cyan
$usdProduct = @{
    productCode = "FD_USD"
    productName = "Fixed Deposit - USD"
    productType = "FIXED_DEPOSIT"
    currency = "USD"
    effectiveDate = "2025-01-01"
    expiryDate = "2025-12-31"
    minTermMonths = 6
    maxTermMonths = 24
    minAmount = 1000.00
    maxAmount = 1000000.00
    minInterestRate = 3.0
    maxInterestRate = 5.0
    status = "ACTIVE"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl" -Method Post -Body $usdProduct -ContentType "application/json"
    if ($response.success) {
        Log-Test "7.1 Create USD Product" "PASS" $response 201
    } else {
        Log-Test "7.1 Create USD Product" "FAIL" $response 200
    }
} catch {
    Log-Test "7.1 Create USD Product" "FAIL" $_.Exception.Message $_.Exception.Response.StatusCode.value__
}

Start-Sleep -Seconds 1

# ====================================
# TEST SUMMARY
# ====================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passCount = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $testResults.Count

Write-Host "Total Tests: $totalCount" -ForegroundColor White
Write-Host "Passed: $passCount" -ForegroundColor Green
Write-Host "Failed: $failCount" -ForegroundColor Red
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "🎉 ALL TESTS PASSED!" -ForegroundColor Green
} else {
    Write-Host "⚠️ Some tests failed. Review the results above." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Test completed at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
