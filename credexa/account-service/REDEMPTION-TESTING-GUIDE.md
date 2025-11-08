# Redemption Features - Comprehensive Testing Guide

## Overview
This guide provides complete testing instructions for the FD Redemption features:
- **Feature 11**: Redemption Inquiry - API/UI
- **Feature 12**: Redemption Process - API/UI

Both features support flexible account identification using:
- `ACCOUNT_NUMBER` (default)
- `IBAN`
- `INTERNAL_ID`

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Feature 11: Redemption Inquiry](#feature-11-redemption-inquiry)
3. [Feature 12: Redemption Process](#feature-12-redemption-process)
4. [Complete Redemption Flow](#complete-redemption-flow)
5. [Redemption Calculation Logic](#redemption-calculation-logic)
6. [Redemption Types](#redemption-types)
7. [Penalty Calculation](#penalty-calculation)
8. [Role-Based Access Control](#role-based-access-control)
9. [Validation Tests](#validation-tests)
10. [Troubleshooting](#troubleshooting)

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
Obtain JWT token from login-service for appropriate role:
- **CUSTOMER**: Can inquire redemption details
- **MANAGER**: Can inquire and process redemption
- **ADMIN**: Can inquire and process redemption with override

### 4. Test Account Setup
Create an FD account and perform some transactions first:

**Step 1: Create Account**
```bash
POST /api/accounts/create
{
  "customerId": 101,
  "productCode": "FD-5YR-SENIOR",
  "principalAmount": 100000.00,
  "termMonths": 60,
  "branchCode": "BR001"
}
```
Response will include: `accountNumber`, `ibanNumber`, `id`

**Step 2: Create Interest Credit Transaction**
```bash
POST /api/transactions/create?idValue=FD-20251108120000-1234-5
{
  "transactionType": "INTEREST_CREDIT",
  "amount": 7500.00,
  "description": "6 months interest"
}
```

**Step 3: Create TDS Deduction Transaction**
```bash
POST /api/transactions/create?idValue=FD-20251108120000-1234-5
{
  "transactionType": "TDS_DEDUCTION",
  "amount": 750.00,
  "description": "TDS on interest"
}
```

---

## Feature 11: Redemption Inquiry

### Endpoint Details
- **URL**: `POST /api/redemptions/inquiry`
- **Roles**: CUSTOMER, MANAGER, ADMIN
- **Purpose**: Get complete redemption calculation details

### Test Case 1: Inquiry Using ACCOUNT_NUMBER (Default)

**Request:**
```json
POST /api/redemptions/inquiry
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "idValue": "FD-20251108120000-1234-5"
}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Redemption inquiry retrieved successfully",
  "data": {
    "accountId": 1,
    "accountNumber": "FD-20251108120000-1234-5",
    "ibanNumber": "IN29CRED0001FD2511081234",
    "accountName": "John Doe",
    "accountStatus": "ACTIVE",
    "customerId": 101,
    "customerName": "John Doe",
    "productCode": "FD-5YR-SENIOR",
    "productName": "5 Year Senior Citizen FD",
    "principalAmount": 100000.00,
    "interestRate": 7.50,
    "termMonths": 60,
    "maturityAmount": 143965.00,
    "effectiveDate": "2023-11-08",
    "maturityDate": "2028-11-08",
    "inquiryDate": "2025-11-08",
    "daysElapsed": 730,
    "daysRemaining": 1095,
    "monthsElapsed": 24,
    "monthsRemaining": 36,
    "isMatured": false,
    "currentBalance": 100000.00,
    "interestEarned": 7500.00,
    "tdsDeducted": 750.00,
    "penaltyAmount": 37.50,
    "netRedemptionAmount": 106712.50,
    "penaltyApplicable": true,
    "penaltyRate": 0.50,
    "penaltyDescription": "Premature redemption penalty: 0.5% on interest earned",
    "tdsRate": 10.00,
    "tdsApplicable": true,
    "totalTransactions": 2,
    "interestCreditCount": 1,
    "tdsDeductionCount": 1,
    "redemptionType": "PREMATURE",
    "remarks": "Premature redemption - penalty applicable",
    "branchCode": "BR001",
    "branchName": "Main Branch"
  }
}
```

### Test Case 2: Inquiry Using IBAN

**Request:**
```json
POST /api/redemptions/inquiry
Authorization: Bearer <JWT_TOKEN>

{
  "idType": "IBAN",
  "idValue": "IN29CRED0001FD2511081234"
}
```

**Expected Response:** Same as Test Case 1 (200 OK)

### Test Case 3: Inquiry Using INTERNAL_ID

**Request:**
```json
POST /api/redemptions/inquiry
Authorization: Bearer <JWT_TOKEN>

{
  "idType": "INTERNAL_ID",
  "idValue": "1"
}
```

**Expected Response:** Same as Test Case 1 (200 OK)

### Test Case 4: Inquiry for Matured FD

For an FD where current date >= maturity date:

**Expected Response Changes:**
```json
{
  "isMatured": true,
  "daysRemaining": 0,
  "monthsRemaining": 0,
  "penaltyApplicable": false,
  "penaltyAmount": 0.00,
  "penaltyRate": 0.00,
  "penaltyDescription": null,
  "netRedemptionAmount": 106750.00,  // Higher (no penalty)
  "redemptionType": "ON_MATURITY",
  "remarks": "Redemption on or after maturity - no penalty"
}
```

### Test Case 5: Inquiry for Closed Account (Error)

**Request:**
```json
POST /api/redemptions/inquiry

{
  "idValue": "FD-CLOSED-ACCOUNT-123"
}
```

**Expected Response (409 CONFLICT):**
```json
{
  "success": false,
  "message": "Invalid state: Account is already closed. Cannot perform redemption inquiry.",
  "data": null
}
```

---

## Feature 12: Redemption Process

### Endpoint Details
- **URL**: `POST /api/redemptions/process`
- **Roles**: MANAGER, ADMIN (CUSTOMER cannot process)
- **Purpose**: Process full or partial redemption

### Test Case 1: Full Redemption Using ACCOUNT_NUMBER

**Request:**
```json
POST /api/redemptions/process
Authorization: Bearer <JWT_TOKEN_MANAGER>
Content-Type: application/json

{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "FULL",
  "paymentReference": "PAY-2025110812345",
  "remarks": "Customer requested full redemption",
  "channel": "BRANCH",
  "branchCode": "BR001"
}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Redemption processed successfully. Transaction ID: TXN-20251108120000-5001",
  "data": {
    "redemptionTransactionId": "TXN-20251108120000-5001",
    "redemptionStatus": "COMPLETED",
    "redemptionType": "FULL",
    "accountNumber": "FD-20251108120000-1234-5",
    "accountName": "John Doe",
    "accountStatus": "CLOSED",
    "principalAmount": 100000.00,
    "interestEarned": 7500.00,
    "tdsDeducted": 750.00,
    "penaltyAmount": 37.50,
    "grossRedemptionAmount": 107500.00,
    "netRedemptionAmount": 106712.50,
    "balanceAfter": 0.00,
    "paymentReference": "PAY-2025110812345",
    "redemptionDate": "2025-11-08T12:00:00",
    "processedBy": "manager1",
    "channel": "BRANCH",
    "branchCode": "BR001",
    "breakdown": {
      "balanceBefore": 100000.00,
      "interestAmount": 7500.00,
      "tdsAmount": 750.00,
      "penaltyAmount": 37.50,
      "netAmount": 106712.50,
      "penaltyApplicable": true,
      "penaltyReason": "Premature redemption penalty: 0.5% on interest earned"
    },
    "remarks": "Customer requested full redemption",
    "message": "Redemption processed successfully. Net amount: ₹106,712.50"
  }
}
```

**Verify:**
1. Check account status changed to CLOSED
2. Check closure date is set
3. Check transaction created with type CLOSURE
4. Check balance after is 0.00

### Test Case 2: Partial Redemption Using IBAN

**Request:**
```json
POST /api/redemptions/process
Authorization: Bearer <JWT_TOKEN_MANAGER>

{
  "idType": "IBAN",
  "idValue": "IN29CRED0001FD2511081234",
  "redemptionType": "PARTIAL",
  "redemptionAmount": 50000.00,
  "paymentReference": "PAY-PARTIAL-001",
  "remarks": "Partial withdrawal for emergency",
  "channel": "INTERNET_BANKING"
}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "message": "Redemption processed successfully. Transaction ID: TXN-20251108120500-5002",
  "data": {
    "redemptionTransactionId": "TXN-20251108120500-5002",
    "redemptionStatus": "COMPLETED",
    "redemptionType": "PARTIAL",
    "accountNumber": "FD-20251108120000-1234-5",
    "accountName": "John Doe",
    "accountStatus": "ACTIVE",
    "principalAmount": 100000.00,
    "interestEarned": 7500.00,
    "tdsDeducted": 750.00,
    "penaltyAmount": 37.50,
    "grossRedemptionAmount": 107500.00,
    "netRedemptionAmount": 50000.00,
    "balanceAfter": 56712.50,
    "paymentReference": "PAY-PARTIAL-001",
    "redemptionDate": "2025-11-08T12:05:00",
    "processedBy": "manager1",
    "channel": "INTERNET_BANKING",
    "branchCode": "BR001",
    "breakdown": {
      "balanceBefore": 100000.00,
      "interestAmount": 7500.00,
      "tdsAmount": 750.00,
      "penaltyAmount": 37.50,
      "netAmount": 106712.50,
      "penaltyApplicable": true,
      "penaltyReason": "Premature redemption penalty: 0.5% on interest earned"
    },
    "remarks": "Partial withdrawal for emergency",
    "message": "Redemption processed successfully. Net amount: ₹50,000.00"
  }
}
```

**Verify:**
1. Account status remains ACTIVE
2. Balance after = 56712.50 (106712.50 - 50000.00)
3. Transaction type is WITHDRAWAL
4. Closure date is NOT set

### Test Case 3: Full Redemption on Maturity (No Penalty)

**Setup:** Wait until maturity date or test with matured account

**Request:**
```json
POST /api/redemptions/process
Authorization: Bearer <JWT_TOKEN_MANAGER>

{
  "idValue": "FD-MATURED-ACCOUNT-789",
  "redemptionType": "FULL",
  "paymentReference": "PAY-MATURITY-001",
  "remarks": "Maturity redemption"
}
```

**Expected Response:**
```json
{
  "data": {
    "penaltyAmount": 0.00,
    "netRedemptionAmount": 143965.00,
    "accountStatus": "CLOSED",
    "breakdown": {
      "penaltyApplicable": false,
      "penaltyReason": null
    }
  }
}
```

### Test Case 4: CUSTOMER Role Attempting Process (Error)

**Request:**
```json
POST /api/redemptions/process
Authorization: Bearer <JWT_TOKEN_CUSTOMER>

{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "FULL"
}
```

**Expected Response (403 FORBIDDEN):**
```json
{
  "success": false,
  "message": "Access Denied",
  "data": null
}
```

### Test Case 5: Partial Redemption Below Minimum Balance (Error)

**Request:**
```json
POST /api/redemptions/process
Authorization: Bearer <JWT_TOKEN_MANAGER>

{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "PARTIAL",
  "redemptionAmount": 100000.00
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Invalid request: Remaining balance after redemption would be below minimum required balance. Minimum: 10000.00, Remaining would be: 6712.50",
  "data": null
}
```

---

## Complete Redemption Flow

### Scenario: Complete FD Lifecycle with Redemption

**Step 1: Create Account**
```bash
POST /api/accounts/create
{
  "customerId": 101,
  "productCode": "FD-5YR-SENIOR",
  "principalAmount": 100000.00,
  "termMonths": 60
}
```
Note: `accountNumber` from response

**Step 2: Add Interest Credit**
```bash
POST /api/transactions/create?idValue=<accountNumber>
{
  "transactionType": "INTEREST_CREDIT",
  "amount": 3750.00,
  "description": "Q1 Interest"
}
```

**Step 3: Add TDS Deduction**
```bash
POST /api/transactions/create?idValue=<accountNumber>
{
  "transactionType": "TDS_DEDUCTION",
  "amount": 375.00,
  "description": "TDS Q1"
}
```

**Step 4: Check Redemption Inquiry**
```bash
POST /api/redemptions/inquiry
{
  "idValue": "<accountNumber>"
}
```
Note: `netRedemptionAmount`, `penaltyAmount`

**Step 5: Process Full Redemption**
```bash
POST /api/redemptions/process
{
  "idValue": "<accountNumber>",
  "redemptionType": "FULL",
  "paymentReference": "PAY-001"
}
```
Note: `redemptionTransactionId`, account status becomes CLOSED

**Step 6: Verify Account Closed**
```bash
GET /api/accounts/inquiry?idValue=<accountNumber>
```
Expected: `status: CLOSED`, `closureDate` set

**Step 7: List All Transactions**
```bash
GET /api/transactions/list?idValue=<accountNumber>
```
Expected: Shows all transactions including CLOSURE

---

## Redemption Calculation Logic

### Net Redemption Amount Formula

```
Current Balance = Last Transaction Balance After (or Principal if no transactions)

Interest Earned = SUM(all INTEREST_CREDIT transactions)

TDS Deducted = SUM(all TDS_DEDUCTION transactions)

Penalty Amount = Interest Earned × Penalty Rate (if premature)

Net Redemption = Current Balance + Interest Earned - TDS Deducted - Penalty Amount
```

### Example Calculation

**Given:**
- Principal: 100,000.00
- Interest Earned: 7,500.00
- TDS Deducted: 750.00
- Premature Redemption Penalty: 0.5% on interest

**Calculation:**
```
Current Balance     = 100,000.00
Interest Earned     =   7,500.00
TDS Deducted        =     750.00
Penalty (0.5%)      =      37.50  (7500 × 0.005)

Net Redemption      = 100,000 + 7,500 - 750 - 37.50
                    = 106,712.50
```

---

## Redemption Types

### 1. PREMATURE Redemption
- **Condition**: Current date < Maturity date
- **Penalty**: Yes (0.5% on interest earned)
- **Example**: Withdrawing after 2 years of 5-year FD

### 2. ON_MATURITY Redemption
- **Condition**: Current date = Maturity date
- **Penalty**: No
- **Example**: Withdrawing on exact maturity date

### 3. POST_MATURITY Redemption
- **Condition**: Current date > Maturity date
- **Penalty**: No
- **Example**: Withdrawing 1 month after maturity
- **Note**: May earn post-maturity interest at lower rate

---

## Penalty Calculation

### Penalty Rules

| Redemption Type | Penalty Applicable | Rate | Applied On |
|----------------|-------------------|------|------------|
| PREMATURE | Yes | 0.5% | Interest Earned |
| ON_MATURITY | No | 0% | - |
| POST_MATURITY | No | 0% | - |

### Penalty Formula
```
Penalty Amount = Interest Earned × (Penalty Rate / 100)

Example:
Interest Earned = 7,500.00
Penalty Rate = 0.50%
Penalty = 7,500.00 × 0.005 = 37.50
```

---

## Role-Based Access Control

| Endpoint | CUSTOMER | MANAGER | ADMIN | Notes |
|----------|----------|---------|-------|-------|
| POST /inquiry | ✅ | ✅ | ✅ | All can view redemption details |
| POST /process (FULL) | ❌ | ✅ | ✅ | Only authorized personnel can process |
| POST /process (PARTIAL) | ❌ | ✅ | ✅ | Withdrawal requires approval |

---

## Validation Tests

### Test 1: Invalid Account Number

**Request:**
```json
POST /api/redemptions/inquiry
{
  "idValue": "FD-INVALID-12345"
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Invalid request: Account not found with account number: FD-INVALID-12345"
}
```

### Test 2: Missing Redemption Amount for PARTIAL

**Request:**
```json
POST /api/redemptions/process
{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "PARTIAL"
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Invalid request: Redemption amount is required for partial redemption"
}
```

### Test 3: Negative Redemption Amount

**Request:**
```json
POST /api/redemptions/process
{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "PARTIAL",
  "redemptionAmount": -5000.00
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Validation failed: redemptionAmount must be greater than 0"
}
```

### Test 4: Already Closed Account

**Request:**
```json
POST /api/redemptions/process
{
  "idValue": "FD-CLOSED-ACCOUNT-123",
  "redemptionType": "FULL"
}
```

**Expected Response (409 CONFLICT):**
```json
{
  "success": false,
  "message": "Invalid state: Account is already closed. Cannot process redemption."
}
```

### Test 5: Exceeding Available Balance

**Request:**
```json
POST /api/redemptions/process
{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "PARTIAL",
  "redemptionAmount": 200000.00
}
```

**Expected Response (400 BAD REQUEST):**
```json
{
  "success": false,
  "message": "Invalid request: Redemption amount exceeds available balance. Max available: 106712.50"
}
```

---

## Troubleshooting

### Issue 1: Penalty Amount Not Calculated

**Symptom:** `penaltyAmount` is always 0.00

**Causes:**
1. Account has reached maturity
2. No interest credits recorded
3. Inquiry date >= maturity date

**Solution:**
- Check `redemptionType` in response
- Verify `isMatured` flag
- Check interest credit transactions exist

### Issue 2: Minimum Balance Error on Partial Redemption

**Symptom:** Error "below minimum required balance"

**Cause:** Remaining balance would be < 10% of principal

**Solution:**
- Calculate: `netRedemptionAmount - redemptionAmount >= principal × 0.10`
- For 100,000 principal, minimum remaining = 10,000
- Adjust redemption amount or use FULL redemption

### Issue 3: Account Not Closing After Full Redemption

**Symptom:** Account status still ACTIVE after full redemption

**Causes:**
1. Used PARTIAL instead of FULL
2. Transaction failed silently
3. Database transaction rolled back

**Solution:**
- Check `redemptionType` in request
- Verify response shows `accountStatus: CLOSED`
- Check `closureDate` is set
- Query account again to confirm status

### Issue 4: TDS Not Reflected in Calculation

**Symptom:** TDS amount not deducted from net redemption

**Cause:** No TDS deduction transactions recorded

**Solution:**
- Create TDS_DEDUCTION transactions first
- TDS is automatically summed from transactions
- Check `tdsDeductionCount` in inquiry response

### Issue 5: 403 Forbidden on Process

**Symptom:** CUSTOMER role getting access denied

**Cause:** Only MANAGER/ADMIN can process redemption

**Solution:**
- Use JWT token with MANAGER or ADMIN role
- CUSTOMER can only inquire, not process

---

## Success Indicators

✅ **Redemption Inquiry Success:**
- Response code: 200 OK
- `netRedemptionAmount` calculated correctly
- Penalty applied for premature redemption
- TDS and interest reflected accurately
- Transaction counts match actual transactions

✅ **Full Redemption Success:**
- Response code: 200 OK
- `accountStatus` = CLOSED
- `balanceAfter` = 0.00
- `closureDate` is set
- CLOSURE transaction created
- Net amount matches inquiry calculation

✅ **Partial Redemption Success:**
- Response code: 200 OK
- `accountStatus` = ACTIVE (remains active)
- `balanceAfter` > minimum balance
- WITHDRAWAL transaction created
- Balance correctly reduced

---

## API Response Fields

### Redemption Inquiry Response Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| accountNumber | String | FD account number | FD-20251108120000-1234-5 |
| netRedemptionAmount | Decimal | Final amount payable | 106712.50 |
| interestEarned | Decimal | Total interest credited | 7500.00 |
| tdsDeducted | Decimal | Total TDS deducted | 750.00 |
| penaltyAmount | Decimal | Penalty charged | 37.50 |
| penaltyApplicable | Boolean | Is penalty applicable? | true |
| redemptionType | String | PREMATURE/ON_MATURITY/POST_MATURITY | PREMATURE |
| isMatured | Boolean | Has FD matured? | false |
| daysElapsed | Long | Days since start | 730 |
| daysRemaining | Long | Days until maturity | 1095 |

### Redemption Process Response Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| redemptionTransactionId | String | Transaction ID | TXN-20251108120000-5001 |
| redemptionStatus | String | Transaction status | COMPLETED |
| redemptionType | String | FULL or PARTIAL | FULL |
| accountStatus | String | Account status after redemption | CLOSED |
| netRedemptionAmount | Decimal | Amount paid out | 106712.50 |
| balanceAfter | Decimal | Remaining balance | 0.00 |
| breakdown | Object | Detailed calculation | {...} |

---

## Additional Notes

### Partial Redemption Rules
1. Minimum redemption: 1,000.00
2. Minimum remaining balance: 10% of principal
3. For 100,000 principal: minimum remaining = 10,000

### Full Redemption Effects
1. Account status → CLOSED
2. Closure date set to redemption date
3. Balance becomes 0.00
4. No further transactions allowed

### Post-Redemption Verification
After redemption, verify:
1. Transaction created (CLOSURE or WITHDRAWAL)
2. Account status updated
3. Balance updated correctly
4. Closure date set (for FULL)
5. Cannot perform further transactions on closed accounts

---

**Testing Complete**: All redemption features tested successfully! ✅
