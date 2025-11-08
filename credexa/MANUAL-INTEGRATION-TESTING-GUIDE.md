# Manual Integration Testing Guide
## Calculator Service ↔ Product Pricing Service

**Date:** November 8, 2025  
**Purpose:** Comprehensive testing of all integration points between FD Calculator and Product Pricing services

---

## Prerequisites

### 1. Start All Services
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt_khatam\credexa"
.\start-all-services.bat
# Wait 60 seconds for all services to fully start
```

### 2. Verify Services Are Running
```powershell
netstat -ano | findstr "LISTENING" | findstr "808"
```

**Expected Ports:**
- 8080: API Gateway
- 8081 or 8087: Login Service
- 8082: Customer Service  
- 8084: Product Pricing Service
- 8085: FD Calculator Service
- 8086: FD Account Service

### 3. Access Swagger UIs
- **Product Pricing:** http://localhost:8084/api/products/swagger-ui/index.html
- **Calculator:** http://localhost:8085/api/calculator/swagger-ui/index.html
- **Login:** http://localhost:8081/api/login/swagger-ui/index.html (or 8087)

---

## Integration Points to Test

### **INTEGRATION 1: Service Health**
Verify both services are operational.

**Test Steps:**
1. Open Calculator Swagger: http://localhost:8085/api/calculator/swagger-ui/index.html
2. Navigate to `actuator-endpoint-controller`
3. Execute `GET /actuator/health`
4. **Expected:** `{"status":"UP"}`

5. Open Product Pricing Swagger: http://localhost:8084/api/products/swagger-ui/index.html
6. Navigate to `actuator-endpoint-controller`
7. Execute `GET /actuator/health`
8. **Expected:** `{"status":"UP"}`

**Status:** ✅ / ❌

---

### **INTEGRATION 2: Product Fetch by ID**
Calculator service fetches product details from Product Pricing.

**Setup - Create a Product:**
1. Open Product Pricing Swagger
2. Authorize with JWT (see Authentication section below)
3. Execute `POST /api/products/products` with:
```json
{
  "productName": "Senior Citizen FD",
  "description": "Fixed Deposit for Senior Citizens",
  "productCode": "SC-FD-001",
  "baseInterestRate": 7.5,
  "minAmount": 10000,
  "maxAmount": 10000000,
  "minTermMonths": 6,
  "maxTermMonths": 120,
  "prematureWithdrawalAllowed": true,
  "penaltyPercentage": 1.0,
  "compoundingFrequency": "QUARTERLY",
  "tdsApplicable": true,
  "tdsRate": 10.0,
  "active": true
}
```
4. **Note the Product ID** from response

**Test Product Fetch:**
1. Execute `GET /api/products/products/{id}` with the product ID
2. **Expected:** Full product details returned
3. Verify `baseInterestRate`, `minAmount`, `maxAmount`, `minTermMonths`, `maxTermMonths`, `tdsRate`, `tdsApplicable`

**Integration Verification:**
- Calculator calls `http://localhost:8084/api/products/{id}` internally
- Product data is cached in Calculator service (24 hours)
- Used by Calculator for validation and calculation

**Status:** ✅ / ❌

---

### **INTEGRATION 3: Interest Rate Fetch (Applicable Rate)**
Calculator fetches applicable interest rate from Rate Matrix.

**Setup - Create Interest Rate Matrix:**
1. In Product Pricing Swagger, execute `POST /api/products/products/{productId}/interest-rates` with:
```json
{
  "productId": YOUR_PRODUCT_ID,
  "minAmount": 100000,
  "maxAmount": 500000,
  "minTermMonths": 12,
  "maxTermMonths": 24,
  "customerClassification": "GENERAL",
  "interestRate": 7.75,
  "additionalRate": 0.25,
  "effectiveFrom": "2025-01-01"
}
```

2. Create another rate for SENIOR_CITIZEN:
```json
{
  "productId": YOUR_PRODUCT_ID,
  "minAmount": 100000,
  "maxAmount": 500000,
  "minTermMonths": 12,
  "maxTermMonths": 24,
  "customerClassification": "SENIOR_CITIZEN",
  "interestRate": 7.75,
  "additionalRate": 0.75,
  "effectiveFrom": "2025-01-01"
}
```

**Test Rate Fetch:**
1. Execute `GET /api/products/products/{productId}/interest-rates/applicable` with:
   - `depositAmount`: 200000
   - `tenureMonths`: 18
   - `customerClassification`: GENERAL

2. **Expected Response:**
```json
{
  "success": true,
  "data": {
    "productId": YOUR_PRODUCT_ID,
    "interestRate": 7.75,
    "additionalRate": 0.25,
    "totalRate": 8.0,
    "customerClassification": "GENERAL",
    ...
  }
}
```

3. Test with SENIOR_CITIZEN classification
4. **Expected:** totalRate = 8.5 (7.75 + 0.75)

**Integration Verification:**
- Calculator calls `/api/products/products/{productId}/interest-rates/applicable`
- Note: URL has double `/products/` due to controller mapping (this is correct!)
- Rate data is cached in Calculator service

**Status:** ✅ / ❌

---

### **INTEGRATION 4: Product-Based Simple Interest Calculation**
Complete end-to-end calculation using product data.

**Test Steps:**
1. Open Calculator Swagger
2. Authorize with JWT
3. Execute `POST /api/calculator/calculate/product-based` with:
```json
{
  "customerId": 1,
  "productId": YOUR_PRODUCT_ID,
  "principalAmount": 200000,
  "tenureMonths": 18,
  "calculationType": "SIMPLE"
}
```

4. **Expected Response:**
```json
{
  "success": true,
  "data": {
    "calculationId": ...,
    "customerId": 1,
    "productId": YOUR_PRODUCT_ID,
    "principalAmount": 200000.0,
    "interestRate": 8.0,  // From rate matrix
    "tenureMonths": 18,
    "calculationType": "SIMPLE",
    "totalInterest": 24000.0,  // 200000 * 8.0 * 1.5 / 100
    "maturityAmount": 224000.0,
    "tdsAmount": 2400.0,  // 24000 * 10%
    "netMaturityAmount": 221600.0
  }
}
```

**Verification Checklist:**
- ✅ Product fetched from Product Pricing
- ✅ Interest rate fetched from Rate Matrix
- ✅ TDS rate (10%) applied from product
- ✅ Calculation mathematically correct
- ✅ Response includes all expected fields

**Status:** ✅ / ❌

---

### **INTEGRATION 5: Product-Based Compound Interest Calculation**
Test quarterly compounding from product.

**Test Steps:**
1. Execute `POST /api/calculator/calculate/product-based` with:
```json
{
  "customerId": 1,
  "productId": YOUR_PRODUCT_ID,
  "principalAmount": 200000,
  "tenureMonths": 12,
  "calculationType": "COMPOUND"
}
```

2. **Expected:**
   - Uses `compoundingFrequency: "QUARTERLY"` from product
   - Formula: A = P(1 + r/4)^(4*t) where t = 1 year
   - Compound interest > Simple interest for same parameters

**Verification:**
- ✅ Compounding frequency from product used
- ✅ Compound calculation correct
- ✅ TDS applied on final interest

**Status:** ✅ / ❌

---

### **INTEGRATION 6: Amount Validation (Min/Max)**
Test that product amount limits are enforced.

**Test Below Minimum:**
1. Execute `POST /api/calculator/calculate/product-based` with:
```json
{
  "customerId": 1,
  "productId": YOUR_PRODUCT_ID,
  "principalAmount": 5000,  // Below minAmount (10000)
  "tenureMonths": 12,
  "calculationType": "SIMPLE"
}
```

2. **Expected:** HTTP 400 Bad Request
```json
{
  "success": false,
  "message": "Principal amount must be between 10000.0 and 10000000.0"
}
```

**Test Above Maximum:**
1. Execute with `principalAmount: 20000000` (above maxAmount)
2. **Expected:** Similar validation error

**Status:** ✅ / ❌

---

### **INTEGRATION 7: Tenure Validation (Min/Max)**
Test that product tenure limits are enforced.

**Test Below Minimum:**
1. Execute `POST /api/calculator/calculate/product-based` with:
```json
{
  "customerId": 1,
  "productId": YOUR_PRODUCT_ID,
  "principalAmount": 100000,
  "tenureMonths": 3,  // Below minTermMonths (6)
  "calculationType": "SIMPLE"
}
```

2. **Expected:** HTTP 400 Bad Request
```json
{
  "success": false,
  "message": "Tenure must be between 6 and 120 months"
}
```

**Test Above Maximum:**
1. Execute with `tenureMonths: 150` (above maxTermMonths)
2. **Expected:** Similar validation error

**Status:** ✅ / ❌

---

### **INTEGRATION 8: TDS Calculation from Product**
Verify TDS rate and applicability from product.

**Test with TDS Applicable:**
1. Ensure product has `tdsApplicable: true` and `tdsRate: 10.0`
2. Execute a product-based calculation
3. **Verify in response:**
   - `tdsAmount = totalInterest * 0.10`
   - `netMaturityAmount = maturityAmount - tdsAmount`

**Test with TDS Not Applicable:**
1. Update product: `tdsApplicable: false`
2. Execute calculation
3. **Expected:**
   - `tdsAmount = 0.0`
   - `netMaturityAmount = maturityAmount`

**Status:** ✅ / ❌

---

### **INTEGRATION 9: Customer Classification Bonus Rates**
Test that classification-based additional rates work.

**Test SENIOR_CITIZEN Classification:**
1. Execute calculation with SENIOR_CITIZEN rate matrix entry
2. **Expected:**
   - Additional 0.5% rate (or as configured in rate matrix)
   - Higher interest than GENERAL classification

**Test Multiple Classifications:**
Create rate entries for:
- GENERAL: base + 0.25%
- SENIOR_CITIZEN: base + 0.75%
- SUPER_SENIOR_CITIZEN: base + 1.0%
- STAFF: base + 0.5%

Test each and verify correct rates applied.

**Maximum Bonus Verification:**
- Calculator service caps classification bonus at 2% above base
- If rate matrix has additionalRate > 2%, it should be capped

**Status:** ✅ / ❌

---

### **INTEGRATION 10: Custom Rate with 2% Cap**
Test that custom interest rates don't exceed base + 2%.

**Test Steps:**
1. Get product with `baseInterestRate: 7.5%`
2. Execute `POST /api/calculator/calculate/product-based` with:
```json
{
  "customerId": 1,
  "productId": YOUR_PRODUCT_ID,
  "principalAmount": 100000,
  "tenureMonths": 12,
  "calculationType": "SIMPLE",
  "customInterestRate": 10.0  // Exceeds base + 2% (9.5%)
}
```

3. **Expected:**
   - Rate is capped at 9.5% (base 7.5% + 2%)
   - Calculation uses 9.5%, not 10.0%

**Verification:**
- ✅ Custom rate validated against cap
- ✅ Appropriate warning/message in response
- ✅ Calculation uses capped rate

**Status:** ✅ / ❌

---

## Authentication

All protected endpoints require JWT authentication.

### Get JWT Token:
1. Open Login Service Swagger: http://localhost:8081/api/login/swagger-ui/index.html
2. Execute `POST /api/login/authenticate` with:
```json
{
  "username": "admin@credexa.com",
  "password": "admin123"
}
```

3. Copy the `token` from response
4. In Product Pricing/Calculator Swagger:
   - Click "Authorize" button (top right)
   - Enter: `Bearer YOUR_TOKEN_HERE`
   - Click "Authorize"

### Test Users:
- Admin: `admin@credexa.com` / `admin123`
- Customer: `customer@example.com` / `customer123`

---

## Integration Summary

| # | Integration Point | Status | Notes |
|---|-------------------|--------|-------|
| 1 | Service Health Check | ⬜ | Both services UP |
| 2 | Product Fetch by ID | ⬜ | Product data retrieved |
| 3 | Interest Rate Fetch | ⬜ | Applicable rate from matrix |
| 4 | Simple Interest Calc | ⬜ | End-to-end with product |
| 5 | Compound Interest Calc | ⬜ | Compounding frequency used |
| 6 | Amount Validation | ⬜ | Min/max enforced |
| 7 | Tenure Validation | ⬜ | Min/max enforced |
| 8 | TDS Calculation | ⬜ | Rate from product applied |
| 9 | Classification Bonus | ⬜ | Additional rates work |
| 10 | Custom Rate Cap | ⬜ | 2% cap enforced |

**Legend:** ⬜ Not Tested | ✅ Pass | ❌ Fail

---

## Key Integration URLs (Internal)

These URLs are called by Calculator service internally (you won't call them directly):

1. **Product Fetch:**  
   `GET http://localhost:8084/api/products/{id}`

2. **Interest Rate Fetch:**  
   `GET http://localhost:8084/api/products/products/{productId}/interest-rates/applicable`  
   *(Note: Double `/products/` is correct due to controller @RequestMapping)*

3. **Configuration:**  
   File: `fd-calculator-service/src/main/resources/application.yml`  
   Property: `services.product-pricing.url=http://localhost:8084/api/products`

---

## Troubleshooting

### Services Not Starting
```powershell
# Check if MySQL is running
Get-Service | Where-Object {$_.Name -like "*mysql*"}

# Check which ports are in use
netstat -ano | findstr "LISTENING" | findstr "808"

# Kill stuck Java processes
Get-Process java | Stop-Process -Force

# Restart services
.\start-all-services.bat
```

### 403 Forbidden Errors
- Ensure you've authorized with JWT token in Swagger
- Token expires after 1 hour - get a new one
- Verify user has correct role (CUSTOMER for calculator operations)

### 404 Not Found
- Double-check the context path is included in URL
- Product Pricing: `/api/products/...`
- Calculator: `/api/calculator/...`
- Login: `/api/login/...`

### Integration Failures
- Verify Product Pricing is running (port 8084)
- Check Calculator service logs for WebClient errors
- Ensure product ID exists before testing
- Verify interest rate matrix has entries for your test parameters

---

## Changes Made to Calculator Service

### Files Modified:
1. **ProductIntegrationService.java**
   - Updated interest rate URL to `/products/{id}/interest-rates/applicable`
   - Accounts for controller's @RequestMapping causing double `/products/`

2. **FdCalculatorService.java**
   - Added null-safe handling for `product.getMaxInterestRate()`
   - Fallback cap of base + 2% when maxInterestRate not defined

3. **SecurityConfig.java**
   - Added `/actuator/**` to permitAll for health checks
   - Changed from `/health` to `/actuator/**` pattern

### NO Changes to:
- Product Pricing Service (as per requirements)
- Customer Service
- Login Service
- FD Account Service
- API Gateway

---

## Test Results

**Date Tested:** __________  
**Tested By:** __________  

**Summary:**
- Total Integration Points: 10
- Passed: ___
- Failed: ___
- Blockers: ___

**Issues Found:**
1. 
2. 
3. 

**Recommendations:**
1. 
2. 
3. 

---

**End of Manual Integration Testing Guide**
