# Account Service - Swagger Testing Guide

## Service Information
- **Service Name**: Account Service (Simplified FD Account Management)
- **Port**: 8087
- **Context Path**: /api/accounts
- **Swagger UI**: http://localhost:8087/swagger-ui.html
- **OpenAPI Docs**: http://localhost:8087/v3/api-docs

## Prerequisites

### 1. Start Required Services
All services must be running:
```bash
# 1. Login Service (Port 8080)
cd login-service
start-service.bat

# 2. Customer Service (Port 8082)
cd customer-service
start-service.bat

# 3. Product Pricing Service (Port 8084)
cd product-pricing-service
start-service.bat

# 4. Calculator Service (Port 8085)
cd fd-calculator-service
mvnw spring-boot:run

# 5. Account Service (Port 8087)
cd account-service
start-service.bat
```

### 2. Get JWT Token
Use login-service to authenticate:

**Endpoint**: `POST http://localhost:8080/api/auth/login`

**Test Users**:
```json
// ADMIN User
{
  "username": "admin",
  "password": "admin123"
}

// MANAGER User
{
  "username": "manager01",
  "password": "password123"
}

// CUSTOMER User
{
  "username": "john_doe",
  "password": "password123"
}
```

**Response** (Copy the token):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "roles": "ROLE_ADMIN"
}
```

### 3. Configure Swagger Authorization
1. Open Swagger UI: http://localhost:8087/swagger-ui.html
2. Click the **"Authorize"** button (🔒 lock icon) at top right
3. Enter: `Bearer <your-token-here>`
4. Click **"Authorize"** then **"Close"**

---

## Feature Testing Guide

## Feature 1: Account Creation - VERSION 1 (Default Values)

### Description
Creates FD account with all values defaulted from the product configuration.

### Endpoint
`POST /api/accounts/create/default`

### Required Role
MANAGER or ADMIN

### Test Case 1.1: Create Standard FD Account
```json
{
  "accountName": "John Doe Standard FD",
  "customerId": 1,
  "productCode": "FD-STD-001",
  "principalAmount": 50000,
  "termMonths": 12,
  "effectiveDate": "2025-11-08",
  "branchCode": "BR001",
  "branchName": "Main Branch",
  "remarks": "Regular FD for 1 year"
}
```

**Expected Response**:
- Status: 201 Created
- Account number generated with check digit (e.g., `FD-20251108120000-1234-5`)
- IBAN number generated (e.g., `IN29CRED0001FD2511081234`)
- Interest rate from product (e.g., 7.5%)
- Maturity amount calculated
- TDS applied if applicable

### Test Case 1.2: Create Senior Citizen FD
```json
{
  "accountName": "Jane Doe Senior Citizen FD",
  "customerId": 2,
  "productCode": "FD-SEN-001",
  "principalAmount": 100000,
  "termMonths": 24,
  "effectiveDate": "2025-11-08",
  "branchCode": "BR001",
  "branchName": "Main Branch"
}
```

**Expected**: Higher interest rate for senior citizen product

---

## Feature 2: Account Creation - VERSION 2 (Custom Values)

### Description
Creates FD account with customized interest rate within ±2% of product base rate.

### Endpoint
`POST /api/accounts/create/custom`

### Required Role
MANAGER or ADMIN

### Test Case 2.1: Custom Interest Rate (Valid)
**Base Request**:
```json
{
  "accountName": "Custom Rate FD Account",
  "customerId": 1,
  "productCode": "FD-STD-001",
  "principalAmount": 75000,
  "termMonths": 18,
  "effectiveDate": "2025-11-08",
  "branchCode": "BR001",
  "branchName": "Main Branch",
  "remarks": "Negotiated rate for valued customer"
}
```

**Query Parameters**:
- `customInterestRate`: 8.5 (if base is 7.5%, this is +1% which is valid)
- `customCalculationType`: COMPOUND
- `customCompoundingFrequency`: MONTHLY

**Full URL**:
```
POST /api/accounts/create/custom?customInterestRate=8.5&customCalculationType=COMPOUND&customCompoundingFrequency=MONTHLY
```

**Expected Response**:
- Custom rate applied: 8.5%
- Calculation type: COMPOUND
- Compounding: MONTHLY
- Higher maturity amount than default
- Remarks include "[Custom Rate Applied]"

### Test Case 2.2: Custom Rate Out of Range (Should Fail)
Same request with:
- `customInterestRate`: 10.5 (if base is 7.5%, this is +3% - outside ±2% range)

**Expected Response**:
- Status: 400 Bad Request
- Error: "Custom interest rate 10.50% is outside allowed range..."

### Test Case 2.3: Only Custom Calculation Type
```json
{
  "accountName": "Compound Interest FD",
  "customerId": 1,
  "productCode": "FD-STD-001",
  "principalAmount": 60000,
  "termMonths": 12,
  "effectiveDate": "2025-11-08"
}
```

**Query Parameters**:
- `customCalculationType`: COMPOUND
- `customCompoundingFrequency`: QUARTERLY

**Expected**: Uses base rate but with compound calculation

---

## Feature 3: Account Inquiry (Flexible ID Types)

### Description
Search for account using different ID types: ACCOUNT_NUMBER (default), IBAN, or INTERNAL_ID.

### Endpoint
`POST /api/accounts/inquiry`

### Required Role
CUSTOMER, MANAGER, or ADMIN

### Test Case 3.1: Inquiry by Account Number (Default)
```json
{
  "idValue": "FD-20251108120000-1234-5"
}
```

**Note**: `idType` not specified, defaults to ACCOUNT_NUMBER

**Expected**: Account details returned

### Test Case 3.2: Inquiry by IBAN
```json
{
  "idType": "IBAN",
  "idValue": "IN29CRED0001FD2511081234"
}
```

**Expected**: Same account details returned

### Test Case 3.3: Inquiry by Internal ID
```json
{
  "idType": "INTERNAL_ID",
  "idValue": "1"
}
```

**Expected**: Account with database ID 1 returned

### Test Case 3.4: Account Not Found
```json
{
  "idType": "ACCOUNT_NUMBER",
  "idValue": "FD-INVALID-9999"
}
```

**Expected**:
- Status: 404 Not Found
- Error message: "Account not found with account number: FD-INVALID-9999"

---

## Feature 4: Account List

### Endpoint
`GET /api/accounts`

### Required Role
MANAGER or ADMIN

### Test Case 4.1: List All Accounts (First Page)
**URL**: `/api/accounts?page=0&size=10&sortBy=createdAt&sortDir=DESC`

**Expected Response**:
- Paginated list of accounts
- Newest accounts first
- Page metadata (totalElements, totalPages, etc.)

### Test Case 4.2: List with Custom Sorting
**URL**: `/api/accounts?page=0&size=5&sortBy=principalAmount&sortDir=DESC`

**Expected**: Accounts sorted by principal amount (highest first)

### Test Case 4.3: List by Customer
**Endpoint**: `GET /api/accounts/customer/1`

**Expected**: All accounts for customer ID 1

---

## Feature 5: Account Details

### Endpoint
`GET /api/accounts/{accountNumber}`

### Required Role
CUSTOMER, MANAGER, or ADMIN

### Test Case 5.1: Get Account Details
**URL**: `/api/accounts/FD-20251108120000-1234-5`

**Expected Response**: Complete account details including:
- Account numbers (standard + IBAN)
- Customer info
- Product info
- Financial details (principal, interest, maturity)
- Calculation details
- TDS info
- Dates
- Status

---

## Feature 6: Account Balance

### Endpoint
`GET /api/accounts/{accountNumber}/balance`

### Required Role
CUSTOMER, MANAGER, or ADMIN

### Test Case 6.1: Get Balance
**URL**: `/api/accounts/FD-20251108120000-1234-5/balance`

**Expected Response**:
```json
{
  "success": true,
  "message": "Balance retrieved successfully",
  "data": {
    "accountNumber": "FD-20251108120000-1234-5",
    "accountName": "John Doe Standard FD",
    "principalAmount": 50000.00,
    "interestEarned": 3750.00,
    "maturityAmount": 53750.00,
    "tdsAmount": 375.00,
    "netAmount": 53375.00,
    "status": "ACTIVE",
    "effectiveDate": "2025-11-08",
    "maturityDate": "2026-11-08",
    "daysToMaturity": 365
  }
}
```

---

## Feature 7: Account Number Generation with Check Digit

### How It Works
1. **Standard Format**: `FD-YYYYMMDDHHMMSS-NNNN-C`
   - `FD`: Fixed Deposit prefix
   - `YYYYMMDDHHMMSS`: Timestamp
   - `NNNN`: 4-digit sequence
   - `C`: Luhn check digit

2. **IBAN Format**: `IN<check-digit><bank><branch><account>`
   - `IN`: Country code
   - Check digit calculated using mod 97 algorithm
   - Bank: CRED (Credexa)
   - Branch: 0001 (default)
   - Account: Shortened timestamp + sequence

### Validation
- Account numbers are validated using Luhn algorithm
- IBAN validated using mod 97 algorithm
- Both are automatically validated during creation

---

## Common Test Scenarios

### Scenario 1: End-to-End Account Creation
1. Login as MANAGER
2. Create customer (if needed) via customer-service
3. Verify product exists via product-pricing-service
4. Create FD account with default values
5. Verify account created with proper calculations
6. Check balance
7. List accounts to verify it appears

### Scenario 2: Custom Rate Negotiation
1. Login as ADMIN
2. Create account with custom rate within allowed range
3. Verify custom rate applied
4. Compare maturity amount with default rate account

### Scenario 3: Customer Access
1. Login as CUSTOMER (john_doe)
2. List own accounts: `GET /api/accounts/customer/1`
3. View specific account
4. Check balance
5. Try to create account → Should get 403 Forbidden

### Scenario 4: Multi-ID Lookup
1. Create account, note all IDs:
   - Internal ID from response
   - Account number
   - IBAN number
2. Use inquiry endpoint with each ID type
3. Verify all return same account

---

## Validation Tests

### Test: Product Constraints
Try creating account with:
- Amount below product minimum → 400 Bad Request
- Amount above product maximum → 400 Bad Request
- Term below minimum months → 400 Bad Request
- Term above maximum months → 400 Bad Request

**Example (Invalid Amount)**:
```json
{
  "accountName": "Invalid Amount FD",
  "customerId": 1,
  "productCode": "FD-STD-001",
  "principalAmount": 500,
  "termMonths": 12,
  "effectiveDate": "2025-11-08"
}
```

**Expected**: Error about minimum amount constraint

### Test: Customer Validation
```json
{
  "accountName": "Invalid Customer FD",
  "customerId": 99999,
  "productCode": "FD-STD-001",
  "principalAmount": 50000,
  "termMonths": 12,
  "effectiveDate": "2025-11-08"
}
```

**Expected**: Error "Customer not found with ID: 99999"

### Test: Product Validation
```json
{
  "accountName": "Invalid Product FD",
  "customerId": 1,
  "productCode": "INVALID-PROD",
  "principalAmount": 50000,
  "termMonths": 12,
  "effectiveDate": "2025-11-08"
}
```

**Expected**: Error "Product not found with code: INVALID-PROD"

---

## Health Check

### Endpoint
`GET /api/accounts/health`

### No Authentication Required

**Expected Response**:
```json
{
  "success": true,
  "message": "Account Service is running"
}
```

---

## Role-Based Access Control

| Endpoint | CUSTOMER | MANAGER | ADMIN |
|----------|----------|---------|-------|
| POST /create/default | ❌ | ✅ | ✅ |
| POST /create/custom | ❌ | ✅ | ✅ |
| POST /inquiry | ✅ | ✅ | ✅ |
| GET /{accountNumber} | ✅ | ✅ | ✅ |
| GET / (list all) | ❌ | ✅ | ✅ |
| GET /customer/{id} | ✅* | ✅ | ✅ |
| GET /{accountNumber}/balance | ✅ | ✅ | ✅ |
| GET /health | ✅ (Public) | ✅ (Public) | ✅ (Public) |

*Customers should only see their own accounts (implement customer ID check in future)

---

## Integration Points

### 1. Customer Service (Port 8082)
- **Used For**: Customer validation and details
- **Endpoint**: `GET /api/customer/customers/{id}`
- **Impact**: Account creation fails if customer not found

### 2. Product Pricing Service (Port 8084)
- **Used For**: Product configuration and validation
- **Endpoint**: `GET /api/products/products/code/{code}`
- **Impact**: Provides interest rate, term limits, TDS rules

### 3. Calculator Service (Port 8085)
- **Used For**: FD maturity calculations
- **Endpoint**: `POST /api/calculator/calculate/standalone`
- **Impact**: Calculates maturity amount, interest, TDS

---

## Troubleshooting

### Issue: 401 Unauthorized
**Solution**: 
1. Verify JWT token is valid
2. Check token in Swagger: Click "Authorize", enter `Bearer <token>`
3. Token might be expired, get new token from login-service

### Issue: 403 Forbidden
**Solution**: 
- Verify user has required role for endpoint
- ADMIN/MANAGER: Can create accounts
- CUSTOMER: Can only view accounts

### Issue: Service Integration Failed
**Solution**:
1. Verify all dependent services are running
2. Check logs for specific error
3. Test individual service endpoints

### Issue: Validation Failed
**Solution**:
- Check product constraints (min/max amount, term)
- Verify customer exists in customer-service
- Verify product is active

---

## Success Indicators

✅ **Account Created Successfully**:
- Response code: 201
- Account number with check digit generated
- IBAN number generated
- Maturity amount calculated
- All integrations successful

✅ **Custom Rate Applied**:
- Custom rate within ±2% of base
- Remarks include "[Custom Rate Applied]"
- Calculation uses custom parameters

✅ **Inquiry Works**:
- Can find account by all three ID types
- Returns same account details

✅ **List Pagination**:
- Returns correct page size
- Sorting works
- Metadata accurate

---

## Next Steps

After testing basic functionality:
1. Test role-based access (try unauthorized actions)
2. Test boundary conditions (min/max amounts, terms)
3. Test with multiple products
4. Create accounts for multiple customers
5. Verify calculations match calculator-service directly
6. Test IBAN validation
7. Test account number check digit validation

---

## API Documentation
Full API documentation available at: http://localhost:8087/swagger-ui.html
