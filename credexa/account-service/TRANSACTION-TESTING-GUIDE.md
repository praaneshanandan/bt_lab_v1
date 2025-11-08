# Transaction Management - Testing Guide

## Overview
The account service now supports comprehensive transaction management with flexible account ID types.

---

## Feature 8: Create Transaction (Using Account ID Types)

### Description
Creates transactions on FD accounts using flexible account ID types (ACCOUNT_NUMBER, IBAN, INTERNAL_ID).

### Endpoint
`POST /api/transactions/create`

### Required Role
MANAGER or ADMIN

### Test Case 8.1: Interest Credit Transaction (Account Number)
```bash
POST /api/transactions/create?idType=ACCOUNT_NUMBER&idValue=FD-20251108120000-1234-5
```

**Request Body**:
```json
{
  "transactionType": "INTEREST_CREDIT",
  "amount": 3125.00,
  "referenceNumber": "INT-Q1-2025",
  "description": "Quarterly interest credit for Q1 2025",
  "remarks": "Regular interest payment",
  "channel": "SYSTEM"
}
```

**Expected Response**:
- Status: 201 Created
- Transaction ID generated (e.g., `TXN-20251108120000-1001`)
- Balance before: 50000.00
- Balance after: 53125.00
- Status: COMPLETED
- Initiated by and approved by: current user
- IP address captured

### Test Case 8.2: TDS Deduction Transaction (IBAN)
```bash
POST /api/transactions/create?idType=IBAN&idValue=IN29CRED0001FD2511081234
```

**Request Body**:
```json
{
  "transactionType": "TDS_DEDUCTION",
  "amount": 312.50,
  "referenceNumber": "TDS-Q1-2025",
  "description": "TDS deduction on interest earned",
  "remarks": "10% TDS on interest",
  "channel": "SYSTEM"
}
```

**Expected Response**:
- Transaction ID generated
- Balance before: 53125.00
- Balance after: 52812.50 (decreased by TDS amount)
- Status: COMPLETED

### Test Case 8.3: Deposit Transaction (Internal ID)
```bash
POST /api/transactions/create?idType=INTERNAL_ID&idValue=1
```

**Request Body**:
```json
{
  "transactionType": "DEPOSIT",
  "amount": 10000.00,
  "referenceNumber": "DEP-2025-001",
  "description": "Additional deposit",
  "remarks": "Customer top-up deposit",
  "channel": "BRANCH",
  "branchCode": "BR001"
}
```

**Expected Response**:
- Balance increased by deposit amount
- Channel: BRANCH
- Branch code: BR001

### Transaction Types Available:
- `DEPOSIT` - Initial/additional deposits (increases balance)
- `INTEREST_CREDIT` - Interest accrual (increases balance)
- `TDS_DEDUCTION` - TDS deducted (decreases balance)
- `WITHDRAWAL` - Premature withdrawal (decreases balance)
- `MATURITY_CREDIT` - Maturity amount (increases balance)
- `CLOSURE` - Account closure (decreases balance)
- `REVERSAL` - Transaction reversal (decreases balance)
- `ADJUSTMENT` - Manual adjustment (increases balance)

---

## Feature 9: Transaction Inquiry (Using Account ID Types)

### Description
Find a specific transaction using account ID type and transaction ID. Verifies the transaction belongs to the specified account.

### Endpoint
`POST /api/transactions/inquiry`

### Required Role
CUSTOMER, MANAGER, or ADMIN

### Test Case 9.1: Inquiry by Account Number
```json
{
  "idType": "ACCOUNT_NUMBER",
  "idValue": "FD-20251108120000-1234-5",
  "transactionId": "TXN-20251108120000-1001"
}
```

**Expected Response**:
- Transaction details returned
- Account name included
- Balance before/after shown
- All transaction metadata

### Test Case 9.2: Inquiry by IBAN
```json
{
  "idType": "IBAN",
  "idValue": "IN29CRED0001FD2511081234",
  "transactionId": "TXN-20251108120000-1002"
}
```

**Expected Response**:
- Same transaction details
- Validates transaction belongs to the account

### Test Case 9.3: Inquiry with Default ID Type
```json
{
  "idValue": "FD-20251108120000-1234-5",
  "transactionId": "TXN-20251108120000-1001"
}
```

**Note**: `idType` not specified, defaults to ACCOUNT_NUMBER

**Expected Response**: Transaction details returned

### Test Case 9.4: Transaction Not Found
```json
{
  "idValue": "FD-20251108120000-1234-5",
  "transactionId": "TXN-INVALID-9999"
}
```

**Expected**:
- Status: 404 Not Found
- Error message: "Transaction not found: TXN-INVALID-9999"

### Test Case 9.5: Transaction Belongs to Different Account
```json
{
  "idValue": "FD-OTHER-ACCOUNT",
  "transactionId": "TXN-20251108120000-1001"
}
```

**Expected**:
- Status: 404 Not Found
- Error: "Transaction TXN-xxx does not belong to account FD-OTHER-ACCOUNT"

---

## Feature 10: Transaction List (Using Account ID Types)

### Description
Get paginated list of transactions for a specific account using flexible account ID types.

### Endpoint
`GET /api/transactions/list`

### Required Role
CUSTOMER, MANAGER, or ADMIN

### Test Case 10.1: List by Account Number
```bash
GET /api/transactions/list?idType=ACCOUNT_NUMBER&idValue=FD-20251108120000-1234-5&page=0&size=10&sortBy=transactionDate&sortDir=DESC
```

**Expected Response**:
- Paginated list of transactions
- Newest transactions first
- All transaction details included
- Account name included in each transaction

### Test Case 10.2: List by IBAN
```bash
GET /api/transactions/list?idType=IBAN&idValue=IN29CRED0001FD2511081234&page=0&size=5&sortDir=ASC
```

**Expected Response**:
- Transactions sorted oldest first
- Maximum 5 transactions per page

### Test Case 10.3: List by Internal ID
```bash
GET /api/transactions/list?idType=INTERNAL_ID&idValue=1&page=0&size=20
```

**Expected Response**:
- All transactions for account with database ID 1
- Up to 20 per page

### Test Case 10.4: List with Default ID Type
```bash
GET /api/transactions/list?idValue=FD-20251108120000-1234-5
```

**Note**: Defaults to ACCOUNT_NUMBER if idType not specified

**Expected Response**: Transaction list returned

---

## Additional Transaction Endpoints

### Get Transaction by ID (Simple Lookup)
**Endpoint**: `GET /api/transactions/{transactionId}`

**Example**:
```bash
GET /api/transactions/TXN-20251108120000-1001
```

**Expected**: Transaction details without needing account ID

---

### List Transactions by Account Number (Standard)
**Endpoint**: `GET /api/transactions/account/{accountNumber}`

**Example**:
```bash
GET /api/transactions/account/FD-20251108120000-1234-5?page=0&size=10
```

**Expected**: All transactions for the account (paginated)

---

### List Transactions by Type
**Endpoint**: `GET /api/transactions/type/{transactionType}`

**Required Role**: MANAGER or ADMIN

**Example**:
```bash
GET /api/transactions/type/INTEREST_CREDIT?page=0&size=20
```

**Expected**: All INTEREST_CREDIT transactions across all accounts

**Available Types**:
- DEPOSIT
- INTEREST_CREDIT
- TDS_DEDUCTION
- WITHDRAWAL
- MATURITY_CREDIT
- CLOSURE
- REVERSAL
- ADJUSTMENT

---

### List Transactions by Status
**Endpoint**: `GET /api/transactions/status/{status}`

**Required Role**: MANAGER or ADMIN

**Example**:
```bash
GET /api/transactions/status/COMPLETED?page=0&size=50
```

**Expected**: All COMPLETED transactions

**Available Statuses**:
- PENDING
- APPROVED
- COMPLETED
- FAILED
- REJECTED
- REVERSED

---

### Get Transaction Count
**Endpoint**: `GET /api/transactions/count/{accountNumber}`

**Example**:
```bash
GET /api/transactions/count/FD-20251108120000-1234-5
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Account FD-20251108120000-1234-5 has 5 transactions",
  "data": 5
}
```

---

## Complete Transaction Flow Test

### Scenario: Full Transaction Lifecycle

1. **Create Account**:
   ```bash
   POST /api/accounts/create/default
   # Note the account number
   ```

2. **Create Initial Deposit Transaction**:
   ```bash
   POST /api/transactions/create?idType=ACCOUNT_NUMBER&idValue=YOUR_ACCOUNT_NUMBER
   ```
   ```json
   {
     "transactionType": "DEPOSIT",
     "amount": 50000,
     "description": "Initial FD deposit"
   }
   ```

3. **Credit Quarterly Interest**:
   ```json
   {
     "transactionType": "INTEREST_CREDIT",
     "amount": 937.50,
     "description": "Q1 Interest @ 7.5% p.a."
   }
   ```

4. **Deduct TDS**:
   ```json
   {
     "transactionType": "TDS_DEDUCTION",
     "amount": 93.75,
     "description": "TDS @ 10% on interest"
   }
   ```

5. **List All Transactions**:
   ```bash
   GET /api/transactions/list?idValue=YOUR_ACCOUNT_NUMBER
   ```
   **Expected**: 3 transactions showing balance progression

6. **Inquire Specific Transaction**:
   ```bash
   POST /api/transactions/inquiry
   ```
   ```json
   {
     "idValue": "YOUR_ACCOUNT_NUMBER",
     "transactionId": "TXN-xxx"
   }
   ```

7. **Get Transaction Count**:
   ```bash
   GET /api/transactions/count/YOUR_ACCOUNT_NUMBER
   ```
   **Expected**: Count = 3

8. **Check Account Balance**:
   ```bash
   GET /api/accounts/YOUR_ACCOUNT_NUMBER/balance
   ```
   **Expected**: Balance reflects all transactions

---

## Transaction Response Fields Explained

```json
{
  "id": 1,                                    // Database ID
  "transactionId": "TXN-20251108120000-1001", // Unique transaction ID
  "accountNumber": "FD-xxx",                  // Account number
  "accountName": "John Doe FD Account",       // Account name
  "transactionType": "INTEREST_CREDIT",       // Type of transaction
  "amount": 3125.00,                          // Transaction amount
  "balanceBefore": 50000.00,                  // Balance before this transaction
  "balanceAfter": 53125.00,                   // Balance after this transaction
  "status": "COMPLETED",                      // Transaction status
  "referenceNumber": "INT-Q1-2025",           // Reference number
  "description": "Quarterly interest...",      // Description
  "remarks": "Regular interest payment",       // Additional remarks
  "initiatedBy": "admin",                     // User who created transaction
  "approvedBy": "admin",                      // User who approved
  "transactionDate": "2025-11-08T10:30:00",   // When created
  "approvalDate": "2025-11-08T10:30:00",      // When approved
  "valueDate": "2025-11-08T00:00:00",         // Effective date
  "channel": "SYSTEM",                        // Transaction channel
  "branchCode": "BR001",                      // Branch code
  "ipAddress": "192.168.1.1"                  // Client IP
}
```

---

## Balance Calculation Logic

The service automatically tracks balance changes:

1. **Initial State**: Balance = Principal Amount (from account)
2. **After First Transaction**: Balance = balanceAfter from first transaction
3. **Subsequent Transactions**: Each transaction uses previous balanceAfter as new balanceBefore

**Credit Transactions** (increase balance):
- DEPOSIT
- INTEREST_CREDIT
- MATURITY_CREDIT
- ADJUSTMENT

**Debit Transactions** (decrease balance):
- WITHDRAWAL
- TDS_DEDUCTION
- CLOSURE
- REVERSAL

---

## Role-Based Access Control (Transactions)

| Endpoint | CUSTOMER | MANAGER | ADMIN |
|----------|----------|---------|-------|
| POST /create | ❌ | ✅ | ✅ |
| POST /inquiry | ✅ | ✅ | ✅ |
| GET /{transactionId} | ✅ | ✅ | ✅ |
| GET /list | ✅ | ✅ | ✅ |
| GET /account/{accountNumber} | ✅ | ✅ | ✅ |
| GET /type/{type} | ❌ | ✅ | ✅ |
| GET /status/{status} | ❌ | ✅ | ✅ |
| GET /count/{accountNumber} | ✅ | ✅ | ✅ |

---

## Validation Tests

### Test: Invalid Account ID
```bash
POST /api/transactions/create?idValue=INVALID-ACCOUNT
```
**Expected**: 400 Bad Request - "Account not found"

### Test: Negative Amount
```json
{
  "transactionType": "DEPOSIT",
  "amount": -100
}
```
**Expected**: 400 Bad Request - "Amount must be positive"

### Test: Missing Transaction Type
```json
{
  "amount": 1000
}
```
**Expected**: 400 Bad Request - "Transaction type is required"

---

## Transaction ID Format

**Format**: `TXN-YYYYMMDDHHMMSS-NNNN`

Example: `TXN-20251108120000-1001`

- `TXN`: Transaction prefix
- `YYYYMMDDHHMMSS`: Timestamp
- `NNNN`: 4-digit sequence number

---

## Success Indicators

✅ **Transaction Created Successfully**:
- Response code: 201
- Transaction ID generated
- Balance before/after calculated correctly
- Status: COMPLETED
- Current user as initiatedBy and approvedBy

✅ **Transaction Inquiry Works**:
- Can find by all 3 account ID types
- Validates transaction belongs to account
- Returns complete details

✅ **Transaction List Works**:
- Paginated results
- Sorted by date (newest first)
- All account ID types supported
- Balance progression visible

---

## Troubleshooting

### Issue: Balance Calculation Incorrect
**Solution**: Check transaction order. Each transaction uses the previous balanceAfter as the new balanceBefore.

### Issue: Transaction Not Found in Inquiry
**Solution**: 
1. Verify transaction ID is correct
2. Ensure transaction belongs to the specified account
3. Check account ID type and value match

### Issue: Cannot Create Transaction
**Solution**:
1. Verify user has MANAGER or ADMIN role
2. Check account exists with given ID
3. Validate amount is positive
4. Ensure transaction type is valid

---

## Next Steps

After testing transaction features:
1. Test balance calculations across multiple transactions
2. Verify different transaction types (credit vs debit)
3. Test pagination with large transaction counts
4. Verify all three account ID types work for inquiry and list
5. Test role-based access (CUSTOMER can't create)
6. Check transaction count accuracy

---

**Transaction Management Features**: ✅ **COMPLETE**  
**Last Updated**: November 8, 2025
