# Lab L12: FD Module Setup - Testing Guide

**Date:** November 6, 2025  
**Service:** FD Account Service (fd-account-service)  
**Port:** 8086  
**Context Path:** `/api/fd-accounts`

---

## 🚀 Quick Start

### 1. Start All Required Services

The FD Account Service depends on several other services. All services have been started:

```powershell
# Services are running in separate PowerShell windows (minimized)
# If you need to restart, use:
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
.\start-all-services.bat
```

**Services Started:**
- ✅ Customer Service (Port 8083)
- ✅ Product Pricing Service (Port 8084)
- ✅ FD Calculator Service (Port 8085)
- ✅ FD Account Service (Port 8086)

**Note:** Services take 1-2 minutes to fully start. Please wait before testing.

---

## 🔗 Testing Links (Swagger UI)

### Main Service - FD Account Service
**URL:** http://localhost:8086/api/fd-accounts/swagger-ui.html

This is your primary testing interface for Lab L12. It provides:
- Interactive API documentation
- Try-it-out functionality for all endpoints
- Request/response examples
- Schema definitions

### Supporting Services

**Customer Service:**  
http://localhost:8083/api/customer/swagger-ui.html

**Product Pricing Service:**  
http://localhost:8084/api/products/swagger-ui.html

**FD Calculator Service:**  
http://localhost:8085/api/calculator/swagger-ui.html

---

## 📋 Health Check

Before testing, verify all services are UP:

### FD Account Service Health
```
http://localhost:8086/api/fd-accounts/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### Check All Services
```powershell
# Run this in PowerShell to check all services
$services = @(8083, 8084, 8085, 8086)
foreach($port in $services) {
    try {
        $response = Invoke-RestMethod "http://localhost:$port/actuator/health" -TimeoutSec 2
        Write-Host "✅ Port $port - $($response.status)" -ForegroundColor Green
    } catch {
        Write-Host "❌ Port $port - Not responding" -ForegroundColor Red
    }
}
```

---

## 🧪 Test Scenarios

### Scenario 1: Create FD Account (Standard)

**Endpoint:** `POST /api/fd-accounts/accounts`

**Prerequisites:**
1. At least one product must exist (use Product Service to create if needed)
2. At least one customer must exist (use Customer Service to create if needed)
3. You need BANK_OFFICER role (security can be bypassed for testing by removing the token requirement temporarily)

**Sample Request:**
```json
{
  "productCode": "FD001",
  "principalAmount": 100000,
  "termMonths": 12,
  "effectiveDate": "2025-11-06",
  "roles": [
    {
      "customerId": 1,
      "customerName": "John Doe",
      "roleType": "PRIMARY_HOLDER",
      "ownershipPercentage": 100.0,
      "isPrimary": true
    }
  ],
  "branchCode": "001",
  "accountName": "John's Fixed Deposit",
  "autoRenewal": false,
  "tdsApplicable": true,
  "maturityInstruction": "HOLD",
  "remarks": "First FD account",
  "createdBy": "BANK_OFFICER"
}
```

**Expected Response:** HTTP 201 Created
```json
{
  "id": 1,
  "accountNumber": "0011000007",
  "ibanNumber": "IN...",
  "accountName": "John's Fixed Deposit",
  "productCode": "FD001",
  "status": "ACTIVE",
  "principalAmount": 100000.00,
  "interestRate": 7.50,
  "termMonths": 12,
  "maturityAmount": 107500.00,
  "effectiveDate": "2025-11-06",
  "maturityDate": "2026-11-06",
  ...
}
```

### Scenario 2: Create Customized FD Account

**Endpoint:** `POST /api/fd-accounts/accounts/customize`

**Sample Request:**
```json
{
  "productCode": "FD001",
  "principalAmount": 500000,
  "termMonths": 24,
  "effectiveDate": "2025-11-06",
  "customInterestRate": 8.0,
  "customTermMonths": 24,
  "customInterestCalculationMethod": "COMPOUND",
  "customInterestPayoutFrequency": "QUARTERLY",
  "roles": [
    {
      "customerId": 1,
      "customerName": "John Doe",
      "roleType": "PRIMARY_HOLDER",
      "ownershipPercentage": 100.0,
      "isPrimary": true
    }
  ],
  "branchCode": "001",
  "accountName": "Premium FD Account",
  "autoRenewal": true,
  "tdsApplicable": true,
  "maturityInstruction": "AUTO_RENEW",
  "createdBy": "BANK_OFFICER"
}
```

### Scenario 3: Get Account Details

**Endpoint:** `GET /api/fd-accounts/accounts/{accountNumber}`

**Example:**
```
GET http://localhost:8086/api/fd-accounts/accounts/0011000007
```

**Query Parameter:**
- `idType` - ACCOUNT_NUMBER (default), IBAN, or INTERNAL_ID

### Scenario 4: Get Customer's Accounts

**Endpoint:** `GET /api/fd-accounts/accounts/customer/{customerId}`

**Example:**
```
GET http://localhost:8086/api/fd-accounts/accounts/customer/1
```

### Scenario 5: Search Accounts

**Endpoint:** `POST /api/fd-accounts/accounts/search`

**Sample Request:**
```json
{
  "productCode": "FD001",
  "status": "ACTIVE",
  "branchCode": "001",
  "minAmount": 50000,
  "maxAmount": 1000000,
  "page": 0,
  "size": 10,
  "sortBy": "effectiveDate",
  "sortDirection": "DESC"
}
```

### Scenario 6: Get Maturing Accounts

**Endpoint:** `GET /api/fd-accounts/accounts/maturing?days=30`

**Example:**
```
GET http://localhost:8086/api/fd-accounts/accounts/maturing?days=30
```

---

## 🔐 Security Testing

### Role-Based Access Control

**BANK_OFFICER Role:** (For testing, these restrictions can be bypassed)
- ✅ Can create FD accounts
- ✅ Can search all accounts
- ✅ Can view any customer's accounts
- ✅ Can access all reports

**CUSTOMER Role:**
- ✅ Can view own accounts only
- ❌ Cannot create accounts
- ❌ Cannot search accounts
- ❌ Cannot view other customers' accounts

### Testing Without Authentication (Development Mode)

For Lab L12 testing, authentication is currently set to `permitAll()` in SecurityConfig. This allows testing without JWT tokens.

To test with security enabled:
1. Uncomment the JWT filter in SecurityConfig
2. Login via Login Service to get a JWT token
3. Add `Authorization: Bearer <token>` header to requests

---

## 🔍 Validation Testing

### Test Invalid Product Code
```json
{
  "productCode": "INVALID",
  "principalAmount": 100000,
  ...
}
```
**Expected:** 400 Bad Request - "Product not found"

### Test Amount Below Minimum
```json
{
  "productCode": "FD001",
  "principalAmount": 100,
  ...
}
```
**Expected:** 400 Bad Request - "Principal amount below minimum"

### Test Invalid Term
```json
{
  "productCode": "FD001",
  "termMonths": 1,
  ...
}
```
**Expected:** 400 Bad Request - "Term below minimum months"

### Test Invalid Customer
```json
{
  "productCode": "FD001",
  "roles": [
    {
      "customerId": 99999,
      ...
    }
  ]
}
```
**Expected:** 400 Bad Request - "Customer not found"

---

## 🎯 Account Number Generation Testing

### Test Pattern: [3-digit branch][6-digit seq][1-digit check]

Create multiple accounts and verify:
- ✅ Account numbers are 10 digits
- ✅ First 3 digits match branch code
- ✅ Middle 6 digits are sequential
- ✅ Last digit is valid check digit (Luhn algorithm)

**Example Account Numbers:**
- 0011000007 (branch 001, sequence 100000, check 7)
- 0011000015 (branch 001, sequence 100001, check 5)
- 0011000023 (branch 001, sequence 100002, check 3)

### Validate Account Number
```
GET http://localhost:8086/api/fd-accounts/accounts/exists/{accountNumber}
```

---

## 📊 Integration Testing

### Product Service Integration
1. Navigate to Product Service Swagger
2. Create a product if none exists:
```json
{
  "productCode": "FD001",
  "productName": "Regular Fixed Deposit",
  "productType": "FIXED_DEPOSIT",
  "baseInterestRate": 7.5,
  "minAmount": 10000,
  "maxAmount": 10000000,
  "minTermMonths": 6,
  "maxTermMonths": 120,
  "isActive": true,
  "interestCalculationMethod": "COMPOUND",
  "interestPayoutFrequency": "ON_MATURITY"
}
```

### Customer Service Integration
1. Navigate to Customer Service Swagger
2. Create a customer if none exists:
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "1234567890",
  "dateOfBirth": "1990-01-01",
  "address": "123 Main St"
}
```

### Calculator Service Integration
The FD Account Service automatically calls the Calculator Service to compute maturity amounts and dates. No manual testing required.

---

## 📈 Expected Lab L12 Outcomes

After completing Lab L12, you should be able to:

### Database
- ✅ View `fd_accounts` table in MySQL with all fields
- ✅ See `account_roles`, `account_transactions`, `account_balances` tables
- ✅ Verify `account_number_sequences` table for sequence tracking

### Account Number Generation
- ✅ Generate unique 10-digit account numbers
- ✅ Verify Luhn check digit calculation
- ✅ Generate IBAN numbers
- ✅ Validate account numbers

### Entity Design
- ✅ FdAccount entity with all required fields
- ✅ Relationships with AccountRole, AccountTransaction, AccountBalance
- ✅ Proper JPA annotations
- ✅ Audit fields (created_at, created_by, etc.)

### Integration Points
- ✅ Product validation works
- ✅ Rate limits enforced
- ✅ Term boundaries validated
- ✅ Customer validation works
- ✅ Maturity calculation integration works

### Security
- ✅ BANK_OFFICER can create accounts
- ✅ CUSTOMER can view only their accounts
- ✅ Method-level security with @PreAuthorize
- ✅ JWT authentication ready

---

## 🐛 Troubleshooting

### Service Not Starting
1. Check if port is already in use:
```powershell
netstat -ano | findstr :8086
```
2. Kill the process if needed:
```powershell
taskkill /PID <process_id> /F
```

### Service Started but Health Check Fails
- Wait 1-2 minutes for Spring Boot to fully initialize
- Check console logs in the PowerShell window for errors

### Database Connection Errors
- Ensure MySQL is running on localhost:3306
- Verify username/password: root/root
- Check if `fd_account_db` database was created

### Integration Service Errors
- Ensure all dependent services are running (8083, 8084, 8085)
- Check service URLs in application.yml
- Verify network connectivity

### Account Creation Fails
1. Check if product exists:
```
GET http://localhost:8084/api/products/product/{productCode}
```
2. Check if customer exists:
```
GET http://localhost:8083/api/customer/id/{customerId}
```
3. Verify amount and term are within product limits

---

## 📝 Testing Checklist

### Basic Operations
- [ ] Health check responds with UP
- [ ] Swagger UI loads successfully
- [ ] Can create standard FD account
- [ ] Can create customized FD account
- [ ] Can retrieve account by account number
- [ ] Can retrieve account by IBAN
- [ ] Can get customer's accounts
- [ ] Can search accounts with criteria

### Account Number Generation
- [ ] Account numbers are 10 digits
- [ ] Check digit is valid (Luhn algorithm)
- [ ] Sequence increments correctly
- [ ] IBAN is generated correctly

### Validation
- [ ] Invalid product code rejected
- [ ] Amount below minimum rejected
- [ ] Amount above maximum rejected
- [ ] Term below minimum rejected
- [ ] Term above maximum rejected
- [ ] Invalid customer rejected
- [ ] Duplicate account number prevented

### Integration
- [ ] Product details fetched correctly
- [ ] Customer details validated
- [ ] Maturity calculated correctly
- [ ] Rate limits enforced
- [ ] Term boundaries enforced

### Security
- [ ] BANK_OFFICER can create accounts
- [ ] CUSTOMER cannot create accounts
- [ ] CUSTOMER can view own accounts
- [ ] CUSTOMER cannot view others' accounts
- [ ] BANK_OFFICER can access all reports

---

## 🎓 Next Steps

After completing Lab L12 testing:

1. **Lab L13:** FD Account Transactions
   - Interest credit transactions
   - Premature withdrawals
   - Transaction history

2. **Lab L14:** FD Maturity Processing
   - Auto-renewal logic
   - Maturity processing
   - Account closure

3. **Lab L15:** FD Reporting
   - Account statements
   - Interest certificates
   - Portfolio reports

4. **Lab L16:** FD Alerts & Notifications
   - Maturity reminders
   - Interest credit notifications
   - SMS/Email alerts

---

## 📞 Support

If you encounter any issues:
1. Check the console logs in the PowerShell windows
2. Verify all prerequisites are met
3. Review the error messages in Swagger UI
4. Check the Lab-L12-Implementation-Status.md document

---

**Happy Testing! 🚀**

**Services should be ready in 1-2 minutes. Please proceed with testing using the Swagger UI link above.**
