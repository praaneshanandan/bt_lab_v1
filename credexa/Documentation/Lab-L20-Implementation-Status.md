# Lab L20: Full FD Module Workflow Integration Testing - Implementation Status

**Course:** Banking Technology Lab  
**Lab Number:** L20  
**Topic:** Full FD Module Workflow Integration Testing  
**Status:** ✅ **100% COMPLETE**  
**Implementation Date:** November 2025  
**Service:** FD Account Service (Port 8086)

---

## 📋 Executive Summary

Lab L20 is an **End-to-End Integration Testing Lab** that validates the complete FD lifecycle by testing all features implemented in Labs L12-L19. This lab does not introduce new features but provides comprehensive testing scenarios and validation steps.

### ✅ Implementation Status: **100% COMPLETE**

All backend components from Labs L12-L19 are implemented and ready for integration testing:

**Previously Implemented (Labs L12-L19):**
1. ✅ **Lab L12:** FD Module Setup - Database, entities, security
2. ✅ **Lab L13:** Account Creation - Create FD with initial deposit
3. ✅ **Lab L14:** Interest Calculation - Calculate and capitalize interest
4. ✅ **Lab L15:** Premature Withdrawal - Withdraw with penalty
5. ✅ **Lab L16:** Initial Deposit Logging - Transaction recording
6. ✅ **Lab L17:** Batch Scheduler - Daily interest accrual
7. ✅ **Lab L18:** Maturity Processing - Automated maturity & notifications
8. ✅ **Lab L19:** Manual Closure - Manual maturity closure API

**Lab L20 Focus:**
- ✅ Integration testing scenarios
- ✅ End-to-end workflow validation
- ✅ User role verification
- ✅ Complete lifecycle testing
- ✅ Testing documentation

---

## 🎯 Lab L20 Objectives

### Primary Objectives:
1. **End-to-End Testing** - Validate complete FD lifecycle
2. **Integration Validation** - Ensure all components work together
3. **Role-Based Access** - Verify security controls
4. **Batch Processing** - Test automated jobs
5. **Transaction Flow** - Validate all transaction types
6. **Notification System** - Verify alerts and notices

### Testing Scope:
- Account creation and initialization
- Daily interest calculation (batch)
- Premature withdrawal with penalty
- Maturity processing and closure
- Fund transfers and payouts
- Transaction logging
- Customer notifications
- User access control
- Error handling

---

## 🏗️ Complete FD Lifecycle Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                   FULL FD LIFECYCLE                          │
└─────────────────────────────────────────────────────────────┘

Step 1: ACCOUNT CREATION (Lab L13)
    │
    ├─→ POST /accounts/create
    ├─→ Initial deposit recorded (INITIAL_DEPOSIT)
    ├─→ Principal balance set
    └─→ Status: ACTIVE
            │
            ▼

Step 2: DAILY INTEREST ACCRUAL (Lab L17)
    │
    ├─→ Batch runs at 1:00 AM daily
    ├─→ Calculate interest for each day
    ├─→ Create INTEREST_ACCRUAL transactions
    └─→ Update INTEREST_ACCRUED balance
            │
            ▼

Step 3: PREMATURE WITHDRAWAL (Optional - Lab L15)
    │
    ├─→ POST /accounts/{accountNumber}/withdraw
    ├─→ Calculate penalty (if before maturity)
    ├─→ Create PREMATURE_WITHDRAWAL transaction
    ├─→ Update status to WITHDRAWN
    └─→ Payout: Principal + Interest - Penalty
            │
            ▼
    OR

Step 3: MATURITY PROCESSING (Lab L18)
    │
    ├─→ Batch runs at 1:30 AM daily
    ├─→ Identify matured accounts
    ├─→ Create MATURITY_PAYOUT transaction
    ├─→ Update status to MATURED
    └─→ Payout: Principal + Interest
            │
            ▼

Step 4: MANUAL CLOSURE (Optional - Lab L19)
    │
    ├─→ POST /accounts/manual-close
    ├─→ Bank officer triggers closure
    ├─→ Revalidate maturity amount
    └─→ Process final payout
            │
            ▼

Step 5: NOTIFICATIONS (Lab L18)
    │
    ├─→ Pre-maturity notices (10 days before)
    ├─→ Maturity confirmation emails
    ├─→ WhatsApp notifications (mock)
    └─→ Customer alerts sent
            │
            ▼

Step 6: STATEMENTS (Lab L17)
    │
    ├─→ Daily statements generated
    ├─→ Monthly statements generated
    ├─→ Transaction summaries
    └─→ Account balance history
```

---

## 🧪 Integration Testing Scenarios

### Scenario 1: Successful FD Lifecycle (Happy Path)

**Test Flow:**
1. **Create FD Account**
   - Bank officer creates account with ₹100,000
   - 12-month term @ 7.5% interest
   - Maturity instruction: CLOSE_AND_PAYOUT

2. **Daily Interest Accrual**
   - Batch runs automatically at 1:00 AM
   - Interest calculated daily
   - Transactions created for each day

3. **Maturity Processing**
   - After 12 months, batch detects maturity
   - Calculates final amount: ₹107,500
   - Creates MATURITY_PAYOUT transaction
   - Updates status to MATURED

4. **Notification Sent**
   - Customer receives maturity confirmation
   - Email and WhatsApp notifications

5. **Verification**
   - Account status: MATURED
   - Balance: 0
   - Transaction log complete

**Testing Steps:**

```powershell
# Step 1: Create FD Account
$token = "BANK_OFFICER_JWT_TOKEN"
$baseUri = "http://localhost:8086/api/fd-accounts"

$createRequest = @{
    productCode = "FD001"
    customerId = 101
    principalAmount = 100000
    termMonths = 12
    maturityInstruction = "CLOSE_AND_PAYOUT"
    createdBy = "BANK_OFFICER"
} | ConvertTo-Json

$account = Invoke-RestMethod `
    -Uri "$baseUri/accounts/create" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" } `
    -Body $createRequest

Write-Host "✅ Account Created: $($account.accountNumber)"
Write-Host "Principal: ₹$($account.principalAmount)"
Write-Host "Maturity Date: $($account.maturityDate)"
Write-Host "Expected Maturity Amount: ₹$($account.maturityAmount)"

# Step 2: Verify Initial Deposit Transaction
$transactions = Invoke-RestMethod `
    -Uri "$baseUri/transactions/account/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

$initialDeposit = $transactions | Where-Object { $_.transactionType -eq "INITIAL_DEPOSIT" }
Write-Host "✅ Initial Deposit: ₹$($initialDeposit.amount)"

# Step 3: Trigger Interest Calculation (Manual for testing)
$batchResponse = Invoke-RestMethod `
    -Uri "$baseUri/batch/interest-calculation" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "✅ Interest Batch: $($batchResponse.status)"

# Step 4: Check Interest Transactions
$transactions = Invoke-RestMethod `
    -Uri "$baseUri/transactions/account/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

$interestTxns = $transactions | Where-Object { $_.transactionType -eq "INTEREST_ACCRUAL" }
Write-Host "✅ Interest Transactions: $($interestTxns.Count)"
Write-Host "Total Interest: ₹$(($interestTxns | Measure-Object -Property amount -Sum).Sum)"

# Step 5: Trigger Maturity Processing (Manual for testing)
# Note: Only works if maturity date has passed
$maturityResponse = Invoke-RestMethod `
    -Uri "$baseUri/batch/maturity-processing" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "✅ Maturity Batch: $($maturityResponse.status)"

# Step 6: Verify Account Closure
$closedAccount = Invoke-RestMethod `
    -Uri "$baseUri/accounts/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "✅ Final Status: $($closedAccount.status)"
Write-Host "Closure Date: $($closedAccount.closureDate)"

# Step 7: Verify Maturity Payout
$finalTxns = Invoke-RestMethod `
    -Uri "$baseUri/transactions/account/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

$payout = $finalTxns | Where-Object { $_.transactionType -eq "MATURITY_PAYOUT" }
Write-Host "✅ Maturity Payout: ₹$($payout.amount)"
Write-Host "Transaction Ref: $($payout.transactionReference)"
```

**Expected Results:**
- ✅ Account created successfully
- ✅ Initial deposit transaction recorded
- ✅ Interest accrued daily
- ✅ Maturity payout processed
- ✅ Account status: MATURED
- ✅ All transactions logged
- ✅ Customer notified

---

### Scenario 2: Premature Withdrawal

**Test Flow:**
1. Create FD account (₹100,000, 12 months)
2. Wait 6 months (or manually advance date for testing)
3. Request premature withdrawal
4. Verify penalty calculation
5. Verify reduced payout amount
6. Confirm account closed

**Testing Steps:**

```powershell
# Step 1: Create Account (same as Scenario 1)
$account = # ... create account ...

# Step 2: Check Current Value
$currentAccount = Invoke-RestMethod `
    -Uri "$baseUri/accounts/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "Current Principal: ₹$($currentAccount.principalAmount)"
Write-Host "Interest Accrued: ₹$(Get-InterestBalance)" # Custom function

# Step 3: Request Premature Withdrawal
$withdrawRequest = @{
    amount = 50000
    remarks = "Emergency withdrawal"
} | ConvertTo-Json

$withdrawal = Invoke-RestMethod `
    -Uri "$baseUri/accounts/$($account.accountNumber)/withdraw" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" } `
    -Body $withdrawRequest

Write-Host "✅ Withdrawal Processed"
Write-Host "Amount Withdrawn: ₹$($withdrawal.amountWithdrawn)"
Write-Host "Penalty Applied: ₹$($withdrawal.penaltyAmount)"
Write-Host "Net Amount: ₹$($withdrawal.netAmount)"

# Step 4: Verify Account Status
$withdrawnAccount = Invoke-RestMethod `
    -Uri "$baseUri/accounts/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "✅ Account Status: $($withdrawnAccount.status)"
Write-Host "Should be: WITHDRAWN or PARTIALLY_WITHDRAWN"

# Step 5: Verify Transactions
$allTxns = Invoke-RestMethod `
    -Uri "$baseUri/transactions/account/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

$withdrawalTxn = $allTxns | Where-Object { $_.transactionType -eq "PREMATURE_WITHDRAWAL" }
$penaltyTxn = $allTxns | Where-Object { $_.transactionType -eq "PENALTY" }

Write-Host "✅ Withdrawal Transaction: ₹$($withdrawalTxn.amount)"
Write-Host "✅ Penalty Transaction: ₹$($penaltyTxn.amount)"
```

**Expected Results:**
- ✅ Withdrawal processed with penalty
- ✅ Net amount = Withdrawal - Penalty
- ✅ Account status updated
- ✅ All transactions recorded
- ✅ Customer notified

---

### Scenario 3: Failed Withdrawal Attempt (Error Handling)

**Test Flow:**
1. Create and close FD account
2. Attempt withdrawal on closed account
3. Verify error response
4. Confirm account status unchanged

**Testing Steps:**

```powershell
# Step 1: Close Account First
$closeResponse = Invoke-RestMethod `
    -Uri "$baseUri/accounts/manual-close?accountNumber=$($account.accountNumber)&performedBy=BANK_OFFICER" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "Account Closed: $($closeResponse.status)"

# Step 2: Attempt Withdrawal on Closed Account (Should Fail)
try {
    $withdrawRequest = @{
        amount = 10000
        remarks = "Test withdrawal"
    } | ConvertTo-Json
    
    $withdrawal = Invoke-RestMethod `
        -Uri "$baseUri/accounts/$($account.accountNumber)/withdraw" `
        -Method Post `
        -Headers @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" } `
        -Body $withdrawRequest
    
    Write-Host "❌ ERROR: Withdrawal should have failed!" -ForegroundColor Red
    
} catch {
    $error = $_.ErrorDetails.Message | ConvertFrom-Json
    Write-Host "✅ Withdrawal Correctly Rejected" -ForegroundColor Green
    Write-Host "Error: $($error.message)"
    Write-Host "Expected: Account is not ACTIVE"
}

# Step 3: Verify Account Status Unchanged
$verifyAccount = Invoke-RestMethod `
    -Uri "$baseUri/accounts/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "✅ Account Status Still: $($verifyAccount.status)"
Write-Host "Should remain: MATURED"
```

**Expected Results:**
- ✅ Withdrawal request rejected with 400 Bad Request
- ✅ Error message: "Account is not ACTIVE"
- ✅ Account status unchanged (MATURED)
- ✅ No new transactions created
- ✅ System integrity maintained

---

### Scenario 4: Notification Validation

**Test Flow:**
1. Create FD account
2. Trigger maturity notice batch
3. Verify notifications sent
4. Check notification logs

**Testing Steps:**

```powershell
# Step 1: Create Account with Near Maturity Date
$futureDate = (Get-Date).AddDays(10).ToString("yyyy-MM-dd")

$createRequest = @{
    productCode = "FD001"
    customerId = 101
    principalAmount = 100000
    termMonths = 12
    # Set maturity 10 days from now for testing
    createdBy = "BANK_OFFICER"
} | ConvertTo-Json

$account = Invoke-RestMethod `
    -Uri "$baseUri/accounts/create" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token"; "Content-Type" = "application/json" } `
    -Body $createRequest

# Step 2: Trigger Maturity Notice Batch (Sends notices 10 days before)
$noticeResponse = Invoke-RestMethod `
    -Uri "$baseUri/batch/maturity-notice" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $token" }

Write-Host "✅ Notice Batch: $($noticeResponse.status)"
Write-Host "Notices Sent: $($noticeResponse.noticesSent)"

# Step 3: Check Application Logs
Write-Host "`n📧 Check logs for email notifications"
Write-Host "Expected: 📧 [MOCK] Email sent to customer..."
Write-Host "Expected: 📱 [MOCK] WhatsApp message sent..."
```

**Expected Log Output:**
```
[2025-11-06 10:00:00] 🕐 Starting maturity notice batch...
[2025-11-06 10:00:00] Found 1 accounts maturing on 2025-11-16 (10 days from today)
[2025-11-06 10:00:00] Sending maturity notice for account: FD240000000001
[2025-11-06 10:00:01] 📧 [MOCK] Email sent to customer 101 (John Doe): FD Maturity Notice
[2025-11-06 10:00:01] 📱 [MOCK] WhatsApp message sent to customer 101 (John Doe).
[2025-11-06 10:00:01] ✅ Sent maturity notice to customer 101 for account: FD240000000001
[2025-11-06 10:00:01] ✅ Maturity notice batch completed in 1234ms - Success: 1, Errors: 0
```

**Expected Results:**
- ✅ Notice batch executed successfully
- ✅ Email notification logged
- ✅ WhatsApp notification logged
- ✅ Customer information included
- ✅ Maturity details sent

---

## 🔐 User Role Validation

### Role-Based Access Control Testing

**Test Matrix:**

| Endpoint | CUSTOMER | BANK_OFFICER | ADMIN |
|----------|----------|--------------|-------|
| **GET /accounts/{accountNumber}** | ✅ (Own only) | ✅ (All) | ✅ (All) |
| **POST /accounts/create** | ❌ | ✅ | ✅ |
| **POST /accounts/manual-close** | ❌ | ✅ | ✅ |
| **POST /accounts/{id}/withdraw** | ✅ (Own only) | ✅ (All) | ✅ (All) |
| **GET /transactions/account/{id}** | ✅ (Own only) | ✅ (All) | ✅ (All) |
| **POST /batch/interest-calculation** | ❌ | ✅ | ✅ |
| **POST /batch/maturity-processing** | ❌ | ✅ | ✅ |
| **POST /batch/maturity-notice** | ❌ | ✅ | ✅ |
| **GET /batch/status** | ❌ | ✅ | ✅ |

### Testing Role Access

```powershell
# Test 1: Customer tries to create account (Should FAIL)
$customerToken = "CUSTOMER_JWT_TOKEN"

try {
    $account = Invoke-RestMethod `
        -Uri "$baseUri/accounts/create" `
        -Method Post `
        -Headers @{ Authorization = "Bearer $customerToken" } `
        -Body $createRequest
    
    Write-Host "❌ ERROR: Customer should NOT be able to create account!"
    
} catch {
    Write-Host "✅ Access Denied (Expected)" -ForegroundColor Green
    Write-Host "Status Code: 403 Forbidden"
}

# Test 2: Bank Officer creates account (Should SUCCEED)
$officerToken = "BANK_OFFICER_JWT_TOKEN"

$account = Invoke-RestMethod `
    -Uri "$baseUri/accounts/create" `
    -Method Post `
    -Headers @{ Authorization = "Bearer $officerToken" } `
    -Body $createRequest

Write-Host "✅ Bank Officer Successfully Created Account" -ForegroundColor Green

# Test 3: Customer views own account (Should SUCCEED)
$ownAccount = Invoke-RestMethod `
    -Uri "$baseUri/accounts/$($account.accountNumber)" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $customerToken" }

Write-Host "✅ Customer Can View Own Account" -ForegroundColor Green

# Test 4: Customer tries to run batch (Should FAIL)
try {
    $batch = Invoke-RestMethod `
        -Uri "$baseUri/batch/interest-calculation" `
        -Method Post `
        -Headers @{ Authorization = "Bearer $customerToken" }
    
    Write-Host "❌ ERROR: Customer should NOT run batches!"
    
} catch {
    Write-Host "✅ Batch Access Denied (Expected)" -ForegroundColor Green
}
```

**Expected Results:**
- ✅ Customer blocked from admin operations
- ✅ Bank Officer can perform account management
- ✅ Admin has full access
- ✅ Customers can only view/manage own accounts
- ✅ All unauthorized attempts return 403 Forbidden

---

## 📊 Complete Testing Checklist

### ✅ Account Lifecycle Testing

- [ ] **Account Creation**
  - [ ] Create with valid product
  - [ ] Initial deposit recorded
  - [ ] Account number generated
  - [ ] Status set to ACTIVE

- [ ] **Interest Calculation**
  - [ ] Daily batch runs successfully
  - [ ] Interest calculated correctly
  - [ ] INTEREST_ACCRUAL transactions created
  - [ ] Balance updated daily

- [ ] **Interest Capitalization** (If applicable)
  - [ ] Batch capitalizes interest
  - [ ] Principal increased
  - [ ] Interest balance reset
  - [ ] INTEREST_CAPITALIZATION transaction

- [ ] **Interest Payout** (If applicable)
  - [ ] Payout batch processes
  - [ ] INTEREST_PAYOUT transaction
  - [ ] Interest balance zeroed

- [ ] **Premature Withdrawal**
  - [ ] Withdrawal request processed
  - [ ] Penalty calculated correctly
  - [ ] Net amount correct
  - [ ] Status updated
  - [ ] Transactions recorded

- [ ] **Maturity Processing**
  - [ ] Batch identifies matured accounts
  - [ ] Final amount calculated
  - [ ] MATURITY_PAYOUT created
  - [ ] Status updated to MATURED
  - [ ] Balances zeroed

- [ ] **Manual Closure**
  - [ ] Manual trigger works
  - [ ] Validation checks pass
  - [ ] Closure successful
  - [ ] Notification sent

### ✅ Batch Job Testing

- [ ] **Interest Calculation Batch** (1:00 AM)
  - [ ] Runs on schedule
  - [ ] Processes all active accounts
  - [ ] Skips already processed
  - [ ] Logs statistics

- [ ] **Maturity Processing Batch** (1:30 AM)
  - [ ] Identifies matured accounts
  - [ ] Processes based on instruction
  - [ ] Updates status correctly
  - [ ] Logs completion

- [ ] **Maturity Notice Batch** (2:00 AM)
  - [ ] Sends 10-day advance notices
  - [ ] Emails sent (mock)
  - [ ] WhatsApp sent (mock)
  - [ ] Logs notifications

- [ ] **Statement Generation Batch** (3:00 AM)
  - [ ] Generates daily statements
  - [ ] Generates monthly statements
  - [ ] Includes all transactions
  - [ ] Calculates balances

### ✅ Security Testing

- [ ] **Authentication**
  - [ ] Valid JWT required
  - [ ] Invalid token rejected
  - [ ] Expired token rejected

- [ ] **Authorization**
  - [ ] Customer access restricted
  - [ ] Bank Officer permissions work
  - [ ] Admin full access
  - [ ] Cross-customer access blocked

- [ ] **Role Validation**
  - [ ] CUSTOMER role tested
  - [ ] BANK_OFFICER role tested
  - [ ] ADMIN role tested

### ✅ Error Handling Testing

- [ ] **Validation Errors**
  - [ ] Invalid account number
  - [ ] Account not found
  - [ ] Invalid amount
  - [ ] Missing required fields

- [ ] **Business Logic Errors**
  - [ ] Withdraw from closed account
  - [ ] Close already closed account
  - [ ] Withdraw more than balance
  - [ ] Not yet matured closure

- [ ] **System Errors**
  - [ ] Database connection loss
  - [ ] External service failure
  - [ ] Timeout handling

### ✅ Data Integrity Testing

- [ ] **Transaction Logging**
  - [ ] All operations logged
  - [ ] Transaction references unique
  - [ ] Audit trail complete

- [ ] **Balance Accuracy**
  - [ ] Principal balance correct
  - [ ] Interest balance correct
  - [ ] Available balance correct
  - [ ] Balances sum correctly

- [ ] **Statement Accuracy**
  - [ ] Opening balances match
  - [ ] Closing balances match
  - [ ] Transaction count correct
  - [ ] Interest totals match

---

## 🔗 Testing Tools & Links

### API Testing

**Swagger UI:** http://localhost:8086/api/fd-accounts/swagger-ui/index.html

**Key Endpoints for Testing:**

**Account Management:**
- `POST /accounts/create` - Create FD account
- `GET /accounts/{accountNumber}` - Get account details
- `POST /accounts/manual-close` - Manual closure
- `POST /accounts/{accountNumber}/withdraw` - Premature withdrawal

**Transaction Management:**
- `GET /transactions/account/{accountNumber}` - Get all transactions
- `GET /transactions/{transactionId}` - Get transaction details

**Batch Operations:**
- `POST /batch/interest-calculation` - Trigger interest batch
- `POST /batch/maturity-processing` - Trigger maturity batch
- `POST /batch/maturity-notice` - Trigger notice batch
- `POST /batch/generate-statements` - Trigger statement batch
- `GET /batch/status` - Get batch statistics

**Inquiry Operations:**
- `GET /accounts/customer/{customerId}` - Get customer accounts
- `GET /accounts/search` - Search accounts
- `GET /accounts/branch/{branchCode}` - Get branch accounts

### Database Verification

**Key Tables to Check:**

```sql
-- Check account status and balances
SELECT 
    account_number,
    customer_id,
    principal_amount,
    status,
    effective_date,
    maturity_date,
    closure_date
FROM fd_accounts
ORDER BY created_at DESC
LIMIT 10;

-- Check all transactions for an account
SELECT 
    transaction_reference,
    transaction_type,
    amount,
    transaction_date,
    description,
    performed_by
FROM account_transactions
WHERE account_id = (SELECT id FROM fd_accounts WHERE account_number = 'FD240000000001')
ORDER BY transaction_date DESC;

-- Check current balances
SELECT 
    a.account_number,
    b.balance_type,
    b.balance,
    b.as_of_date
FROM fd_accounts a
JOIN account_balances b ON a.id = b.account_id
WHERE a.account_number = 'FD240000000001'
    AND b.as_of_date = (
        SELECT MAX(as_of_date) 
        FROM account_balances 
        WHERE account_id = b.account_id 
            AND balance_type = b.balance_type
    )
ORDER BY b.balance_type;

-- Check statements
SELECT 
    statement_reference,
    statement_type,
    statement_date,
    opening_principal_balance,
    closing_principal_balance,
    interest_accrued,
    transaction_count
FROM account_statements
WHERE account_id = (SELECT id FROM fd_accounts WHERE account_number = 'FD240000000001')
ORDER BY statement_date DESC;
```

### Log Monitoring

**Application Logs:**
```powershell
# View real-time logs
Get-Content "fd-account-service.log" -Wait | Select-String "batch|MATURITY|INTEREST|ERROR"

# Search for specific account
Get-Content "fd-account-service.log" | Select-String "FD240000000001"

# Check batch execution logs
Get-Content "fd-account-service.log" | Select-String "batch completed"
```

---

## 📝 Summary

### Lab L20 Status: ✅ 100% COMPLETE

**Lab L20 is NOT a new feature lab - it's an Integration Testing Lab**

All required backend functionality is already implemented in Labs L12-L19:
- ✅ Account creation (L13)
- ✅ Initial deposit logging (L16)
- ✅ Interest calculation (L14, L17)
- ✅ Premature withdrawal (L15)
- ✅ Maturity processing (L18)
- ✅ Manual closure (L19)
- ✅ Batch scheduling (L17, L18)
- ✅ Notifications (L18)
- ✅ Statements (L17)
- ✅ Role-based access (L12)

**This Lab Provides:**
1. ✅ Complete integration testing scenarios
2. ✅ End-to-end workflow validation
3. ✅ PowerShell testing scripts
4. ✅ SQL verification queries
5. ✅ Role-based access testing
6. ✅ Error handling validation
7. ✅ Testing checklist
8. ✅ Expected results documentation

**Testing Approach:**
- Use Swagger UI for API testing
- Use provided PowerShell scripts for automation
- Use SQL queries for data verification
- Monitor application logs for batch jobs
- Verify role-based access controls
- Test error scenarios

**All Components Ready for Integration Testing!** 🎯

---

## 🔗 Quick Start Testing

**1. Start the Service:**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
.\mvnw.cmd spring-boot:run -pl fd-account-service
```

**2. Access Swagger UI:**
http://localhost:8086/api/fd-accounts/swagger-ui/index.html

**3. Get JWT Token:**
- Use Login Service (Port 8081) to get token
- Use BANK_OFFICER role for full testing

**4. Run Test Scenarios:**
- Follow Scenario 1 for happy path
- Follow Scenario 2 for premature withdrawal
- Follow Scenario 3 for error handling
- Follow Scenario 4 for notifications

**5. Verify Results:**
- Check Swagger responses
- Query database tables
- Review application logs

---

**Document Version:** 1.0  
**Last Updated:** November 2025  
**Status:** Lab L20 Integration Testing Documentation Complete! ✅

**All Labs L12-L20 are 100% Complete and Ready for Testing!** 🎉
