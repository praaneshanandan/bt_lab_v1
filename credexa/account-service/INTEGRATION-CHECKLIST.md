# Account Service - Integration Checklist

## ✅ Pre-Deployment Verification

### 1. Service Dependencies Status

| Service | Port | Status | Required For |
|---------|------|--------|--------------|
| MySQL | 3306 | ⬜ To Check | Database |
| login-service | 8080 | ⬜ To Check | JWT tokens |
| customer-service | 8082 | ⬜ To Check | Customer validation |
| product-pricing-service | 8084 | ⬜ To Check | Product config |
| calculator-service | 8085 | ⬜ To Check | FD calculations |

### 2. Database Setup

- ⬜ MySQL is running
- ⬜ User `root` / password `Root@123` has access
- ⬜ Database `account_db` will be auto-created (or create manually)

**Manual Database Creation** (if needed):
```sql
CREATE DATABASE IF NOT EXISTS account_db;
USE account_db;
-- Tables will be auto-created by Hibernate
```

### 3. Configuration Verification

- ✅ Port 8087 is available
- ✅ Context path: /api/accounts
- ✅ JWT secret matches other services
- ✅ JJWT version: 0.12.6
- ✅ Service URLs configured correctly
- ✅ Timeouts set appropriately

### 4. Build Verification

```bash
cd account-service
mvnw clean install
```

**Expected**:
- ⬜ Build SUCCESS
- ⬜ No compilation errors
- ⬜ All dependencies resolved

### 5. Start Service

```bash
# Windows
start-service.bat

# Or manual
mvnw spring-boot:run
```

**Expected Console Output**:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.5)

...
🚀 Starting Account Service Application...
...
✅ Account Service started successfully
🌐 Swagger UI: http://localhost:8087/swagger-ui.html
📋 Context Path: /api/accounts
...
```

---

## 🔍 Integration Testing

### Step 1: Health Check (No Auth Required)

**Request**:
```bash
curl http://localhost:8087/api/accounts/health
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Account Service is running",
  "data": null
}
```

**Status**: ⬜ Passed

---

### Step 2: Get JWT Token

**Request to login-service**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Expected Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "roles": "ROLE_ADMIN"
}
```

**Status**: ⬜ Token Obtained

**Copy Token**: `_____________________________________`

---

### Step 3: Test Customer Service Integration

**Request to customer-service**:
```bash
curl -X GET http://localhost:8082/api/customer/customers/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Customer details returned

**Status**: ⬜ Customer Service OK

---

### Step 4: Test Product Service Integration

**Request to product-pricing-service**:
```bash
curl -X GET http://localhost:8084/api/products/products/code/FD-STD-001 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Product details with interest rate, term limits

**Status**: ⬜ Product Service OK

---

### Step 5: Test Calculator Service Integration

**Request to calculator-service**:
```bash
curl -X POST http://localhost:8085/api/calculator/calculate/standalone \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "principalAmount": 50000,
    "interestRate": 7.5,
    "tenureMonths": 12,
    "calculationType": "SIMPLE",
    "startDate": "2025-11-08",
    "tdsApplicable": true,
    "tdsRate": 10
  }'
```

**Expected**: Calculation response with maturity amount

**Status**: ⬜ Calculator Service OK

---

### Step 6: Create Account with Default Values

**Request**:
```bash
curl -X POST http://localhost:8087/api/accounts/create/default \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountName": "Test FD Account",
    "customerId": 1,
    "productCode": "FD-STD-001",
    "principalAmount": 50000,
    "termMonths": 12,
    "effectiveDate": "2025-11-08",
    "branchCode": "BR001",
    "branchName": "Main Branch"
  }'
```

**Expected Response**:
- Status: 201 Created
- Account number generated (e.g., `FD-20251108120000-1234-5`)
- IBAN generated (e.g., `IN29CRED0001FD2511081234`)
- Maturity amount calculated
- Customer details populated
- Product details populated

**Status**: ⬜ Account Created

**Account Number**: `_____________________________________`

**IBAN Number**: `_____________________________________`

---

### Step 7: Create Account with Custom Rate

**Request**:
```bash
curl -X POST "http://localhost:8087/api/accounts/create/custom?customInterestRate=8.5&customCalculationType=COMPOUND&customCompoundingFrequency=MONTHLY" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountName": "Custom Rate FD",
    "customerId": 1,
    "productCode": "FD-STD-001",
    "principalAmount": 75000,
    "termMonths": 18,
    "effectiveDate": "2025-11-08"
  }'
```

**Expected**:
- Custom rate 8.5% applied
- Calculation type: COMPOUND
- Frequency: MONTHLY
- Remarks include "[Custom Rate Applied]"

**Status**: ⬜ Custom Account Created

---

### Step 8: Account Inquiry by Account Number

**Request**:
```bash
curl -X POST http://localhost:8087/api/accounts/inquiry \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "idValue": "YOUR_ACCOUNT_NUMBER"
  }'
```

**Expected**: Account details returned

**Status**: ⬜ Inquiry by Account Number OK

---

### Step 9: Account Inquiry by IBAN

**Request**:
```bash
curl -X POST http://localhost:8087/api/accounts/inquiry \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "idType": "IBAN",
    "idValue": "YOUR_IBAN_NUMBER"
  }'
```

**Expected**: Same account details returned

**Status**: ⬜ Inquiry by IBAN OK

---

### Step 10: Account Inquiry by Internal ID

**Request**:
```bash
curl -X POST http://localhost:8087/api/accounts/inquiry \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "idType": "INTERNAL_ID",
    "idValue": "1"
  }'
```

**Expected**: Same account details returned

**Status**: ⬜ Inquiry by Internal ID OK

---

### Step 11: List All Accounts

**Request**:
```bash
curl -X GET "http://localhost:8087/api/accounts?page=0&size=10&sortBy=createdAt&sortDir=DESC" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Paginated list with both accounts

**Status**: ⬜ List Accounts OK

---

### Step 12: List Accounts by Customer

**Request**:
```bash
curl -X GET http://localhost:8087/api/accounts/customer/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: All accounts for customer ID 1

**Status**: ⬜ List by Customer OK

---

### Step 13: Get Account Balance

**Request**:
```bash
curl -X GET http://localhost:8087/api/accounts/YOUR_ACCOUNT_NUMBER/balance \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**:
- Principal amount
- Interest earned
- Maturity amount
- TDS amount
- Net amount
- Days to maturity

**Status**: ⬜ Balance Check OK

---

### Step 14: Get Account Details

**Request**:
```bash
curl -X GET http://localhost:8087/api/accounts/YOUR_ACCOUNT_NUMBER \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Complete account details

**Status**: ⬜ Account Details OK

---

## 🔒 Security Testing

### Test 1: No Token (Should Fail)

**Request**:
```bash
curl -X POST http://localhost:8087/api/accounts/create/default \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Expected**: 401 Unauthorized

**Status**: ⬜ Auth Required OK

---

### Test 2: CUSTOMER Role (Should Fail Create)

**Login as CUSTOMER**:
```json
{
  "username": "john_doe",
  "password": "password123"
}
```

**Try to Create Account**:
```bash
curl -X POST http://localhost:8087/api/accounts/create/default \
  -H "Authorization: Bearer CUSTOMER_TOKEN" \
  -d '{...}'
```

**Expected**: 403 Forbidden

**Status**: ⬜ RBAC Working OK

---

### Test 3: CUSTOMER Can View Own Accounts

**Request**:
```bash
curl -X GET http://localhost:8087/api/accounts/customer/1 \
  -H "Authorization: Bearer CUSTOMER_TOKEN"
```

**Expected**: 200 OK with accounts list

**Status**: ⬜ Customer Access OK

---

## ✅ Validation Testing

### Test 1: Invalid Customer ID

**Request with non-existent customer**:
```json
{
  "customerId": 99999,
  ...
}
```

**Expected**: 400 Bad Request - "Customer not found with ID: 99999"

**Status**: ⬜ Customer Validation OK

---

### Test 2: Invalid Product Code

**Request with non-existent product**:
```json
{
  "productCode": "INVALID-PROD",
  ...
}
```

**Expected**: 400 Bad Request - "Product not found with code: INVALID-PROD"

**Status**: ⬜ Product Validation OK

---

### Test 3: Amount Below Minimum

**Request with low amount**:
```json
{
  "principalAmount": 500,
  ...
}
```

**Expected**: 400 Bad Request - Amount below minimum

**Status**: ⬜ Min Amount Validation OK

---

### Test 4: Custom Rate Out of Range

**Request with high custom rate**:
```
?customInterestRate=15.0
```

**Expected**: 400 Bad Request - Rate outside allowed range

**Status**: ⬜ Rate Range Validation OK

---

## 📊 Final Integration Status

### Overall Service Health
- ⬜ All dependencies running
- ⬜ Database connected
- ⬜ JWT authentication working
- ⬜ Service integrations successful
- ⬜ All endpoints responding
- ⬜ RBAC functioning
- ⬜ Validations working

### Performance Indicators
- ⬜ Response times < 2 seconds
- ⬜ No memory leaks
- ⬜ Logs showing proper flow
- ⬜ Error handling graceful

### Swagger UI
- ⬜ Accessible at http://localhost:8087/swagger-ui.html
- ⬜ Authorization working
- ⬜ All endpoints visible
- ⬜ Request/Response schemas correct

---

## 🐛 Known Issues / Notes

| Issue | Status | Notes |
|-------|--------|-------|
| - | - | - |

---

## ✅ Sign-Off

**Tested By**: _____________________

**Date**: _____________________

**Environment**: 
- ⬜ Local Development
- ⬜ Testing
- ⬜ Staging

**Overall Status**: 
- ⬜ ✅ All Tests Passed - Ready for Production
- ⬜ ⚠️ Minor Issues - Can Proceed with Notes
- ⬜ ❌ Major Issues - Needs Fixes

**Notes**:
```
_________________________________________________________

_________________________________________________________

_________________________________________________________
```

---

**Generated**: November 8, 2025  
**Version**: 1.0.0  
**Account Service Integration Checklist**
