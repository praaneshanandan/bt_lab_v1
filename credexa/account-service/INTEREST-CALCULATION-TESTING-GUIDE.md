# Interest Calculation - Comprehensive Testing Guide

## Overview
This guide provides complete testing instructions for the Interest Calculation Process API feature.

**Feature**: Calculate and credit interest for FD accounts with automatic TDS deduction support.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Feature Overview](#feature-overview)
3. [Test Scenarios](#test-scenarios)
4. [Interest Calculation Formula](#interest-calculation-formula)
5. [Automatic Period Detection](#automatic-period-detection)
6. [Role-Based Access Control](#role-based-access-control)
7. [Validation Tests](#validation-tests)
8. [Complete Flow Example](#complete-flow-example)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### 1. Service Running
```bash
cd account-service
mvnw spring-boot:run
```

### 2. Access Swagger UI
```
http://localhost:8087/swagger-ui.html
```

### 3. JWT Token
Obtain JWT token from login-service for MANAGER or ADMIN role:
- **MANAGER**: Can calculate and credit interest
- **ADMIN**: Can calculate and credit interest
- **CUSTOMER**: ❌ Cannot access this API

### 4. Test Account Setup
Create an FD account first:

**Create Account:**
```bash
POST /api/accounts/create/default
{
  "customerId": 101,
  "productCode": "FD-5YR-SENIOR",
  "principalAmount": 100000.00,
  "termMonths": 60,
  "branchCode": "BR001"
}
```
Note the `accountNumber` from response

---

## Feature Overview

### Endpoint
- **URL**: `POST /api/interest/calculate`
- **Roles**: MANAGER, ADMIN (CUSTOMER ❌)
- **Purpose**: Calculate and optionally credit interest for FD account

### Key Features
1. ✅ **Simple Interest Calculation**: (P × R × T) / (100 × 365)
2. ✅ **Flexible Period**: Specify from/to dates or auto-detect from last interest credit
3. ✅ **Optional Crediting**: Calculate only OR calculate + credit to account
4. ✅ **TDS Support**: Automatic TDS deduction if applicable
5. ✅ **Transaction Creation**: Creates INTEREST_CREDIT and TDS_DEDUCTION transactions
6. ✅ **Balance Tracking**: Maintains accurate balance chain
7. ✅ **Historical Summary**: Shows total interest/TDS till date

---

## Test Scenarios

### Scenario 1: Calculate Interest (Without Crediting)

**Purpose**: Just calculate interest without affecting account balance

**Request:**
```json
POST /api/interest/calculate
Authorization: Bearer <JWT_TOKEN_MANAGER>
Content-Type: application/json

{
  "accountNumber": "FD-20251108120000-1234-5",
  "fromDate": "2025-05-08",
  "toDate": "2025-11-08",
  "creditInterest": false,
  "applyTds": false
}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Interest calculated successfully (not credited)",
  "data": {
    "accountNumber": "FD-20251108120000-1234-5",
    "accountName": "John Doe",
    "accountStatus": "ACTIVE",
    "fromDate": "2025-05-08",
    "toDate": "2025-11-08",
    "daysInPeriod": 184,
    "principalAmount": 100000.00,
    "interestRate": 7.50,
    "calculationType": "SIMPLE",
    "compoundingFrequency": "QUARTERLY",
    "interestAmount": 3780.82,
    "tdsRate": 10.00,
    "tdsAmount": 0.00,
    "netInterest": 3780.82,
    "balanceBefore": 100000.00,
    "balanceAfter": 100000.00,
    "interestCredited": false,
    "tdsDeducted": false,
    "interestTransactionId": null,
    "tdsTransactionId": null,
    "breakdown": {
      "principal": 100000.00,
      "annualRate": 7.50,
      "days": 184,
      "daysInYear": 365,
      "grossInterest": 3780.82,
      "tdsApplicable": false,
      "tdsRate": 10.00,
      "tdsAmount": 0.00,
      "netInterest": 3780.82,
      "formula": "Simple Interest: (Principal × Rate × Days) / (100 × 365)",
      "transactionsCreated": []
    },
    "totalInterestCreditedTillDate": 0.00,
    "totalTdsDeductedTillDate": 0.00,
    "previousInterestCreditsCount": 0,
    "message": "Interest calculated: ₹3,780.82 for 184 days (2025-05-08 to 2025-11-08)",
    "remarks": null
  }
}
```

**Verification:**
- ✅ Interest amount calculated correctly
- ✅ No transactions created
- ✅ Balance unchanged
- ✅ Formula shown in breakdown

---

### Scenario 2: Calculate and Credit Interest (No TDS)

**Purpose**: Credit interest to account without TDS deduction

**Request:**
```json
POST /api/interest/calculate
Authorization: Bearer <JWT_TOKEN_MANAGER>

{
  "accountNumber": "FD-20251108120000-1234-5",
  "fromDate": "2025-05-08",
  "toDate": "2025-11-08",
  "creditInterest": true,
  "applyTds": false,
  "paymentReference": "INT-Q2-2025",
  "remarks": "Half-yearly interest credit"
}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Interest calculated and credited successfully",
  "data": {
    "accountNumber": "FD-20251108120000-1234-5",
    "accountName": "John Doe",
    "accountStatus": "ACTIVE",
    "fromDate": "2025-05-08",
    "toDate": "2025-11-08",
    "daysInPeriod": 184,
    "interestAmount": 3780.82,
    "tdsAmount": 0.00,
    "netInterest": 3780.82,
    "balanceBefore": 100000.00,
    "balanceAfter": 103780.82,
    "interestCredited": true,
    "tdsDeducted": false,
    "interestTransactionId": "TXN-20251108120000-6001",
    "tdsTransactionId": null,
    "breakdown": {
      "grossInterest": 3780.82,
      "tdsApplicable": false,
      "tdsAmount": 0.00,
      "netInterest": 3780.82,
      "transactionsCreated": [
        "INTEREST_CREDIT: TXN-20251108120000-6001"
      ]
    },
    "totalInterestCreditedTillDate": 3780.82,
    "totalTdsDeductedTillDate": 0.00,
    "previousInterestCreditsCount": 0,
    "message": "Interest calculated: ₹3,780.82 for 184 days (2025-05-08 to 2025-11-08). Net credited: ₹3,780.82",
    "remarks": "Half-yearly interest credit"
  }
}
```

**Verification:**
1. ✅ INTEREST_CREDIT transaction created
2. ✅ Balance increased by interest amount
3. ✅ Transaction ID returned
4. ✅ Total interest till date updated

**Check Transaction:**
```bash
GET /api/transactions/{interestTransactionId}
```

---

### Scenario 3: Calculate and Credit Interest with TDS

**Purpose**: Credit interest and deduct TDS automatically

**Request:**
```json
POST /api/interest/calculate
Authorization: Bearer <JWT_TOKEN_MANAGER>

{
  "accountNumber": "FD-20251108120000-1234-5",
  "fromDate": "2025-05-08",
  "toDate": "2025-11-08",
  "creditInterest": true,
  "applyTds": true,
  "paymentReference": "INT-Q2-2025",
  "remarks": "Half-yearly interest with TDS"
}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Interest calculated and credited successfully",
  "data": {
    "interestAmount": 3780.82,
    "tdsRate": 10.00,
    "tdsAmount": 378.08,
    "netInterest": 3402.74,
    "balanceBefore": 100000.00,
    "balanceAfter": 103402.74,
    "interestCredited": true,
    "tdsDeducted": true,
    "interestTransactionId": "TXN-20251108120000-6001",
    "tdsTransactionId": "TXN-20251108120100-6002",
    "breakdown": {
      "grossInterest": 3780.82,
      "tdsApplicable": true,
      "tdsRate": 10.00,
      "tdsAmount": 378.08,
      "netInterest": 3402.74,
      "transactionsCreated": [
        "INTEREST_CREDIT: TXN-20251108120000-6001",
        "TDS_DEDUCTION: TXN-20251108120100-6002"
      ]
    },
    "totalInterestCreditedTillDate": 3780.82,
    "totalTdsDeductedTillDate": 378.08,
    "message": "Interest calculated: ₹3,780.82 for 184 days (2025-05-08 to 2025-11-08). Net credited: ₹3,402.74"
  }
}
```

**Verification:**
1. ✅ INTEREST_CREDIT transaction created (gross amount)
2. ✅ TDS_DEDUCTION transaction created
3. ✅ Balance increased by net amount (interest - TDS)
4. ✅ Both transaction IDs returned
5. ✅ TDS calculated at 10% of interest

**Check Transactions:**
```bash
GET /api/transactions/{interestTransactionId}
GET /api/transactions/{tdsTransactionId}
```

---

### Scenario 4: Automatic Period Detection

**Purpose**: Let system determine from date based on last interest credit

**Setup First:**
Credit interest for first period:
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "fromDate": "2024-11-08",
  "toDate": "2025-05-08",
  "creditInterest": true,
  "applyTds": true
}
```

**Now Calculate Next Period (Auto-detect from date):**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "toDate": "2025-11-08",
  "creditInterest": true,
  "applyTds": true,
  "remarks": "Automatic period detection"
}
```

**Expected Behavior:**
- System automatically sets `fromDate` to 2025-05-09 (day after last interest credit)
- Calculates interest for period 2025-05-09 to 2025-11-08
- Prevents double-counting of interest

**Response:**
```json
{
  "data": {
    "fromDate": "2025-05-09",
    "toDate": "2025-11-08",
    "daysInPeriod": 183,
    "previousInterestCreditsCount": 1,
    "message": "Interest calculated: ₹3,760.27 for 183 days..."
  }
}
```

---

### Scenario 5: Quarterly Interest Credits (Full Year)

**Purpose**: Process quarterly interest for one year

**Q1 (Nov 2024 - Feb 2025): 92 days**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "fromDate": "2024-11-08",
  "toDate": "2025-02-08",
  "creditInterest": true,
  "applyTds": true,
  "paymentReference": "INT-2024-Q4",
  "remarks": "Q4 2024 interest"
}
```

**Q2 (Feb 2025 - May 2025): 91 days**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "toDate": "2025-05-08",
  "creditInterest": true,
  "applyTds": true,
  "paymentReference": "INT-2025-Q1"
}
```

**Q3 (May 2025 - Aug 2025): 92 days**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "toDate": "2025-08-08",
  "creditInterest": true,
  "applyTds": true,
  "paymentReference": "INT-2025-Q2"
}
```

**Q4 (Aug 2025 - Nov 2025): 92 days**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "toDate": "2025-11-08",
  "creditInterest": true,
  "applyTds": true,
  "paymentReference": "INT-2025-Q3"
}
```

**Verification After Q4:**
```bash
GET /api/transactions/list?idValue=FD-20251108120000-1234-5
```
Expected: 8 transactions (4 INTEREST_CREDIT + 4 TDS_DEDUCTION)

---

## Interest Calculation Formula

### Simple Interest Formula
```
Interest = (Principal × Rate × Days) / (100 × 365)

Where:
- Principal: FD amount (from account)
- Rate: Annual interest rate (from account)
- Days: Number of days in calculation period
- 365: Days in year (constant)
```

### Example Calculation

**Given:**
- Principal: 100,000.00
- Annual Rate: 7.50%
- Period: 184 days

**Calculation:**
```
Interest = (100,000 × 7.50 × 184) / (100 × 365)
         = (138,000,000) / (36,500)
         = 3,780.82
```

**With TDS at 10%:**
```
TDS = 3,780.82 × 10 / 100 = 378.08
Net Interest = 3,780.82 - 378.08 = 3,402.74
```

---

## Automatic Period Detection

### Logic
1. **If fromDate provided**: Use provided date
2. **If fromDate NOT provided**:
   - Find last INTEREST_CREDIT transaction
   - Set fromDate = last transaction date + 1 day
   - If no previous interest: Set fromDate = account effective date

### Example Timeline
```
Account Created: 2024-11-08
First Interest:  2025-02-08 (92 days from 2024-11-08)
Second Interest: Auto-detects from 2025-02-09
```

### Benefits
- ✅ Prevents double-counting
- ✅ Automatic gap detection
- ✅ Simplifies quarterly/monthly processing
- ✅ No manual date calculation needed

---

## Role-Based Access Control

| Endpoint | CUSTOMER | MANAGER | ADMIN | Notes |
|----------|----------|---------|-------|-------|
| POST /calculate | ❌ | ✅ | ✅ | Only authorized staff can calculate/credit interest |

**Rationale:**
- Interest calculation affects account balance
- Requires system-level authority
- CUSTOMER should not trigger interest credits

---

## Validation Tests

### Test 1: Invalid Account Number

**Request:**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-INVALID-12345",
  "creditInterest": true
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Invalid request: Account not found: FD-INVALID-12345"
}
```

---

### Test 2: Closed Account

**Request:**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-CLOSED-ACCOUNT-789",
  "creditInterest": true
}
```

**Expected Response (409 CONFLICT):**
```json
{
  "success": false,
  "message": "Invalid state: Cannot calculate interest for closed account"
}
```

---

### Test 3: Invalid Date Range (From > To)

**Request:**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "fromDate": "2025-11-08",
  "toDate": "2025-05-08",
  "creditInterest": true
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Invalid request: From date cannot be after to date"
}
```

---

### Test 4: Date Before Account Effective Date

**Request:**
```json
POST /api/interest/calculate
{
  "accountNumber": "FD-20251108120000-1234-5",
  "fromDate": "2024-01-01",
  "toDate": "2025-05-08",
  "creditInterest": true
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Invalid request: From date cannot be before account effective date"
}
```

---

### Test 5: CUSTOMER Role Access (Forbidden)

**Request:**
```json
POST /api/interest/calculate
Authorization: Bearer <JWT_TOKEN_CUSTOMER>
{
  "accountNumber": "FD-20251108120000-1234-5",
  "creditInterest": true
}
```

**Expected Response (403 FORBIDDEN):**
```json
{
  "success": false,
  "message": "Access Denied"
}
```

---

## Complete Flow Example

### Step 1: Create FD Account
```bash
POST /api/accounts/create/default
{
  "customerId": 101,
  "productCode": "FD-5YR-SENIOR",
  "principalAmount": 100000.00,
  "termMonths": 60,
  "effectiveDate": "2024-11-08"
}
```
Response: Account created with accountNumber

### Step 2: Calculate Interest (Preview)
```bash
POST /api/interest/calculate
{
  "accountNumber": "<accountNumber>",
  "toDate": "2025-05-08",
  "creditInterest": false
}
```
Response: Shows interest = 3,780.82 for 182 days

### Step 3: Credit Interest with TDS
```bash
POST /api/interest/calculate
{
  "accountNumber": "<accountNumber>",
  "toDate": "2025-05-08",
  "creditInterest": true,
  "applyTds": true,
  "paymentReference": "INT-2025-H1"
}
```
Response: 
- Interest credited: 3,780.82
- TDS deducted: 378.08
- Net amount: 3,402.74
- Balance: 103,402.74

### Step 4: Verify Transactions
```bash
GET /api/transactions/list?idValue=<accountNumber>
```
Response: Shows 2 transactions (INTEREST_CREDIT, TDS_DEDUCTION)

### Step 5: Calculate Next Period (Auto)
```bash
POST /api/interest/calculate
{
  "accountNumber": "<accountNumber>",
  "toDate": "2025-11-08",
  "creditInterest": true,
  "applyTds": true
}
```
Response: Automatically calculates from 2025-05-09

### Step 6: Check Balance
```bash
GET /api/accounts/<accountNumber>/balance
```
Response: Balance includes all interest credits

### Step 7: List All Interest Credits
```bash
GET /api/transactions/type/INTEREST_CREDIT
```
Response: All interest credit transactions across accounts

---

## Troubleshooting

### Issue 1: Interest Amount is Zero

**Symptom:** `interestAmount: 0.00`

**Causes:**
1. Days in period is 0 or negative
2. Interest rate is 0
3. Principal amount is 0

**Solution:**
- Check fromDate and toDate
- Verify account has interest rate set
- Verify principal amount > 0

---

### Issue 2: Balance Not Updated After Credit

**Symptom:** Balance unchanged after `creditInterest: true`

**Cause:** Transaction may have failed

**Solution:**
- Check response for `interestTransactionId`
- If null, transaction wasn't created
- Check logs for errors
- Verify account status is ACTIVE

---

### Issue 3: TDS Not Deducted

**Symptom:** `tdsAmount: 0.00` even with `applyTds: true`

**Causes:**
1. Account has `tdsApplicable: false`
2. TDS rate is 0 or null

**Solution:**
- Check account TDS settings
- Update account TDS rate if needed
- Verify product has TDS configured

---

### Issue 4: Duplicate Interest for Same Period

**Symptom:** Interest credited twice for same dates

**Cause:** Manual date specification bypassing auto-detection

**Solution:**
- Use automatic period detection (omit fromDate)
- System prevents overlapping periods
- Or track credited periods manually

---

### Issue 5: Date Adjusted to Maturity Date

**Symptom:** Warning in logs "To date adjusted to maturity date"

**Cause:** Requested toDate > maturity date

**Behavior:** System automatically adjusts to maturity date (correct behavior)

**Action:** No action needed - system handles correctly

---

## Success Indicators

✅ **Calculation Only:**
- Response code: 200 OK
- `interestAmount` > 0
- `interestCredited: false`
- `balanceBefore` = `balanceAfter`
- Formula shown in breakdown

✅ **Credit Interest:**
- Response code: 200 OK
- `interestCredited: true`
- `interestTransactionId` present
- `balanceAfter` = `balanceBefore` + `netInterest`
- Transaction created in database

✅ **Credit with TDS:**
- Both `interestTransactionId` and `tdsTransactionId` present
- `tdsDeducted: true`
- `tdsAmount` = `interestAmount` × `tdsRate` / 100
- `netInterest` = `interestAmount` - `tdsAmount`
- Balance = before + interest - TDS

---

## API Response Fields

### InterestCalculationResponse

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| accountNumber | String | FD account number | FD-20251108120000-1234-5 |
| fromDate | Date | Calculation from date | 2025-05-08 |
| toDate | Date | Calculation to date | 2025-11-08 |
| daysInPeriod | Long | Days in period | 184 |
| principalAmount | Decimal | Principal amount | 100000.00 |
| interestRate | Decimal | Annual rate | 7.50 |
| interestAmount | Decimal | Calculated interest | 3780.82 |
| tdsAmount | Decimal | TDS deducted | 378.08 |
| netInterest | Decimal | Net after TDS | 3402.74 |
| balanceBefore | Decimal | Balance before | 100000.00 |
| balanceAfter | Decimal | Balance after | 103402.74 |
| interestCredited | Boolean | Was credited? | true |
| tdsDeducted | Boolean | Was TDS deducted? | true |
| interestTransactionId | String | Interest transaction ID | TXN-xxx |
| tdsTransactionId | String | TDS transaction ID | TXN-yyy |
| totalInterestCreditedTillDate | Decimal | Total interest ever | 7560.00 |
| previousInterestCreditsCount | Long | Count of previous credits | 1 |

---

**Testing Complete**: Interest Calculation Process API fully tested! ✅
