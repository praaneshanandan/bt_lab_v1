# Batch Processing Implementation - Account Service

## Overview

The account-service now includes batch processing capabilities for automated FD account management. All batches are **disabled by default** and support **time travel** for testing.

## 🎯 Implemented Batches

### 1. Interest Accrual Batch
**Purpose**: Calculate and record daily interest for all active FD accounts

**Schedule**: Daily at 1:00 AM  
**Configuration**: `batch.interest-accrual.enabled=true`  
**Cron**: `0 0 1 * * ?` (configurable via `batch.interest-accrual.cron`)

**What it does**:
- Finds all ACTIVE accounts
- Calculates daily interest: `(Principal × Interest Rate) / 36500`
- Creates INTEREST_CREDIT transaction with status COMPLETED
- **Does NOT update account balance** (accrual only, capitalization is separate)
- Remarks: "BATCH: Interest accrued but not credited"

**Skip Conditions**:
- Account effective date > batch date
- Account maturity date < batch date

**Example**:
```
Principal: ₹100,000
Rate: 6.5% p.a.
Daily Interest = 100000 × (6.5/36500) = ₹17.81
```

---

### 2. Interest Capitalization Batch
**Purpose**: Add accrued interest to principal (compounding)

**Schedule**: Quarterly on 1st of Jan, Apr, Jul, Oct at 2:00 AM  
**Configuration**: `batch.interest-capitalization.enabled=true`  
**Cron**: `0 0 2 1 1,4,7,10 ?` (configurable via `batch.interest-capitalization.cron`)

**What it does**:
- Finds all ACTIVE accounts with calculation type = "COMPOUND"
- Checks if today is a capitalization date (every 3 months from effective date)
- Sums all INTEREST_CREDIT transactions from the last quarter
- Adds accrued interest to principal amount
- Creates INTEREST_CREDIT transaction **with balance update**
- Remarks: "BATCH: Interest capitalized and added to principal"

**Example**:
```
Original Principal: ₹100,000
3 Months Accrued Interest: ₹1,625
New Principal: ₹101,625
```

---

### 3. Maturity Processing Batch
**Purpose**: Automatically process FD accounts that have reached maturity

**Schedule**: Daily at 3:00 AM  
**Configuration**: `batch.maturity-processing.enabled=true`  
**Cron**: `0 0 3 * * ?` (configurable via `batch.maturity-processing.cron`)

**What it does**:
- Finds all ACTIVE accounts where maturity date <= batch date
- Calculates final maturity amount (principal + all interest)
- Creates MATURITY_CREDIT transaction
- Updates account status to MATURED
- Remarks: "BATCH: Maturity amount credited - Principal: X, Interest: Y"

**Example**:
```
Principal: ₹100,000
Total Interest Earned: ₹6,500
Maturity Amount: ₹106,500
Status: ACTIVE → MATURED
```

---

## ⏰ Time Travel Feature

The batch processing system supports **time travel** for testing batches at different dates without changing the system clock.

### How It Works

**BatchTimeService** provides:
- `getBatchDate()` - Returns override date or current date
- `setOverrideDate(date)` - Enable time travel
- `clearOverrideDate()` - Return to present
- `isTimeTravelActive()` - Check status

All batches use `batchTimeService.getBatchDate()` instead of `LocalDate.now()`.

### API Endpoints

#### Set Time Travel Date
```http
POST /api/accounts/batch/time-travel/set?date=2024-12-31
Authorization: Bearer {admin-token}
```

**Response**:
```json
{
  "message": "Time travel activated",
  "overrideDate": "2024-12-31",
  "batchDate": "2024-12-31",
  "timeTravelActive": true
}
```

#### Clear Time Travel (Return to Present)
```http
POST /api/accounts/batch/time-travel/clear
Authorization: Bearer {admin-token}
```

**Response**:
```json
{
  "message": "Time travel deactivated",
  "batchDate": "2025-01-14",
  "timeTravelActive": false
}
```

#### Check Time Travel Status
```http
GET /api/accounts/batch/time-travel/status
Authorization: Bearer {admin-token}
```

**Response**:
```json
{
  "timeTravelActive": true,
  "batchDate": "2024-12-31",
  "overrideDate": "2024-12-31",
  "currentSystemDate": "2025-01-14"
}
```

---

## 🔧 Configuration

### application.yml

```yaml
batch:
  interest-accrual:
    enabled: false  # Set to true to enable
    cron: "0 0 1 * * ?"  # 1:00 AM daily
  interest-capitalization:
    enabled: false
    cron: "0 0 2 1 1,4,7,10 ?"  # 2:00 AM on 1st of Jan/Apr/Jul/Oct
  maturity-processing:
    enabled: false
    cron: "0 0 3 * * ?"  # 3:00 AM daily
```

### Enable Batches

**Option 1: application.yml**
```yaml
batch:
  interest-accrual:
    enabled: true
```

**Option 2: Environment Variable**
```bash
export BATCH_INTEREST_ACCRUAL_ENABLED=true
```

**Option 3: Command Line**
```bash
java -jar account-service.jar --batch.interest-accrual.enabled=true
```

---

## 🎮 Manual Batch Triggers

All batches can be manually triggered via REST API (ADMIN only).

### Trigger Interest Accrual
```http
POST /api/accounts/batch/interest-accrual/trigger
Authorization: Bearer {admin-token}
```

**Response (Success)**:
```json
{
  "message": "Interest Accrual Batch executed successfully",
  "batchDate": "2025-01-14",
  "timeTravelActive": false
}
```

**Response (Disabled)**:
```json
{
  "error": "Interest Accrual Batch is disabled. Set batch.interest-accrual.enabled=true"
}
```

### Trigger Interest Capitalization
```http
POST /api/accounts/batch/interest-capitalization/trigger
Authorization: Bearer {admin-token}
```

### Trigger Maturity Processing
```http
POST /api/accounts/batch/maturity-processing/trigger
Authorization: Bearer {admin-token}
```

### Check Batch Status
```http
GET /api/accounts/batch/status
Authorization: Bearer {admin-token}
```

**Response**:
```json
{
  "interestAccrualEnabled": true,
  "interestCapitalizationEnabled": false,
  "maturityProcessingEnabled": false,
  "timeTravelActive": false,
  "currentBatchDate": "2025-01-14"
}
```

---

## 🧪 Testing Scenario

### Scenario: Test 90-day FD with quarterly compounding

**Step 1: Create FD Account**
```http
POST /api/accounts/accounts/create/default
{
  "customerId": 1,
  "productId": 101,
  "depositAmount": 100000,
  "tenure": 90,
  "tenureUnit": "DAYS",
  "interestRate": 6.5,
  "branchCode": "BR001"
}
```

Account created with:
- Effective Date: 2024-01-01
- Maturity Date: 2024-03-31
- Principal: ₹100,000

**Step 2: Enable Batches**
```yaml
batch:
  interest-accrual:
    enabled: true
  interest-capitalization:
    enabled: true
  maturity-processing:
    enabled: true
```

**Step 3: Test Day 1 (Interest Accrual)**
```http
POST /api/accounts/batch/time-travel/set?date=2024-01-02
POST /api/accounts/batch/interest-accrual/trigger
```

Check transactions:
```http
GET /api/accounts/transactions/account/{accountNumber}
```

Expected: INTEREST_CREDIT transaction for ₹17.81 (principal unchanged)

**Step 4: Test Day 90 (First Quarter Capitalization)**
```http
POST /api/accounts/batch/time-travel/set?date=2024-04-01
POST /api/accounts/batch/interest-capitalization/trigger
```

Expected:
- Sum of 90 days interest: ~₹1,603
- New principal: ₹101,603
- INTEREST_CREDIT transaction with balance update

**Step 5: Test Maturity**
```http
POST /api/accounts/batch/time-travel/set?date=2024-04-01
POST /api/accounts/batch/maturity-processing/trigger
```

Expected:
- MATURITY_CREDIT transaction
- Account status: ACTIVE → MATURED
- Final amount: ₹101,603 (principal after capitalization)

**Step 6: Return to Present**
```http
POST /api/accounts/batch/time-travel/clear
```

---

## 📊 Batch Execution Logs

Each batch produces comprehensive logs:

```
2025-01-14 01:00:00 - 🕐 Starting Interest Accrual Batch for date: 2025-01-14
2025-01-14 01:00:01 - 📊 Found 150 active FD accounts for interest accrual
2025-01-14 01:00:05 - ✅ Accrued interest 17.81 for account FD-20250114-001
2025-01-14 01:00:05 - ⏭️ Skipping account FD-20250114-002 - matured
2025-01-14 01:00:10 - ✅ Interest Accrual Batch completed in 10523ms - Success: 148, Skipped: 2, Errors: 0
```

---

## 🔒 Security

All batch endpoints require:
- Valid JWT token
- ROLE_ADMIN authority
- Secured with `@PreAuthorize("hasRole('ADMIN')")`

Customers and Managers **cannot** access batch endpoints.

---

## 🏗️ Architecture

### Package Structure
```
com.app.account.batch/
  ├── BatchTimeService.java           # Time travel support
  ├── InterestAccrualBatch.java       # Daily interest accrual
  ├── InterestCapitalizationBatch.java # Quarterly compounding
  └── MaturityProcessingBatch.java    # Maturity processing

com.app.account.controller/
  └── BatchController.java            # Manual triggers & time travel API
```

### Key Features

✅ **Conditional Activation**: All batches use `@ConditionalOnProperty` with `matchIfMissing = false`  
✅ **Safe by Default**: Disabled unless explicitly enabled in configuration  
✅ **Time Travel**: Override batch date for testing without changing system clock  
✅ **Error Isolation**: Per-account error handling prevents batch failure  
✅ **Transaction Support**: All batch methods are `@Transactional`  
✅ **Comprehensive Logging**: Success/skip/error counts with duration tracking  
✅ **Manual Triggers**: REST API for on-demand batch execution  
✅ **Status Monitoring**: Check which batches are enabled and current date

---

## ⚠️ Important Notes

1. **All batches are DISABLED by default** - Must explicitly enable in configuration
2. **Time travel is in-memory** - Override is lost on service restart
3. **Single instance only** - Time travel is not thread-safe across multiple instances
4. **Manual triggers respect time travel** - Use time travel to test batches at different dates
5. **No schema changes** - All batches use existing FdAccount and FdTransaction entities
6. **Existing functionality unchanged** - Batches are in separate package, no modifications to existing code

---

## 🚀 Production Deployment

### Recommended Configuration

```yaml
batch:
  interest-accrual:
    enabled: true
    cron: "0 0 1 * * ?"
  interest-capitalization:
    enabled: true
    cron: "0 0 2 1 1,4,7,10 ?"
  maturity-processing:
    enabled: true
    cron: "0 0 3 * * ?"
```

### Monitoring

Monitor batch execution via logs:
```bash
grep "Batch completed" /var/log/account-service.log
```

Check for errors:
```bash
grep "❌" /var/log/account-service.log
```

### Troubleshooting

**Batch not running?**
1. Check if enabled: `GET /api/accounts/batch/status`
2. Check logs for errors
3. Verify @EnableScheduling is present in main application class
4. Ensure cron expression is valid

**Need to rerun batch?**
1. Use manual trigger endpoint
2. Set time travel date if needed
3. Check execution logs

**Testing in non-prod?**
1. Enable batches via environment variables
2. Use time travel to test different dates
3. Monitor logs for success/error counts
4. Clear time travel before production deployment

---

## 📝 Summary

- ✅ 3 batch jobs implemented: Interest Accrual, Interest Capitalization, Maturity Processing
- ✅ All batches disabled by default (safe for production)
- ✅ Time travel feature for testing at different dates
- ✅ Manual trigger API endpoints (ADMIN only)
- ✅ Comprehensive logging with emojis
- ✅ Per-account error handling
- ✅ No modifications to existing code
- ✅ Fully configurable via application.yml

The batch processing system is production-ready and optional - existing functionality remains unchanged.
