# Batch Processing Testing Guide

## ✅ Prerequisites

1. **Enable Batches**: Set `batch.*.enabled=true` in `application.yml` (already done!)
2. **Start Services**: Ensure all services are running:
   - login-service (port 8080)
   - customer-service (port 8082)
   - product-service (port 8084)
   - calculator-service (port 8085)
   - account-service (port 8087)
3. **Get Admin Token**: Login with ADMIN role to access batch endpoints

## 🔐 Get Admin Token

```bash
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response**: Copy the `token` value for use in batch endpoints.

---

## 📊 Step 1: Check Batch Status

```bash
GET http://localhost:8087/api/accounts/batch/status
Authorization: Bearer {your-admin-token}
```

**Expected Response**:
```json
{
  "interestAccrualEnabled": true,
  "interestCapitalizationEnabled": true,
  "maturityProcessingEnabled": true,
  "timeTravelActive": false,
  "currentBatchDate": "2025-11-09"
}
```

---

## 🕐 Step 2: Test Time Travel Feature

### Set Override Date
```bash
POST http://localhost:8087/api/accounts/batch/time-travel/set?date=2025-01-01
Authorization: Bearer {your-admin-token}
```

**Response**:
```json
{
  "message": "Time travel activated",
  "overrideDate": "2025-01-01",
  "batchDate": "2025-01-01",
  "timeTravelActive": true
}
```

### Check Time Travel Status
```bash
GET http://localhost:8087/api/accounts/batch/time-travel/status
Authorization: Bearer {your-admin-token}
```

**Response**:
```json
{
  "timeTravelActive": true,
  "batchDate": "2025-01-01",
  "overrideDate": "2025-01-01",
  "currentSystemDate": "2025-11-09"
}
```

---

## 🏦 Step 3: Create Test FD Account

First, you need an active FD account to test batches on.

```bash
POST http://localhost:8087/api/accounts/accounts/create/default
Authorization: Bearer {your-admin-token}
Content-Type: application/json

{
  "customerId": 1,
  "productCode": "FD-STANDARD",
  "principalAmount": 100000,
  "termMonths": 12,
  "accountName": "Test FD for Batch Processing",
  "branchCode": "BR001"
}
```

**Note**: Copy the `accountNumber` from the response for later use.

---

## 💰 Step 4: Test Interest Accrual Batch

### Manual Trigger
```bash
POST http://localhost:8087/api/accounts/batch/interest-accrual/trigger
Authorization: Bearer {your-admin-token}
```

**Expected Response**:
```json
{
  "message": "Interest Accrual Batch executed successfully",
  "batchDate": "2025-01-01",
  "timeTravelActive": true
}
```

### Verify Transactions
```bash
GET http://localhost:8087/api/accounts/transactions/account/{accountNumber}
Authorization: Bearer {your-admin-token}
```

**Expected**: You should see INTEREST_CREDIT transactions with remarks "BATCH: Interest accrued but not credited"

**Formula**: Daily Interest = (Principal × Rate) / 36500
- Example: ₹100,000 @ 6.5% = ₹17.81 per day

---

## 📈 Step 5: Test Interest Capitalization Batch

### Advance Time to Next Quarter
```bash
POST http://localhost:8087/api/accounts/batch/time-travel/set?date=2025-04-01
Authorization: Bearer {your-admin-token}
```

### Simulate Daily Interest for 90 Days
You can either:
1. Run interest accrual batch 90 times (tedious)
2. Skip this and test with existing data

### Manual Trigger Capitalization
```bash
POST http://localhost:8087/api/accounts/batch/interest-capitalization/trigger
Authorization: Bearer {your-admin-token}
```

**Expected Response**:
```json
{
  "message": "Interest Capitalization Batch executed successfully",
  "batchDate": "2025-04-01",
  "timeTravelActive": true
}
```

### Verify Account Balance Updated
```bash
GET http://localhost:8087/api/accounts/accounts/{accountNumber}
Authorization: Bearer {your-admin-token}
```

**Expected**: `principalAmount` should have increased by the accrued interest.

---

## 🎯 Step 6: Test Maturity Processing Batch

### Advance Time to Maturity Date
```bash
POST http://localhost:8087/api/accounts/batch/time-travel/set?date=2026-01-01
Authorization: Bearer {your-admin-token}
```

### Manual Trigger Maturity Processing
```bash
POST http://localhost:8087/api/accounts/batch/maturity-processing/trigger
Authorization: Bearer {your-admin-token}
```

**Expected Response**:
```json
{
  "message": "Maturity Processing Batch executed successfully",
  "batchDate": "2026-01-01",
  "timeTravelActive": true
}
```

### Verify Account Status Changed
```bash
GET http://localhost:8087/api/accounts/accounts/{accountNumber}
Authorization: Bearer {your-admin-token}
```

**Expected**: 
- `status` changed from "ACTIVE" to "MATURED"
- Transaction with type "MATURITY_CREDIT" created

---

## 🔄 Step 7: Clear Time Travel

Return to current date:

```bash
POST http://localhost:8087/api/accounts/batch/time-travel/clear
Authorization: Bearer {your-admin-token}
```

**Response**:
```json
{
  "message": "Time travel deactivated",
  "batchDate": "2025-11-09",
  "timeTravelActive": false
}
```

---

## 🧪 Complete Test Scenario

Here's a comprehensive test flow:

### 1. Setup
```bash
# Login as admin
POST http://localhost:8080/api/auth/login
{ "username": "admin", "password": "admin123" }

# Check batch status
GET http://localhost:8087/api/accounts/batch/status
Authorization: Bearer {token}
```

### 2. Create FD Account
```bash
# Set time travel to Jan 1, 2025
POST http://localhost:8087/api/accounts/batch/time-travel/set?date=2025-01-01
Authorization: Bearer {token}

# Create FD account
POST http://localhost:8087/api/accounts/accounts/create/default
Authorization: Bearer {token}
{
  "customerId": 1,
  "productCode": "FD-STANDARD",
  "principalAmount": 100000,
  "termMonths": 3,
  "accountName": "3-Month FD Test",
  "branchCode": "BR001"
}
# Note: Account will be created with effectiveDate = 2025-01-01, maturityDate = 2025-04-01
```

### 3. Accrue Daily Interest
```bash
# Day 1 - Jan 1
POST http://localhost:8087/api/accounts/batch/interest-accrual/trigger
Authorization: Bearer {token}

# Day 2 - Jan 2
POST http://localhost:8087/api/accounts/batch/time-travel/set?date=2025-01-02
POST http://localhost:8087/api/accounts/batch/interest-accrual/trigger

# Continue for 90 days... (or skip to quarter end)
```

### 4. Quarterly Capitalization
```bash
# Advance to Apr 1, 2025 (3 months later)
POST http://localhost:8087/api/accounts/batch/time-travel/set?date=2025-04-01
Authorization: Bearer {token}

# Trigger capitalization
POST http://localhost:8087/api/accounts/batch/interest-capitalization/trigger
Authorization: Bearer {token}

# Check account - principal should have increased
GET http://localhost:8087/api/accounts/accounts/{accountNumber}
Authorization: Bearer {token}
```

### 5. Maturity Processing
```bash
# Trigger maturity (since maturity date = 2025-04-01)
POST http://localhost:8087/api/accounts/batch/maturity-processing/trigger
Authorization: Bearer {token}

# Check account - status should be MATURED
GET http://localhost:8087/api/accounts/accounts/{accountNumber}
Authorization: Bearer {token}
```

### 6. Cleanup
```bash
# Return to present
POST http://localhost:8087/api/accounts/batch/time-travel/clear
Authorization: Bearer {token}
```

---

## 📝 Expected Logs

Check the console output for batch execution logs:

```
🕐 Starting Interest Accrual Batch for date: 2025-01-01
📊 Found 1 active FD accounts for interest accrual
✅ Accrued interest 17.81 for account FD-20250101-001
✅ Interest Accrual Batch completed in 123ms - Success: 1, Skipped: 0, Errors: 0

🕐 Starting Interest Capitalization Batch for date: 2025-04-01
📊 Found 1 compound interest accounts for capitalization
✅ Capitalized interest 1603.56 for account FD-20250101-001
✅ Interest Capitalization Batch completed in 456ms - Success: 1, Skipped: 0, Errors: 0

🕐 Starting Maturity Processing Batch for date: 2025-04-01
📊 Found 1 accounts matured as of 2025-04-01
✅ Processed maturity for account FD-20250101-001
✅ Maturity Processing Batch completed in 234ms - Success: 1, Skipped: 0, Errors: 0
```

---

## ⚠️ Important Notes

1. **ADMIN Only**: All batch endpoints require ADMIN role
2. **Time Travel is In-Memory**: Lost on service restart
3. **Manual Triggers**: Use these for testing; scheduled execution happens at configured cron times
4. **Transaction History**: All batch operations create transaction records
5. **Error Handling**: Batches continue on individual account failures
6. **Idempotency**: Running the same batch twice on the same date may create duplicate transactions (be careful!)

---

## 🐛 Troubleshooting

### Batch Not Enabled
```json
{
  "error": "Interest Accrual Batch is disabled. Set batch.interest-accrual.enabled=true"
}
```
**Solution**: Enable in `application.yml` and restart service.

### 403 Forbidden
```json
{
  "error": "Access Denied"
}
```
**Solution**: Use ADMIN token (CUSTOMER and MANAGER roles cannot access batch endpoints).

### No Accounts Processed
Check logs for skip reasons:
- Account effective date > batch date
- Account maturity date < batch date
- Account status is not ACTIVE
- No compound interest accounts (for capitalization)

---

## ✅ Quick Test Commands (Postman/Swagger)

All endpoints available in Swagger UI:
```
http://localhost:8087/api/accounts/swagger-ui.html
```

Look for the **"Batch Management"** section at the bottom.

---

## 🎉 Success Criteria

- ✅ Batch status shows all batches enabled
- ✅ Time travel can be set and cleared
- ✅ Interest accrual creates INTEREST_CREDIT transactions
- ✅ Interest capitalization increases principal amount
- ✅ Maturity processing changes account status to MATURED
- ✅ All operations logged with success counts
- ✅ ADMIN can access, CUSTOMER/MANAGER cannot

Happy Testing! 🚀
