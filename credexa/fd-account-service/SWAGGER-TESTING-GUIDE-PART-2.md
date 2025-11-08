# FD Account Service - Complete Swagger Testing Guide (Part 2)

**Service:** Fixed Deposit Account Service  
**Base URL:** `http://localhost:8086/api/fd-accounts`  
**Swagger UI:** `http://localhost:8086/swagger-ui.html`

---

## Table of Contents - Part 2
1. [Batch Operations](#batch-operations)
2. [FD Reporting](#fd-reporting)
3. [Role Management](#role-management)
4. [Complete Testing Workflows](#complete-testing-workflows)

---

## Batch Operations

**All batch endpoints require MANAGER or ADMIN role**

### 1. POST /batch/interest-calculation - Trigger Interest Calculation Batch

**Purpose:** Manually trigger daily interest calculation for all active accounts

**Access:** 🔐 MANAGER, ADMIN only

**How It Works:**
1. Fetches all ACTIVE accounts from database
2. For each account:
   - Calculates daily interest: `(Principal × Rate × 1) / (365 × 100)`
   - Credits interest to interest balance
   - Creates INTEREST_CREDIT transaction
   - Publishes `InterestAccruedEvent` to Kafka
3. Returns summary of processed accounts

**Request:**
```
POST /batch/interest-calculation
Authorization: Bearer <manager_token>
```

**No Request Body Required**

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Interest calculation batch completed successfully",
  "accountsProcessed": 125,
  "totalInterestCredited": 15250.75,
  "failedAccounts": 0,
  "errors": [],
  "durationMs": 3450,
  "timestamp": "2025-11-08T01:00:00"
}
```

**Interest Calculation Example:**
```
Account: FD20251108001
Principal: ₹50,000
Interest Rate: 6.5% per annum
Daily Interest = (50000 × 6.5 × 1) / (365 × 100) = ₹8.90 per day

After 30 days: 30 × 8.90 = ₹267.00
After 180 days: 180 × 8.90 = ₹1,602.00
After 365 days: 365 × 8.90 = ₹3,248.50
```

**When to Use:**
- **Scheduled:** Automatically runs at 1:00 AM daily (Spring @Scheduled)
- **Manual:** End of day if scheduled batch failed
- **Testing:** After creating accounts to simulate interest accrual

**Testing Scenarios:**

**✅ MANAGER - Trigger Interest Calculation**
```
POST /batch/interest-calculation
Authorization: Bearer <manager_token>

Expected: 200 OK - Interest credited to all active accounts
```

**✅ ADMIN - Run Batch and Verify Results**
```
1. POST /batch/interest-calculation
2. GET /accounts/FD20251108001
3. Verify interestBalance increased by daily interest amount
```

**❌ CUSTOMER - Try to Trigger Batch**
```
POST /batch/interest-calculation
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
```

**Error Scenarios:**
```json
{
  "status": "PARTIAL_SUCCESS",
  "message": "Interest calculation completed with errors",
  "accountsProcessed": 120,
  "failedAccounts": 5,
  "errors": [
    {
      "accountNumber": "FD20251108001",
      "error": "Account is suspended"
    }
  ]
}
```

---

### 2. POST /batch/maturity-processing - Trigger Maturity Processing Batch

**Purpose:** Process all accounts that reached maturity date

**Access:** 🔐 MANAGER, ADMIN only

**How It Works:**
1. Fetches all accounts where `maturityDate = today` AND `status = ACTIVE`
2. For each matured account:
   - Reads maturity instruction
   - Executes appropriate action based on instruction
   - Updates account status
   - Creates maturity transactions
   - Publishes `MaturityProcessedEvent`

**Maturity Instructions:**

**a) CLOSE_AND_PAYOUT (Default):**
```
1. Calculate total payout = principal + interest
2. Create MATURITY_PAYOUT transaction
3. Update account status to CLOSED
4. Send payout confirmation to customer
5. Publish AccountClosedEvent
```

**b) RENEW_PRINCIPAL_ONLY:**
```
1. Payout accumulated interest to savings account
2. Create new FD with same principal amount
3. Use current product rates (may differ from original)
4. Reset interest balance to 0
5. New maturity date = today + term
6. Publish AccountRenewedEvent
```

**c) RENEW_WITH_INTEREST:**
```
1. New principal = old principal + accumulated interest
2. Create new FD with increased principal
3. Use current product rates
4. Reset interest balance to 0
5. New maturity date = today + term
6. Publish AccountRenewedEvent with capitalized interest
```

**d) TRANSFER_TO_SAVINGS / TRANSFER_TO_CURRENT:**
```
1. Calculate total payout
2. Transfer to specified savings/current account
3. Create MATURITY_TRANSFER transaction
4. Close FD account
5. Send transfer confirmation
```

**e) HOLD:**
```
1. Do not process automatically
2. Wait for customer instruction
3. Account remains ACTIVE
4. Send maturity notification to customer
```

**Request:**
```
POST /batch/maturity-processing
Authorization: Bearer <admin_token>
```

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Maturity processing batch completed successfully",
  "accountsMatured": 15,
  "summary": {
    "closedAndPaidout": 10,
    "renewedPrincipalOnly": 3,
    "renewedWithInterest": 2,
    "transferredToSavings": 0,
    "held": 0
  },
  "totalPayoutAmount": 1250000.00,
  "totalRenewedAmount": 550000.00,
  "failedAccounts": 0,
  "errors": [],
  "durationMs": 2100,
  "timestamp": "2025-11-08T01:30:00"
}
```

**Maturity Processing Example:**
```
Account: FD20251108001
Principal: ₹50,000
Accumulated Interest: ₹3,250
Maturity Date: 2025-11-08 (today)
Instruction: CLOSE_AND_PAYOUT

Processing:
1. Total Payout = 50000 + 3250 = ₹53,250
2. Create MATURITY_PAYOUT transaction
3. Update status: ACTIVE → CLOSED
4. Transfer ₹53,250 to customer's savings account
5. Send SMS: "Your FD A/C FD20251108001 matured. ₹53,250 credited to your savings account."
```

**Testing Scenarios:**

**✅ MANAGER - Process Maturity**
```
Setup:
1. Create account with maturity date = tomorrow
2. Fast-forward time or manually set maturity date to today
3. POST /batch/maturity-processing

Expected: 200 OK - Account processed per instruction
```

**✅ ADMIN - Verify Different Instructions**
```
Create 5 accounts with different maturity instructions:
1. CLOSE_AND_PAYOUT → Verify account closed, funds paid out
2. RENEW_PRINCIPAL_ONLY → Verify new account created, interest paid
3. RENEW_WITH_INTEREST → Verify new account with increased principal
4. TRANSFER_TO_SAVINGS → Verify funds transferred, account closed
5. HOLD → Verify account stays active, notification sent
```

**❌ CUSTOMER - Try to Trigger**
```
POST /batch/maturity-processing
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
```

---

### 3. POST /batch/maturity-notice - Trigger Maturity Notice Batch

**Purpose:** Send advance maturity notifications to customers (7 days before maturity)

**Access:** 🔐 MANAGER, ADMIN only

**How It Works:**
1. Fetches accounts maturing in next 7 days
2. For each account:
   - Fetches customer contact details from customer-service
   - Sends SMS notification
   - Sends email notification
   - Logs notification status
3. Returns summary of notifications sent

**Request:**
```
POST /batch/maturity-notice
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Maturity notice batch completed successfully",
  "noticesSent": 25,
  "smsSent": 25,
  "emailsSent": 23,
  "noticesFailed": 2,
  "errors": [
    {
      "accountNumber": "FD20251108001",
      "error": "Customer email not found"
    }
  ],
  "durationMs": 1500,
  "timestamp": "2025-11-08T09:00:00"
}
```

**Notification Content:**

**SMS:**
```
Dear John Doe,
Your FD A/C FD20251108001 of ₹50,000 is maturing on 15-Nov-2025.
Maturity Amount: ₹53,250
Current Instruction: CLOSE_AND_PAYOUT
To change, visit branch or call 1800-XXX-XXXX
- Credexa Bank
```

**Email:**
```
Subject: Your FD Account is Maturing Soon

Dear John Doe,

This is to inform you that your Fixed Deposit account is maturing soon:

Account Number: FD20251108001
Principal Amount: ₹50,000.00
Maturity Date: 15-Nov-2025
Maturity Amount: ₹53,250.00
Current Instruction: Close and Payout

If you wish to change the maturity instruction, please:
- Visit your nearest branch
- Call our helpline: 1800-XXX-XXXX
- Login to internet banking

Thank you for banking with Credexa Bank.

Regards,
Credexa Bank
```

**When to Use:**
- **Scheduled:** Runs daily at 9:00 AM
- **Manual:** Before long weekends/holidays to ensure customers are notified

**Testing Scenarios:**

**✅ MANAGER - Send Maturity Notices**
```
POST /batch/maturity-notice
Authorization: Bearer <manager_token>

Expected: 200 OK - Notices sent to customers with accounts maturing in 7 days
```

---

### 4. POST /batch/capitalize-interest - Capitalize Interest for Account

**Purpose:** Convert accrued interest into principal (increase principal balance)

**Access:** 🔐 MANAGER, ADMIN only

**Query Parameters:**
- `accountNumber`: Account to capitalize (required)
- `performedBy`: Username of person performing action (required)

**Request:**
```
POST /batch/capitalize-interest?accountNumber=FD20251108001&performedBy=manager01
Authorization: Bearer <manager_token>
```

**How It Works:**
1. Fetches account and validates status is ACTIVE
2. Reads current interest balance
3. Adds interest to principal amount
4. Resets interest balance to 0
5. Recalculates maturity amount with new principal
6. Creates INTEREST_CAPITALIZATION transaction
7. Publishes `InterestCapitalizedEvent`

**Before Capitalization:**
```
Principal Amount: ₹50,000.00
Interest Balance: ₹1,500.00
Current Balance: ₹51,500.00
Future daily interest: (50000 × 6.5) / 36500 = ₹8.90/day
```

**After Capitalization:**
```
Principal Amount: ₹51,500.00 (increased by interest)
Interest Balance: ₹0.00 (reset)
Current Balance: ₹51,500.00
Future daily interest: (51500 × 6.5) / 36500 = ₹9.16/day (higher)
Maturity Amount: Recalculated with new principal
```

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Interest capitalized successfully",
  "accountNumber": "FD20251108001",
  "interestCapitalized": 1500.00,
  "oldPrincipal": 50000.00,
  "newPrincipal": 51500.00,
  "oldMaturityAmount": 53250.00,
  "newMaturityAmount": 54797.50,
  "transactionReference": "TXN20251108050",
  "timestamp": "2025-11-08T10:00:00"
}
```

**Use Cases:**
- **Quarterly Capitalization:** Some products offer quarterly interest capitalization
- **Annual Capitalization:** Compound interest products
- **Customer Request:** Customer wants to increase principal
- **Product Feature:** "Interest Compounding FD"

**Testing Scenarios:**

**✅ MANAGER - Capitalize Interest**
```
POST /batch/capitalize-interest?accountNumber=FD20251108001&performedBy=manager01
Authorization: Bearer <manager_token>

Expected: 200 OK - Interest moved to principal
```

**✅ Verify Capitalization Effect**
```
Before:
1. GET /accounts/FD20251108001
   Principal: 50000, Interest: 1500

2. POST /batch/capitalize-interest?accountNumber=FD20251108001&performedBy=manager01

After:
3. GET /accounts/FD20251108001
   Principal: 51500, Interest: 0

4. Verify future interest calculated on 51500
```

**❌ Capitalize Already Capitalized Account**
```
POST /batch/capitalize-interest?accountNumber=FD20251108001&performedBy=manager01

Expected: 400 Bad Request
{
  "error": "No interest available to capitalize. Interest balance is 0"
}
```

**❌ Capitalize Closed Account**
```
POST /batch/capitalize-interest?accountNumber=FD20251108999&performedBy=manager01

Expected: 400 Bad Request
{
  "error": "Cannot capitalize interest on closed account"
}
```

---

### 5. POST /batch/payout-interest - Process Interest Payout for Account

**Purpose:** Pay out accrued interest to customer's savings account (principal remains in FD)

**Access:** 🔐 MANAGER, ADMIN only

**Query Parameters:**
- `accountNumber`: Account to payout from (required)
- `performedBy`: Username (required)

**Request:**
```
POST /batch/payout-interest?accountNumber=FD20251108001&performedBy=manager01
Authorization: Bearer <manager_token>
```

**How It Works:**
1. Fetches account and validates ACTIVE status
2. Reads current interest balance
3. Creates INTEREST_PAYOUT transaction
4. Resets interest balance to 0
5. Principal remains unchanged
6. Transfers interest to linked savings account
7. Publishes `InterestPaidOutEvent`

**Before Payout:**
```
Principal Amount: ₹50,000.00 (stays in FD)
Interest Balance: ₹1,500.00
Savings Account Balance: ₹10,000.00
```

**After Payout:**
```
FD Account:
- Principal Amount: ₹50,000.00 (unchanged)
- Interest Balance: ₹0.00 (reset)

Savings Account:
- Balance: ₹11,500.00 (increased by ₹1,500)
```

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Interest payout processed successfully",
  "accountNumber": "FD20251108001",
  "interestPaidOut": 1500.00,
  "transferredTo": "SA20251108001",
  "fdPrincipalRemaining": 50000.00,
  "transactionReference": "TXN20251108051",
  "timestamp": "2025-11-08T10:00:00"
}
```

**Difference Between Capitalization and Payout:**
```
CAPITALIZATION:
- Interest → Principal (stays in FD)
- Principal increases
- Future interest calculated on higher amount
- Total investment grows

PAYOUT:
- Interest → Savings Account (leaves FD)
- Principal stays same
- Provides regular income
- Total FD value remains constant
```

**Use Cases:**
- **Monthly Income Scheme:** Pay out interest monthly for regular income
- **Senior Citizen FD:** Provide monthly income to retirees
- **Quarterly Payout:** Pay interest every quarter
- **Customer Liquidity:** Customer needs regular income without closing FD

**Testing Scenarios:**

**✅ MANAGER - Process Interest Payout**
```
POST /batch/payout-interest?accountNumber=FD20251108001&performedBy=manager01
Authorization: Bearer <manager_token>

Expected: 200 OK - Interest transferred to savings account
```

**✅ Verify Payout Effect**
```
Before:
1. GET /accounts/FD20251108001
   Principal: 50000, Interest: 1500

2. POST /batch/payout-interest?accountNumber=FD20251108001&performedBy=manager01

After:
3. GET /accounts/FD20251108001
   Principal: 50000 (unchanged), Interest: 0

4. Check savings account - should have +1500
```

---

### 6. POST /batch/generate-daily-statements - Generate Daily Statements

**Purpose:** Generate account statements for daily transactions

**Access:** 🔐 MANAGER, ADMIN only

**Request:**
```
POST /batch/generate-daily-statements
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Daily statement generation completed successfully",
  "statementsGenerated": 45,
  "accountsProcessed": 45,
  "statementDate": "2025-11-08",
  "durationMs": 800,
  "timestamp": "2025-11-08T23:55:00"
}
```

**Statement Content:**
```
Daily Statement - FD20251108001
Date: 08-Nov-2025

Opening Balance: ₹50,000.00

Transactions:
Date        Type                Amount      Balance
08-Nov-25   INTEREST_CREDIT     ₹8.90       ₹50,008.90

Closing Balance: ₹50,008.90

Interest Earned Today: ₹8.90
Interest Rate: 6.50% p.a.
Maturity Date: 08-Nov-2026
```

---

### 7. POST /batch/generate-monthly-statements - Generate Monthly Statements

**Purpose:** Generate comprehensive monthly account statements

**Access:** 🔐 MANAGER, ADMIN only

**Request:**
```
POST /batch/generate-monthly-statements
Authorization: Bearer <admin_token>
```

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Monthly statement generation completed successfully",
  "statementsGenerated": 120,
  "month": "October 2025",
  "accountsProcessed": 120,
  "pdfGenerated": 120,
  "durationMs": 5200,
  "timestamp": "2025-11-01T00:05:00"
}
```

**Statement Content:**
```
CREDEXA BANK - Monthly FD Statement
October 2025

Account Number: FD20251108001
Account Name: John Doe Regular FD
Product: Regular FD (FD001)

Account Summary:
Opening Balance (01-Oct-2025): ₹50,000.00
Closing Balance (31-Oct-2025): ₹50,275.90

Transaction Details:
Date        Description              Debit       Credit      Balance
01-Oct-25   Opening Balance          -           -           ₹50,000.00
01-Oct-25   Interest Credit          -           ₹8.90       ₹50,008.90
02-Oct-25   Interest Credit          -           ₹8.90       ₹50,017.80
...
31-Oct-25   Interest Credit          -           ₹8.90       ₹50,275.90

Summary:
Total Credits: ₹275.90
Total Debits: ₹0.00
Interest Earned: ₹275.90
Average Daily Balance: ₹50,137.95

Interest Details:
Interest Rate: 6.50% p.a.
Days in Month: 31
Daily Interest: ₹8.90
Total Interest: ₹275.90

Account Information:
Effective Date: 08-Nov-2024
Maturity Date: 08-Nov-2026
Maturity Amount: ₹53,250.00
Status: ACTIVE
```

---

### 8. GET /batch/status - Get Batch Job Status

**Purpose:** Monitor batch job execution status and statistics

**Access:** 🔐 MANAGER, ADMIN only

**Request:**
```
GET /batch/status
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "statistics": {
    "totalAccounts": 150,
    "activeAccounts": 125,
    "maturedAccounts": 15,
    "closedAccounts": 10,
    "suspendedAccounts": 0
  },
  "lastBatchRuns": {
    "interestCalculation": {
      "lastRun": "2025-11-08T01:00:00",
      "status": "SUCCESS",
      "accountsProcessed": 125,
      "duration": 3450,
      "errors": 0
    },
    "maturityProcessing": {
      "lastRun": "2025-11-08T01:30:00",
      "status": "SUCCESS",
      "accountsProcessed": 15,
      "duration": 2100,
      "errors": 0
    },
    "maturityNotice": {
      "lastRun": "2025-11-08T09:00:00",
      "status": "SUCCESS",
      "noticesSent": 25,
      "duration": 1500,
      "errors": 2
    },
    "dailyStatements": {
      "lastRun": "2025-11-07T23:55:00",
      "status": "SUCCESS",
      "statementsGenerated": 45,
      "duration": 800,
      "errors": 0
    }
  },
  "scheduledBatches": {
    "interestCalculation": {
      "schedule": "0 0 1 * * ?",
      "description": "Daily at 1:00 AM",
      "nextRun": "2025-11-09T01:00:00",
      "enabled": true
    },
    "maturityProcessing": {
      "schedule": "0 30 1 * * ?",
      "description": "Daily at 1:30 AM",
      "nextRun": "2025-11-09T01:30:00",
      "enabled": true
    },
    "maturityNotice": {
      "schedule": "0 0 9 * * ?",
      "description": "Daily at 9:00 AM",
      "nextRun": "2025-11-09T09:00:00",
      "enabled": true
    }
  },
  "systemHealth": {
    "databaseConnection": "HEALTHY",
    "kafkaConnection": "HEALTHY",
    "productPricingService": "HEALTHY",
    "customerService": "HEALTHY"
  },
  "timestamp": "2025-11-08T10:00:00"
}
```

**Use Cases:**
- **Monitor Batch Health:** Check if batches are running successfully
- **Troubleshoot Failures:** Identify which batches failed and why
- **Audit Execution:** Track when batches last ran
- **System Health:** Verify all dependencies are working

**Testing Scenarios:**

**✅ MANAGER - Check Batch Status**
```
GET /batch/status
Authorization: Bearer <manager_token>

Expected: 200 OK - Complete batch execution status
```

**✅ ADMIN - Monitor After Batch Execution**
```
1. POST /batch/interest-calculation
2. Wait for completion
3. GET /batch/status
4. Verify lastBatchRuns.interestCalculation shows SUCCESS
```

---

## FD Reporting

### 9. GET /report/fd-summary - Get FD Summary Report

**Purpose:** Aggregated statistics grouped by product

**Access:** 🔐 MANAGER, ADMIN only

**Request:**
```
GET /report/fd-summary
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
[
  {
    "productCode": "FD001",
    "productName": "Regular FD",
    "totalAccounts": 45,
    "activeAccounts": 40,
    "maturedAccounts": 3,
    "closedAccounts": 2,
    "totalPrincipal": 2250000.00,
    "totalInterestAccrued": 125000.50,
    "totalCurrentBalance": 2375000.50,
    "averageAccountSize": 50000.00,
    "averageInterestRate": 6.50,
    "totalMaturityValue": 2462500.00
  },
  {
    "productCode": "FD002",
    "productName": "Senior Citizen FD",
    "totalAccounts": 25,
    "activeAccounts": 23,
    "maturedAccounts": 1,
    "closedAccounts": 1,
    "totalPrincipal": 2500000.00,
    "totalInterestAccrued": 175000.75,
    "totalCurrentBalance": 2675000.75,
    "averageAccountSize": 100000.00,
    "averageInterestRate": 7.25,
    "totalMaturityValue": 3012500.00
  }
]
```

**Use Cases:**
- **Product Performance Analysis:** Which products are most popular
- **Total Liability:** Calculate bank's total FD liability per product
- **Interest Expense Tracking:** Monitor interest being paid
- **Product Comparison:** Compare performance across products
- **Business Planning:** Identify high-performing products

**Testing Scenarios:**

**✅ MANAGER - View FD Summary**
```
GET /report/fd-summary
Authorization: Bearer <manager_token>

Expected: 200 OK - Summary for all products
```

**✅ Verify Calculations**
```
1. Create 5 accounts with FD001 (₹50,000 each)
2. GET /report/fd-summary
3. Verify FD001 shows:
   - totalAccounts: 5
   - totalPrincipal: 250000
   - averageAccountSize: 50000
```

---

### 10. GET /report/customer-portfolio - Get Customer Portfolio Report

**Purpose:** View own portfolio summary (CUSTOMER only)

**Access:** 🔐 CUSTOMER only (own portfolio)

**Request:**
```
GET /report/customer-portfolio
Authorization: Bearer <customer_token>
```

**Success Response (200 OK):**
```json
{
  "customerId": 1,
  "customerName": "John Doe",
  "summary": {
    "totalAccounts": 3,
    "activeAccounts": 2,
    "maturedAccounts": 1,
    "totalInvestment": 200000.00,
    "totalCurrentValue": 205500.75,
    "totalInterestEarned": 5500.75,
    "totalMaturityValue": 242250.00
  },
  "accounts": [
    {
      "accountNumber": "FD20251108001",
      "accountName": "Regular FD",
      "productName": "Regular FD",
      "productCode": "FD001",
      "principalAmount": 50000.00,
      "interestEarned": 1250.50,
      "currentBalance": 51250.50,
      "interestRate": 6.50,
      "effectiveDate": "2025-11-08",
      "maturityDate": "2026-11-08",
      "maturityAmount": 53250.00,
      "status": "ACTIVE",
      "daysToMaturity": 365,
      "roleType": "OWNER"
    },
    {
      "accountNumber": "FD20230108001",
      "accountName": "Tax Saver FD",
      "productName": "Tax Saver FD",
      "productCode": "FD003",
      "principalAmount": 150000.00,
      "interestEarned": 4250.25,
      "currentBalance": 154250.25,
      "interestRate": 7.00,
      "effectiveDate": "2020-11-08",
      "maturityDate": "2030-11-08",
      "maturityAmount": 189000.00,
      "status": "ACTIVE",
      "daysToMaturity": 1825,
      "roleType": "OWNER"
    },
    {
      "accountNumber": "FD20241108001",
      "accountName": "Joint FD",
      "productName": "Regular FD",
      "productCode": "FD001",
      "principalAmount": 100000.00,
      "interestEarned": 6500.00,
      "currentBalance": 106500.00,
      "interestRate": 6.50,
      "effectiveDate": "2024-11-08",
      "maturityDate": "2025-11-08",
      "maturityAmount": 106500.00,
      "status": "MATURED",
      "daysToMaturity": 0,
      "roleType": "CO_OWNER"
    }
  ]
}
```

**Testing Scenarios:**

**✅ CUSTOMER - View Own Portfolio**
```
GET /report/customer-portfolio
Authorization: Bearer <customer_token>

Expected: 200 OK - List of own accounts with summary
```

**✅ Customer with Multiple Roles**
```
Customer has:
- 2 accounts as OWNER
- 1 account as CO_OWNER
- 1 account as NOMINEE

GET /report/customer-portfolio

Expected: Shows all 4 accounts with respective roleType
```

**❌ Customer Without Login**
```
GET /report/customer-portfolio
(No Authorization header)

Expected: 401 Unauthorized
```

---

### 11. GET /report/customer-portfolio/admin - Get Customer Portfolio (Admin)

**Purpose:** View any customer's portfolio (MANAGER/ADMIN only)

**Access:** 🔐 MANAGER, ADMIN only

**Query Parameters:**
- `customerId`: Customer ID to view (required)

**Request:**
```
GET /report/customer-portfolio/admin?customerId=5
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
{
  "customerId": 5,
  "customerName": "Jane Smith",
  "contactPhone": "+919876543210",
  "contactEmail": "jane.smith@example.com",
  "summary": {
    "totalAccounts": 2,
    "activeAccounts": 2,
    "totalInvestment": 300000.00,
    "totalCurrentValue": 308750.50,
    "totalInterestEarned": 8750.50,
    "totalMaturityValue": 345000.00
  },
  "accounts": [
    {
      "accountNumber": "FD20251108005",
      "accountName": "Jane's Regular FD",
      "principalAmount": 100000.00,
      "currentBalance": 103250.25,
      "status": "ACTIVE",
      "maturityDate": "2026-11-08"
    },
    {
      "accountNumber": "FD20251108010",
      "accountName": "Jane's Senior Citizen FD",
      "principalAmount": 200000.00,
      "currentBalance": 205500.25,
      "status": "ACTIVE",
      "maturityDate": "2028-11-08"
    }
  ],
  "relationshipValue": "PREMIUM",
  "remarks": "High-value customer with 2 active FDs"
}
```

**Use Cases:**
- **Customer Service:** Answer customer queries about their portfolio
- **Relationship Management:** View customer's complete FD portfolio
- **Loan Assessment:** Check FD holdings for loan against FD
- **Cross-Selling:** Identify customers with high balances for new products

**Testing Scenarios:**

**✅ MANAGER - View Customer Portfolio**
```
GET /report/customer-portfolio/admin?customerId=5
Authorization: Bearer <manager_token>

Expected: 200 OK - Customer 5's complete portfolio
```

**❌ CUSTOMER - Try Admin Endpoint**
```
GET /report/customer-portfolio/admin?customerId=1
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
```

---

### 12. GET /report/interest-history - Get Interest Transaction History

**Purpose:** View own interest credit history (CUSTOMER only)

**Access:** 🔐 CUSTOMER only (own history)

**Query Parameters:**
- `fromDate`: Start date (YYYY-MM-DD, required)
- `toDate`: End date (YYYY-MM-DD, required)

**Request:**
```
GET /report/interest-history?fromDate=2025-10-01&toDate=2025-10-31
Authorization: Bearer <customer_token>
```

**Success Response (200 OK):**
```json
{
  "customerId": 1,
  "customerName": "John Doe",
  "fromDate": "2025-10-01",
  "toDate": "2025-10-31",
  "summary": {
    "totalInterestEarned": 275.90,
    "numberOfDays": 31,
    "numberOfAccounts": 2,
    "averageDailyInterest": 8.90
  },
  "accountWiseDetails": [
    {
      "accountNumber": "FD20251108001",
      "productName": "Regular FD",
      "principalAmount": 50000.00,
      "interestRate": 6.50,
      "interestEarned": 275.90,
      "dailyInterest": 8.90,
      "transactions": [
        {
          "date": "2025-10-01",
          "amount": 8.90,
          "balanceAfter": 50008.90
        },
        {
          "date": "2025-10-02",
          "amount": 8.90,
          "balanceAfter": 50017.80
        }
        // ... remaining 29 days
      ]
    }
  ]
}
```

**Use Cases:**
- **Tax Calculation:** Customer needs interest income for tax filing
- **Income Tracking:** Monitor monthly interest income
- **TDS Verification:** Verify TDS deducted on interest
- **Account Performance:** Check if interest is being credited properly

**Testing Scenarios:**

**✅ CUSTOMER - View Interest History**
```
GET /report/interest-history?fromDate=2025-10-01&toDate=2025-10-31
Authorization: Bearer <customer_token>

Expected: 200 OK - Interest transactions for October
```

**✅ Customer Requests Full Year History**
```
GET /report/interest-history?fromDate=2025-01-01&toDate=2025-12-31
Authorization: Bearer <customer_token>

Expected: 200 OK - 365 days of interest transactions
```

---

### 13. GET /report/interest-history/admin - Get Interest History (Admin)

**Purpose:** View any customer's interest history

**Access:** 🔐 MANAGER, ADMIN only

**Query Parameters:**
- `customerId`: Customer ID (required)
- `fromDate`: Start date (required)
- `toDate`: End date (required)

**Request:**
```
GET /report/interest-history/admin?customerId=5&fromDate=2025-10-01&toDate=2025-10-31
Authorization: Bearer <manager_token>
```

**Use Cases:**
- **Customer Service:** Answer queries about interest credits
- **TDS Calculation:** Calculate TDS on interest earned
- **Audit:** Verify interest calculation accuracy
- **Compliance:** Generate interest certificates

---

### 14. GET /report/maturity-summary - Get Maturity Summary Report

**Purpose:** Accounts maturing in date range (MANAGER/ADMIN)

**Access:** 🔐 MANAGER, ADMIN only

**Query Parameters:**
- `fromDate`: Start date (required)
- `toDate`: End date (required)

**Request:**
```
GET /report/maturity-summary?fromDate=2025-11-01&toDate=2025-11-30
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
{
  "fromDate": "2025-11-01",
  "toDate": "2025-11-30",
  "summary": {
    "totalAccounts": 15,
    "totalPrincipal": 750000.00,
    "totalMaturityAmount": 825000.00,
    "instructionBreakdown": {
      "CLOSE_AND_PAYOUT": 10,
      "RENEW_PRINCIPAL_ONLY": 3,
      "RENEW_WITH_INTEREST": 2,
      "TRANSFER_TO_SAVINGS": 0,
      "HOLD": 0
    }
  },
  "accounts": [
    {
      "maturityDate": "2025-11-15",
      "accountNumber": "FD20241115001",
      "accountName": "John Doe Regular FD",
      "customerId": 1,
      "customerName": "John Doe",
      "customerPhone": "+919876543210",
      "customerEmail": "john.doe@example.com",
      "productCode": "FD001",
      "productName": "Regular FD",
      "principalAmount": 50000.00,
      "interestAccrued": 3250.00,
      "maturityAmount": 53250.00,
      "maturityInstruction": "CLOSE_AND_PAYOUT",
      "maturityTransferAccount": null,
      "daysToMaturity": 7,
      "branchCode": "BR001",
      "branchName": "Main Branch",
      "notificationSent": true,
      "notificationDate": "2025-11-08"
    }
    // ... more accounts
  ]
}
```

**Use Cases:**
- **Cash Flow Planning:** Plan for maturity payouts
- **Customer Retention:** Contact customers before maturity
- **Branch Operations:** Prepare for maturity processing
- **Renewal Management:** Identify renewal opportunities

**Testing Scenarios:**

**✅ MANAGER - View Maturing Accounts This Month**
```
GET /report/maturity-summary?fromDate=2025-11-01&toDate=2025-11-30
Authorization: Bearer <manager_token>

Expected: 200 OK - List of accounts maturing in November
```

**✅ ADMIN - Plan for Next Quarter**
```
GET /report/maturity-summary?fromDate=2025-12-01&toDate=2026-02-28
Authorization: Bearer <admin_token>

Expected: 200 OK - Q1 2026 maturity schedule
```

---

### 15. GET /report/maturity-summary/customer - Get Customer Maturity Summary

**Purpose:** View own upcoming maturities (CUSTOMER)

**Access:** 🔐 CUSTOMER only

**Request:**
```
GET /report/maturity-summary/customer
Authorization: Bearer <customer_token>
```

**Success Response (200 OK):**
```json
{
  "customerId": 1,
  "customerName": "John Doe",
  "upcomingMaturities": [
    {
      "accountNumber": "FD20251108001",
      "accountName": "Regular FD",
      "productName": "Regular FD",
      "maturityDate": "2026-11-08",
      "principalAmount": 50000.00,
      "maturityAmount": 53250.00,
      "currentInstruction": "CLOSE_AND_PAYOUT",
      "daysToMaturity": 365,
      "canChangeInstruction": true
    }
  ]
}
```

**Testing Scenarios:**

**✅ CUSTOMER - View Upcoming Maturities**
```
GET /report/maturity-summary/customer
Authorization: Bearer <customer_token>

Expected: 200 OK - Own accounts maturing soon
```

---

## Role Management

### 16. GET /roles/account/{accountNumber} - Get Account Roles

**Purpose:** List all roles associated with an account

**Access:** 🔐 MANAGER, ADMIN (all accounts), CUSTOMER (own accounts)

**Request:**
```
GET /roles/account/FD20251108001
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
[
  {
    "roleId": 1,
    "accountNumber": "FD20251108001",
    "customerId": 1,
    "customerName": "John Doe",
    "roleType": "OWNER",
    "ownershipPercentage": 100.00,
    "isPrimary": true,
    "isActive": true,
    "startDate": "2025-11-08",
    "endDate": null,
    "remarks": "Primary account holder"
  }
]

**Example - Joint Account with Multiple Roles:**
```json
[
  {
    "roleId": 1,
    "accountNumber": "FD20251108005",
    "customerId": 1,
    "customerName": "John Doe",
    "roleType": "OWNER",
    "ownershipPercentage": 50.00,
    "isPrimary": true,
    "isActive": true,
    "startDate": "2025-11-08",
    "endDate": null
  },
  {
    "roleId": 2,
    "accountNumber": "FD20251108005",
    "customerId": 2,
    "customerName": "Jane Doe",
    "roleType": "CO_OWNER",
    "ownershipPercentage": 50.00,
    "isPrimary": false,
    "isActive": true,
    "startDate": "2025-11-08",
    "endDate": null
  },
  {
    "roleId": 3,
    "accountNumber": "FD20251108005",
    "customerId": 3,
    "customerName": "Baby Doe",
    "roleType": "NOMINEE",
    "ownershipPercentage": 0.00,
    "isPrimary": false,
    "isActive": true,
    "startDate": "2025-11-08",
    "endDate": null
  }
]
```

**Role Types:**
- `OWNER`: Primary account owner
- `CO_OWNER`: Joint account co-owner
- `NOMINEE`: Beneficiary in case of death
- `AUTHORIZED_SIGNATORY`: Person authorized to transact
- `GUARDIAN`: Guardian for minor accounts

---

### 17. GET /roles/customer/{customerId} - Get Customer Roles

**Purpose:** List all accounts where customer has a role

**Access:** 🔐 MANAGER, ADMIN (any customer), CUSTOMER (own ID only)

**Request:**
```
GET /roles/customer/1
Authorization: Bearer <customer_token> (customerId=1)
```

**Success Response (200 OK):**
```json
[
  {
    "roleId": 1,
    "accountNumber": "FD20251108001",
    "accountName": "My Regular FD",
    "roleType": "OWNER",
    "ownershipPercentage": 100.00,
    "isPrimary": true,
    "isActive": true
  },
  {
    "roleId": 5,
    "accountNumber": "FD20251108005",
    "accountName": "Joint FD with Spouse",
    "roleType": "CO_OWNER",
    "ownershipPercentage": 50.00,
    "isPrimary": false,
    "isActive": true
  },
  {
    "roleId": 10,
    "accountNumber": "FD20251108010",
    "accountName": "Parent's FD",
    "roleType": "NOMINEE",
    "ownershipPercentage": 0.00,
    "isPrimary": false,
    "isActive": true
  }
]
```

**Use Cases:**
- **Customer View:** See all accounts associated with customer
- **Joint Account Management:** Identify joint holdings
- **Nominee Tracking:** Find where customer is nominee

---

### 18. GET /roles/account/{accountNumber}/active - Get Active Account Roles

**Purpose:** List only active roles (excludes expired/removed roles)

**Access:** 🔐 MANAGER, ADMIN, CUSTOMER (own accounts)

**Request:**
```
GET /roles/account/FD20251108001/active
Authorization: Bearer <manager_token>
```

---

### 19. POST /roles/account/{accountNumber} - Add Role

**Purpose:** Add a new role to an existing account

**Access:** 🔐 MANAGER, ADMIN only

**Request Body:**
```json
{
  "customerId": 2,
  "customerName": "Jane Doe",
  "roleType": "CO_OWNER",
  "ownershipPercentage": 50.00,
  "isPrimary": false,
  "isActive": true,
  "remarks": "Spouse added as joint holder"
}
```

**Request:**
```
POST /roles/account/FD20251108001
Authorization: Bearer <manager_token>
Content-Type: application/json

{
  "customerId": 2,
  "customerName": "Jane Doe",
  "roleType": "CO_OWNER",
  "ownershipPercentage": 50.00,
  "isPrimary": false,
  "isActive": true,
  "remarks": "Spouse added as joint holder"
}
```

**Success Response (201 Created):**
```json
{
  "roleId": 4,
  "accountNumber": "FD20251108001",
  "customerId": 2,
  "customerName": "Jane Doe",
  "roleType": "CO_OWNER",
  "ownershipPercentage": 50.00,
  "isPrimary": false,
  "isActive": true,
  "message": "Role added successfully"
}
```

**Validations:**
- Customer must exist in customer-service
- Cannot add duplicate active roles (same customer + same role type)
- OWNER must exist before adding other roles
- NOMINEE cannot be same as OWNER
- Total ownership percentage should not exceed 100%

**Testing Scenarios:**

**✅ MANAGER - Add Joint Holder**
```json
POST /roles/account/FD20251108001
Authorization: Bearer <manager_token>

{
  "customerId": 2,
  "customerName": "Jane Doe",
  "roleType": "CO_OWNER",
  "ownershipPercentage": 50.00,
  "isPrimary": false,
  "isActive": true,
  "remarks": "Spouse"
}

Expected: 201 Created - Joint holder added
```

**✅ ADMIN - Add Nominee**
```json
POST /roles/account/FD20251108001
Authorization: Bearer <admin_token>

{
  "customerId": 3,
  "customerName": "Baby Doe",
  "roleType": "NOMINEE",
  "ownershipPercentage": 0.00,
  "isPrimary": false,
  "isActive": true,
  "remarks": "Child as nominee"
}

Expected: 201 Created - Nominee added
```

**❌ CUSTOMER - Try to Add Role**
```
POST /roles/account/FD20251108001
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
```

**❌ Add Duplicate Role**
```json
POST /roles/account/FD20251108001
{
  "customerId": 2,
  "roleType": "CO_OWNER"  // Already exists
}

Expected: 400 Bad Request
{
  "error": "Customer 2 already has active CO_OWNER role on this account"
}
```

---

### 20. PUT /roles/{roleId} - Update Role

**Purpose:** Modify existing role (e.g., change nominee, update percentage)

**Access:** 🔐 MANAGER, ADMIN only

**Request Body:**
```json
{
  "customerId": 5,
  "customerName": "Updated Nominee",
  "roleType": "NOMINEE",
  "ownershipPercentage": 0.00,
  "remarks": "Changed nominee from child to spouse"
}
```

**Request:**
```
PUT /roles/3
Authorization: Bearer <manager_token>
Content-Type: application/json

{
  "customerId": 5,
  "customerName": "Updated Nominee",
  "roleType": "NOMINEE",
  "remarks": "Changed nominee"
}
```

**Success Response (200 OK):**
```json
{
  "roleId": 3,
  "message": "Role updated successfully",
  "oldCustomerId": 3,
  "newCustomerId": 5,
  "updatedFields": ["customerId", "customerName", "remarks"]
}
```

**Testing Scenarios:**

**✅ MANAGER - Change Nominee**
```json
PUT /roles/3
Authorization: Bearer <manager_token>

{
  "customerId": 5,
  "customerName": "New Nominee",
  "roleType": "NOMINEE",
  "remarks": "Updated nominee"
}

Expected: 200 OK - Nominee changed
```

**✅ ADMIN - Update Ownership Percentage**
```json
PUT /roles/2
Authorization: Bearer <admin_token>

{
  "ownershipPercentage": 60.00,
  "remarks": "Increased ownership from 50% to 60%"
}

Expected: 200 OK - Ownership updated
```

---

### 21. DELETE /roles/{roleId} - Remove Role

**Purpose:** Deactivate a role (soft delete)

**Access:** 🔐 MANAGER, ADMIN only

**Request:**
```
DELETE /roles/4
Authorization: Bearer <manager_token>
```

**Success Response (200 OK):**
```json
{
  "roleId": 4,
  "message": "Role deactivated successfully",
  "accountNumber": "FD20251108001",
  "customerId": 2,
  "roleType": "CO_OWNER",
  "endDate": "2025-11-08"
}
```

**Restrictions:**
- Cannot delete OWNER if account is ACTIVE
- Cannot delete all roles from account (at least one must remain)
- Joint accounts must retain at least one CO_OWNER
- Soft delete only - role remains in database with isActive=false

**Testing Scenarios:**

**✅ MANAGER - Remove Joint Holder**
```
DELETE /roles/4
Authorization: Bearer <manager_token>

Expected: 200 OK - Joint holder removed
```

**❌ Try to Delete Primary Owner**
```
DELETE /roles/1  (Primary OWNER)

Expected: 400 Bad Request
{
  "error": "Cannot remove primary owner while account is active"
}
```

**❌ CUSTOMER - Try to Delete Role**
```
DELETE /roles/4
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
```

---

## Complete Testing Workflows

### Workflow 1: Complete Account Lifecycle (MANAGER)

```
Step 1: Create Account
POST /accounts
Authorization: Bearer <manager_token>

{
  "accountName": "Test FD Account",
  "productCode": "FD001",
  "principalAmount": 50000.00,
  "termMonths": 12,
  "effectiveDate": "2025-11-08",
  "roles": [
    {
      "customerId": 1,
      "customerName": "Test Customer",
      "roleType": "OWNER",
      "ownershipPercentage": 100.00,
      "isPrimary": true,
      "isActive": true
    }
  ],
  "branchCode": "BR001",
  "maturityInstruction": "CLOSE_AND_PAYOUT",
  "tdsApplicable": true,
  "createdBy": "manager01"
}

✅ Expected: 201 Created - Returns account number FD20251108001


Step 2: Add Nominee
POST /roles/account/FD20251108001
Authorization: Bearer <manager_token>

{
  "customerId": 3,
  "customerName": "Nominee Name",
  "roleType": "NOMINEE",
  "ownershipPercentage": 0.00,
  "isPrimary": false,
  "isActive": true,
  "remarks": "Nominee"
}

✅ Expected: 201 Created


Step 3: Run Interest Calculation
POST /batch/interest-calculation
Authorization: Bearer <manager_token>

✅ Expected: 200 OK - Interest credited


Step 4: Check Account Balance
GET /accounts/FD20251108001
Authorization: Bearer <manager_token>

✅ Expected: 200 OK
{
  "principalAmount": 50000.00,
  "interestBalance": 8.90,
  "currentBalance": 50008.90
}


Step 5: View Transaction History
GET /transactions/account/FD20251108001
Authorization: Bearer <manager_token>

✅ Expected: 200 OK - Shows INITIAL_DEPOSIT + INTEREST_CREDIT


Step 6: Fast Forward to Maturity (Manual Close)
POST /accounts/manual-close
Authorization: Bearer <manager_token>

{
  "accountNumber": "FD20251108001",
  "closureDate": "2026-11-08",
  "remarks": "Manual closure on maturity"
}

✅ Expected: 200 OK - Account closed, maturity paid


Step 7: Verify Closure
GET /accounts/FD20251108001

✅ Expected: 200 OK - Status = CLOSED
```

---

### Workflow 2: Premature Withdrawal (CUSTOMER)

```
Step 1: Login as Customer
POST http://localhost:8080/api/auth/login

{
  "username": "customer",
  "password": "customer123"
}

✅ Get JWT token


Step 2: View Own Portfolio
GET /report/customer-portfolio
Authorization: Bearer <customer_token>

✅ Expected: 200 OK - See all own FD accounts


Step 3: Check Withdrawal Penalty
POST /transactions/premature-withdrawal/inquire
Authorization: Bearer <customer_token>

{
  "accountNumber": "FD20251108001",
  "withdrawalDate": "2026-05-08"
}

✅ Expected: 200 OK
{
  "interestEarned": 1602.74,
  "penaltyPercentage": 3.00,
  "penaltyAmount": 48.08,
  "netInterest": 1554.66,
  "totalPayoutAmount": 51554.66,
  "lossAmount": 1695.34
}


Step 4: Decide to Proceed with Withdrawal
POST /fd/account/withdraw
Authorization: Bearer <customer_token>

{
  "fdAccountNo": "FD20251108001",
  "withdrawalDate": "2026-05-08",
  "transferAccount": "SA20251108001"
}

✅ Expected: 200 OK
{
  "totalPayoutAmount": 51554.66,
  "status": "CLOSED",
  "transactionReference": "TXN20260508001"
}


Step 5: Verify Closure
GET /accounts/FD20251108001
Authorization: Bearer <customer_token>

✅ Expected: 200 OK - Status = CLOSED
```

---

### Workflow 3: Role-Based Access Testing

```
Test 1: CUSTOMER tries to create account
POST /accounts
Authorization: Bearer <customer_token>

❌ Expected: 403 Forbidden


Test 2: CUSTOMER views own account
GET /accounts/FD20251108001 (own account)
Authorization: Bearer <customer_token>

✅ Expected: 200 OK


Test 3: CUSTOMER tries to view other's account
GET /accounts/FD20251108005 (owned by another customer)
Authorization: Bearer <customer_token>

❌ Expected: 403 Forbidden


Test 4: CUSTOMER tries batch operation
POST /batch/interest-calculation
Authorization: Bearer <customer_token>

❌ Expected: 403 Forbidden


Test 5: MANAGER creates account
POST /accounts
Authorization: Bearer <manager_token>

✅ Expected: 201 Created


Test 6: MANAGER views reports
GET /report/fd-summary
Authorization: Bearer <manager_token>

✅ Expected: 200 OK


Test 7: MANAGER tries to reverse transaction
POST /transactions/TXN123/reverse
Authorization: Bearer <manager_token>

❌ Expected: 403 Forbidden - Only ADMIN can reverse


Test 8: ADMIN reverses transaction
POST /transactions/TXN123/reverse
Authorization: Bearer <admin_token>

✅ Expected: 200 OK


Test 9: No authentication
GET /accounts
(No Authorization header)

❌ Expected: 401 Unauthorized
```

---

**End of Part 2**

**Complete Guide Status:**
- ✅ Part 1: Authentication, Account Management, Lab Endpoints, Transactions (20 endpoints)
- ✅ Part 2: Batch Operations, Reporting, Role Management, Workflows (21 endpoints)
- ✅ **Total:** 41 endpoints fully documented
- ✅ **All request bodies match actual DTOs**
- ✅ **Complete role-based testing scenarios**
- ✅ **Product-pricing integration explained**

For Part 1, see [SWAGGER-TESTING-GUIDE-PART-1.md](SWAGGER-TESTING-GUIDE-PART-1.md)
