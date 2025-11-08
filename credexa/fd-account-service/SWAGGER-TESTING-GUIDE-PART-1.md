# FD Account Service - Complete Swagger Testing Guide (Part 1)

**Service:** Fixed Deposit Account Service  
**Base URL:** `http://localhost:8086/api/fd-accounts`  
**Swagger UI:** `http://localhost:8086/swagger-ui.html`

---

## Table of Contents - Part 1
1. [Authentication & Setup](#authentication--setup)
2. [FD Account Management](#fd-account-management)
3. [FD Account Operations (Labs)](#fd-account-operations-labs)
4. [Transaction Management](#transaction-management)

---

## Authentication & Setup

### Getting JWT Tokens

**Login Service URL:** `http://localhost:8080/api/auth/login`

#### Admin Login
```json
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

#### Manager Login
```json
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "manager",
  "password": "manager123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "manager",
  "roles": ["ROLE_MANAGER"]
}
```

#### Customer Login
```json
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "customer",
  "password": "customer123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "customer",
  "roles": ["ROLE_CUSTOMER"]
}
```

### Using JWT in Swagger UI

1. Open Swagger UI: `http://localhost:8086/swagger-ui.html`
2. Click the **"Authorize"** button (lock icon in top right)
3. Enter: `Bearer <your_jwt_token>` (include the word "Bearer" followed by a space)
4. Click **"Authorize"**
5. Click **"Close"**

Now all API calls will include the JWT token automatically.

---

## FD Account Management

### 1. POST /accounts - Create FD Account

**Purpose:** Create a new Fixed Deposit account with standard product rates

**Access:** 🔐 MANAGER, ADMIN only

**How It Works:**
1. Validates product exists in product-pricing-service
2. Fetches product configuration (rates, terms, limits)
3. Validates principal amount and term against product limits
4. Generates unique account number and IBAN
5. Creates account with product's default interest rate
6. Calculates maturity date and amount
7. Creates account roles (owners, nominees)
8. Publishes `AccountCreatedEvent` to Kafka

**Request Body:**
```json
{
  "accountName": "John Doe Regular FD",
  "productCode": "FD001",
  "principalAmount": 50000.00,
  "termMonths": 12,
  "effectiveDate": "2025-11-08",
  "roles": [
    {
      "customerId": 1,
      "customerName": "John Doe",
      "roleType": "OWNER",
      "ownershipPercentage": 100.00,
      "isPrimary": true,
      "isActive": true,
      "remarks": "Primary account holder"
    }
  ],
  "branchCode": "BR001",
  "branchName": "Main Branch",
  "autoRenewal": false,
  "maturityInstruction": "CLOSE_AND_PAYOUT",
  "maturityTransferAccount": null,
  "remarks": "Regular FD for 1 year",
  "tdsApplicable": true,
  "createdBy": "manager01"
}
```

**Field Descriptions:**
- `accountName`: Display name for the account (max 100 chars)
- `productCode`: Product code from product-pricing-service (e.g., "FD001", "FD002")
- `principalAmount`: Deposit amount (min: 1000, max: 99999999999999999.99)
- `termMonths`: Duration in months (min: 1, max: 1200)
- `effectiveDate`: Account start date (format: YYYY-MM-DD)
- `roles`: Array of account roles - **at least one OWNER required**
  - `customerId`: Customer ID from customer-service
  - `customerName`: Customer name for display
  - `roleType`: OWNER, CO_OWNER, NOMINEE, AUTHORIZED_SIGNATORY, GUARDIAN
  - `ownershipPercentage`: Ownership percentage (0-100, decimals allowed)
  - `isPrimary`: true for primary owner, false for others
  - `isActive`: true to activate role
  - `remarks`: Optional notes
- `branchCode`: Branch identifier (max 20 chars)
- `branchName`: Branch display name (optional)
- `autoRenewal`: Auto-renew on maturity (true/false)
- `maturityInstruction`: TRANSFER_TO_SAVINGS, TRANSFER_TO_CURRENT, RENEW_PRINCIPAL_ONLY, RENEW_WITH_INTEREST, CLOSE_AND_PAYOUT, HOLD
- `maturityTransferAccount`: Target account number if maturity instruction is TRANSFER_*
- `remarks`: Optional notes (max 500 chars)
- `tdsApplicable`: Whether TDS should be deducted (true/false)
- `createdBy`: Username of creator (max 100 chars)

**Success Response (201 Created):**
```json
{
  "accountNumber": "FD20251108001",
  "iban": "IN12CRED0001FD20251108001",
  "accountName": "John Doe Regular FD",
  "productCode": "FD001",
  "productName": "Regular FD",
  "principalAmount": 50000.00,
  "interestRate": 6.50,
  "termMonths": 12,
  "effectiveDate": "2025-11-08",
  "maturityDate": "2026-11-08",
  "maturityAmount": 53250.00,
  "status": "ACTIVE",
  "branchCode": "BR001",
  "message": "FD Account created successfully"
}
```

**Integration with Product-Pricing Service:**
```
1. FD Service calls: GET http://localhost:8085/api/pricing/products/FD001
2. Product-Pricing returns:
   {
     "productCode": "FD001",
     "productName": "Regular FD",
     "interestRate": 6.50,
     "minAmount": 10000,
     "maxAmount": 1000000,
     "minTermMonths": 6,
     "maxTermMonths": 120
   }
3. FD Service validates:
   ✓ 50000 is between 10000 and 1000000
   ✓ 12 is between 6 and 120
4. Creates account with 6.50% interest rate
5. Calculates maturity: 50000 + (50000 × 6.50 × 1) / 100 = ₹53,250
```

**Testing Scenarios:**

**✅ MANAGER - Create Account Successfully**
```json
POST /accounts
Authorization: Bearer <manager_token>

{
  "accountName": "Manager Test FD",
  "productCode": "FD001",
  "principalAmount": 75000.00,
  "termMonths": 24,
  "effectiveDate": "2025-11-08",
  "roles": [
    {
      "customerId": 5,
      "customerName": "Jane Smith",
      "roleType": "OWNER",
      "ownershipPercentage": 100.00,
      "isPrimary": true,
      "isActive": true,
      "remarks": "Single owner"
    }
  ],
  "branchCode": "BR002",
  "branchName": "Downtown Branch",
  "autoRenewal": false,
  "maturityInstruction": "CLOSE_AND_PAYOUT",
  "tdsApplicable": true,
  "createdBy": "manager01"
}

Expected: 201 Created - Account created with product's default rate
```

**✅ ADMIN - Create Joint Account**
```json
POST /accounts
Authorization: Bearer <admin_token>

{
  "accountName": "Joint FD - John & Jane",
  "productCode": "FD002",
  "principalAmount": 200000.00,
  "termMonths": 36,
  "effectiveDate": "2025-11-08",
  "roles": [
    {
      "customerId": 1,
      "customerName": "John Doe",
      "roleType": "OWNER",
      "ownershipPercentage": 50.00,
      "isPrimary": true,
      "isActive": true,
      "remarks": "Primary holder"
    },
    {
      "customerId": 2,
      "customerName": "Jane Doe",
      "roleType": "CO_OWNER",
      "ownershipPercentage": 50.00,
      "isPrimary": false,
      "isActive": true,
      "remarks": "Co-owner"
    },
    {
      "customerId": 3,
      "customerName": "Baby Doe",
      "roleType": "NOMINEE",
      "ownershipPercentage": 0.00,
      "isPrimary": false,
      "isActive": true,
      "remarks": "Nominee"
    }
  ],
  "branchCode": "BR001",
  "autoRenewal": true,
  "maturityInstruction": "RENEW_WITH_INTEREST",
  "tdsApplicable": true,
  "createdBy": "admin"
}

Expected: 201 Created - Joint account with 3 roles
```

**❌ CUSTOMER - Try to Create Account**
```json
POST /accounts
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
{
  "status": 403,
  "error": "Access Denied",
  "message": "You do not have permission to access this resource. Required role: ADMIN or MANAGER"
}
```

**❌ Invalid Product Code**
```json
POST /accounts
{
  "productCode": "INVALID999",
  ...
}

Expected: 400 Bad Request
{
  "error": "Product not found: INVALID999"
}
```

**❌ Principal Below Minimum**
```json
POST /accounts
{
  "productCode": "FD001",
  "principalAmount": 500.00,  // Product minimum is 10000
  ...
}

Expected: 400 Bad Request
{
  "error": "Principal amount 500.00 is below product minimum: 10000"
}
```

---

### 2. POST /accounts/customize - Create Customized FD Account

**Purpose:** Create FD account with custom interest rate and term within product limits

**Access:** 🔐 MANAGER, ADMIN only

**Request Body:**
```json
{
  "accountName": "Premium Customer FD",
  "productCode": "FD001",
  "principalAmount": 500000.00,
  "customTermMonths": 18,
  "customInterestRate": 7.25,
  "effectiveDate": "2025-11-08",
  "roles": [
    {
      "customerId": 10,
      "customerName": "Premium Customer",
      "roleType": "OWNER",
      "ownershipPercentage": 100.00,
      "isPrimary": true,
      "isActive": true,
      "remarks": "VIP customer with negotiated rate"
    }
  ],
  "branchCode": "BR001",
  "branchName": "Main Branch",
  "customInterestCalculationMethod": "MONTHLY_COMPOUND",
  "customInterestPayoutFrequency": "QUARTERLY",
  "autoRenewal": false,
  "maturityInstruction": "TRANSFER_TO_SAVINGS",
  "maturityTransferAccount": "SA20251108001",
  "remarks": "Special rate for high-value customer",
  "tdsApplicable": true,
  "customTdsRate": 10.00,
  "createdBy": "manager01"
}
```

**Additional Field Descriptions:**
- `customTermMonths`: Custom term (validated against product min/max)
- `customInterestRate`: Custom rate in percentage (validated against product min/max)
- `customInterestCalculationMethod`: Custom calculation method
- `customInterestPayoutFrequency`: MONTHLY, QUARTERLY, ANNUALLY
- `customTdsRate`: Custom TDS rate (0-100%)

**Product Limit Validation:**
```
Product FD001 Configuration:
- minInterestRate: 5.00%
- maxInterestRate: 8.00%
- minTermMonths: 6
- maxTermMonths: 120

Custom Request: 7.25% rate, 18 months

Validation:
✓ 7.25% is between 5.00% and 8.00%
✓ 18 months is between 6 and 120

Result: Account created with 7.25% custom rate
```

**Testing Scenarios:**

**✅ ADMIN - Create with Custom Rate**
```json
POST /accounts/customize
Authorization: Bearer <admin_token>

{
  "accountName": "Senior Citizen Special FD",
  "productCode": "FD002",
  "principalAmount": 300000.00,
  "customTermMonths": 60,
  "customInterestRate": 7.75,
  "effectiveDate": "2025-11-08",
  "roles": [
    {
      "customerId": 15,
      "customerName": "Senior Customer",
      "roleType": "OWNER",
      "ownershipPercentage": 100.00,
      "isPrimary": true,
      "isActive": true,
      "remarks": "Senior citizen"
    }
  ],
  "branchCode": "BR001",
  "autoRenewal": false,
  "maturityInstruction": "RENEW_PRINCIPAL_ONLY",
  "tdsApplicable": false,
  "createdBy": "admin"
}

Expected: 201 Created - Account with 7.75% custom rate
```

**❌ Custom Rate Exceeds Maximum**
```json
POST /accounts/customize
{
  "customInterestRate": 10.00,  // Product max is 8.00%
  ...
}

Expected: 400 Bad Request
{
  "error": "Custom interest rate 10.00% exceeds product maximum: 8.00%"
}
```

---

### 3. GET /accounts/{identifier} - Get Account

**Purpose:** Retrieve account details by account number or IBAN

**Access:** 🔐 MANAGER, ADMIN (all accounts), CUSTOMER (own accounts only)

**Path Parameter:**
- `identifier`: Account number OR IBAN

**Examples:**
```
GET /accounts/FD20251108001
GET /accounts/IN12CRED0001FD20251108001
```

**Success Response (200 OK):**
```json
{
  "accountId": 1,
  "accountNumber": "FD20251108001",
  "iban": "IN12CRED0001FD20251108001",
  "accountName": "John Doe Regular FD",
  "productCode": "FD001",
  "productName": "Regular FD",
  "principalAmount": 50000.00,
  "interestBalance": 1250.50,
  "currentBalance": 51250.50,
  "interestRate": 6.50,
  "termMonths": 12,
  "effectiveDate": "2025-11-08",
  "maturityDate": "2026-11-08",
  "maturityAmount": 53250.00,
  "status": "ACTIVE",
  "branchCode": "BR001",
  "branchName": "Main Branch",
  "autoRenewal": false,
  "maturityInstruction": "CLOSE_AND_PAYOUT",
  "tdsApplicable": true,
  "createdBy": "manager01",
  "createdAt": "2025-11-08T10:00:00",
  "roles": [
    {
      "roleId": 1,
      "customerId": 1,
      "customerName": "John Doe",
      "roleType": "OWNER",
      "ownershipPercentage": 100.00,
      "isPrimary": true,
      "isActive": true
    }
  ]
}
```

**Testing Scenarios:**

**✅ MANAGER - View Any Account**
```
GET /accounts/FD20251108001
Authorization: Bearer <manager_token>

Expected: 200 OK - Full account details
```

**✅ CUSTOMER - View Own Account**
```
GET /accounts/FD20251108001 (customerId=1 owns this)
Authorization: Bearer <customer_token>

Expected: 200 OK - Own account details
```

**❌ CUSTOMER - Try to View Other's Account**
```
GET /accounts/FD20251108002 (customerId=5 owns this)
Authorization: Bearer <customer_token> (customerId=1)

Expected: 403 Forbidden
```

**❌ Account Not Found**
```
GET /accounts/INVALID999

Expected: 404 Not Found
{
  "error": "Account not found: INVALID999"
}
```

---

### 4. GET /accounts/customer/{customerId} - Get Customer Accounts

**Purpose:** Get all FD accounts for a specific customer

**Access:** 🔐 MANAGER, ADMIN (any customer), CUSTOMER (own ID only)

**Path Parameter:**
- `customerId`: Customer ID (Long)

**Example:**
```
GET /accounts/customer/1
```

**Success Response (200 OK):**
```json
[
  {
    "accountNumber": "FD20251108001",
    "accountName": "John Doe Regular FD",
    "productCode": "FD001",
    "principalAmount": 50000.00,
    "currentBalance": 51250.50,
    "interestRate": 6.50,
    "maturityDate": "2026-11-08",
    "status": "ACTIVE",
    "roleType": "OWNER"
  },
  {
    "accountNumber": "FD20251108005",
    "accountName": "Joint FD",
    "productCode": "FD002",
    "principalAmount": 200000.00,
    "currentBalance": 210500.00,
    "interestRate": 7.00,
    "maturityDate": "2028-11-08",
    "status": "ACTIVE",
    "roleType": "CO_OWNER"
  }
]
```

**Testing Scenarios:**

**✅ CUSTOMER - View Own Accounts**
```
GET /accounts/customer/1
Authorization: Bearer <customer_token> (customerId=1)

Expected: 200 OK - List of customer's accounts
```

**❌ CUSTOMER - Try to View Other Customer**
```
GET /accounts/customer/5
Authorization: Bearer <customer_token> (customerId=1)

Expected: 403 Forbidden
```

---

### 5. GET /accounts/branch/{branchCode} - Get Accounts by Branch

**Purpose:** Get all accounts in a specific branch

**Access:** 🔐 MANAGER, ADMIN only

**Example:**
```
GET /accounts/branch/BR001
```

**Testing Scenarios:**

**✅ MANAGER - View Branch Accounts**
```
GET /accounts/branch/BR001
Authorization: Bearer <manager_token>

Expected: 200 OK - List of accounts in BR001
```

**❌ CUSTOMER - Try to Access**
```
GET /accounts/branch/BR001
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
```

---

### 6. GET /accounts/product/{productCode} - Get Accounts by Product

**Purpose:** Get all accounts for a specific product

**Access:** 🔐 MANAGER, ADMIN only

**Example:**
```
GET /accounts/product/FD001
```

---

### 7. GET /accounts/maturing - Get Accounts Maturing Soon

**Purpose:** Get accounts maturing within specified days

**Access:** 🔐 MANAGER, ADMIN only

**Query Parameters:**
- `days`: Number of days to look ahead (default: 30)

**Example:**
```
GET /accounts/maturing?days=7
```

**Success Response (200 OK):**
```json
[
  {
    "accountNumber": "FD20241108001",
    "customerName": "John Doe",
    "principalAmount": 50000.00,
    "maturityDate": "2025-11-15",
    "maturityAmount": 53250.00,
    "daysToMaturity": 7,
    "maturityInstruction": "CLOSE_AND_PAYOUT",
    "contactPhone": "+919876543210"
  }
]
```

---

### 8. POST /accounts/search - Search Accounts

**Purpose:** Search accounts with multiple criteria

**Access:** 🔐 MANAGER, ADMIN only

**Request Body:**
```json
{
  "accountNumber": null,
  "accountName": "John",
  "productCode": "FD001",
  "customerId": null,
  "status": "ACTIVE",
  "branchCode": "BR001",
  "effectiveDateFrom": "2025-01-01",
  "effectiveDateTo": "2025-12-31",
  "maturityDateFrom": null,
  "maturityDateTo": null,
  "page": 0,
  "size": 20,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

**Field Descriptions:**
- All search criteria are optional
- `status`: ACTIVE, MATURED, CLOSED, SUSPENDED
- `page`: Page number (0-based)
- `size`: Results per page (1-100)
- `sortBy`: Field to sort by
- `sortDirection`: ASC or DESC

**Success Response (200 OK):**
```json
{
  "content": [
    {
      "accountNumber": "FD20251108001",
      "accountName": "John Doe Regular FD",
      "productCode": "FD001",
      "principalAmount": 50000.00,
      "status": "ACTIVE"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0,
  "size": 20
}
```

**Testing Scenarios:**

**✅ MANAGER - Search Active Accounts**
```json
POST /accounts/search
Authorization: Bearer <manager_token>

{
  "status": "ACTIVE",
  "branchCode": "BR001",
  "page": 0,
  "size": 10
}

Expected: 200 OK - Paginated list of active accounts in BR001
```

**✅ ADMIN - Search High-Value Accounts**
```json
POST /accounts/search
Authorization: Bearer <admin_token>

{
  "productCode": "FD002",
  "status": "ACTIVE",
  "sortBy": "principalAmount",
  "sortDirection": "DESC"
}

Expected: 200 OK - High-value accounts sorted by amount
```

---

### 9. GET /accounts/{accountNumber}/summary - Get Account Summary

**Purpose:** Get condensed account information

**Access:** 🔐 MANAGER, ADMIN (all), CUSTOMER (own only)

---

### 10. GET /accounts/exists/{accountNumber} - Check Account Exists

**Purpose:** Verify if account exists without fetching full details

**Access:** 🔐 MANAGER, ADMIN, CUSTOMER

**Success Response (200 OK):**
```json
{
  "exists": true,
  "accountNumber": "FD20251108001",
  "status": "ACTIVE"
}
```

---

### 11. POST /accounts/manual-close - Manual Maturity Closure

**Purpose:** Manually close matured account

**Access:** 🔐 MANAGER, ADMIN only

**Request Body:**
```json
{
  "accountNumber": "FD20251108001",
  "closureDate": "2026-11-08",
  "remarks": "Manual closure on maturity",
  "performedBy": "manager01"
}
```

---

## FD Account Operations (Labs)

### 12. POST /fd/account/create - Create FD Account (Lab L13)

**Purpose:** Lab L13 specific account creation endpoint

**Access:** 🔐 MANAGER, ADMIN only

**Request Body:**
```json
{
  "accountName": "Lab L13 Test Account",
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
```

**Note:** Similar to POST /accounts but follows Lab L13 specifications

---

### 13. POST /fd/account/withdraw - Withdraw FD Account (Lab L15)

**Purpose:** Lab L15 specific premature withdrawal endpoint

**Access:** 🔐 MANAGER, ADMIN, CUSTOMER (own accounts)

**Request Body:**
```json
{
  "fdAccountNo": "FD20251108001",
  "withdrawalDate": "2026-05-08",
  "transferAccount": "SA20251108001"
}
```

**Field Descriptions:**
- `fdAccountNo`: FD account number to withdraw (required)
- `withdrawalDate`: Date of withdrawal (required, format: YYYY-MM-DD)
- `transferAccount`: Savings/Current account for fund transfer (optional)

**How It Works:**
1. Validates account exists and is ACTIVE
2. Calculates days completed vs. total term
3. Fetches penalty rules from product-pricing-service
4. Calculates penalty based on days completed
5. Computes final payout: Principal + Interest - Penalty
6. Creates PREMATURE_WITHDRAWAL transaction
7. Updates account status to CLOSED
8. Transfers funds to specified account (if provided)
9. Publishes `PrematureWithdrawalEvent` to Kafka

**Penalty Calculation Example:**
```
Account Details:
- Principal: ₹50,000
- Interest Rate: 6.5%
- Term: 365 days (12 months)
- Days Completed: 180 days (6 months)

Product Penalty Rules (from pricing service):
- 0-90 days: 5% penalty
- 91-180 days: 3% penalty
- 181-270 days: 2% penalty
- 271-365 days: 1% penalty

Calculation:
1. Interest Earned (180 days): (50000 × 6.5 × 180) / (365 × 100) = ₹1,602.74
2. Applicable Penalty Slab: 91-180 days → 3%
3. Penalty Amount: 1602.74 × 0.03 = ₹48.08
4. Net Interest: 1602.74 - 48.08 = ₹1,554.66
5. Total Payout: 50000 + 1554.66 = ₹51,554.66
```

**Success Response (200 OK):**
```json
{
  "accountNumber": "FD20251108001",
  "withdrawalDate": "2026-05-08",
  "principalAmount": 50000.00,
  "interestEarned": 1602.74,
  "penaltyAmount": 48.08,
  "netInterest": 1554.66,
  "totalPayoutAmount": 51554.66,
  "transferAccount": "SA20251108001",
  "status": "CLOSED",
  "transactionReference": "TXN20260508001",
  "message": "Premature withdrawal processed successfully"
}
```

**Testing Scenarios:**

**✅ CUSTOMER - Withdraw Own Account**
```json
POST /fd/account/withdraw
Authorization: Bearer <customer_token> (customerId=1)

{
  "fdAccountNo": "FD20251108001",
  "withdrawalDate": "2026-05-08",
  "transferAccount": "SA20251108001"
}

Expected: 200 OK - Withdrawal processed with penalty
```

**✅ MANAGER - Process Withdrawal for Customer**
```json
POST /fd/account/withdraw
Authorization: Bearer <manager_token>

{
  "fdAccountNo": "FD20251108005",
  "withdrawalDate": "2026-05-08",
  "transferAccount": "SA20251108010"
}

Expected: 200 OK - Withdrawal processed
```

**❌ CUSTOMER - Try to Withdraw Other's Account**
```json
POST /fd/account/withdraw
Authorization: Bearer <customer_token> (customerId=1)

{
  "fdAccountNo": "FD20251108005",  // Owned by customerId=5
  "withdrawalDate": "2026-05-08"
}

Expected: 403 Forbidden
```

**❌ Withdrawal Before Effective Date**
```json
POST /fd/account/withdraw
{
  "fdAccountNo": "FD20251108001",
  "withdrawalDate": "2025-10-08"  // Before effectiveDate 2025-11-08
}

Expected: 400 Bad Request
{
  "error": "Withdrawal date cannot be before account effective date"
}
```

---

## Transaction Management

### 14. POST /transactions/premature-withdrawal/inquire - Premature Withdrawal Inquiry

**Purpose:** Calculate penalty and payout amount before actual withdrawal

**Access:** 🔐 MANAGER, ADMIN, CUSTOMER (own accounts)

**Request Body:**
```json
{
  "accountNumber": "FD20251108001",
  "withdrawalDate": "2026-05-08"
}
```

**Success Response (200 OK):**
```json
{
  "accountNumber": "FD20251108001",
  "principalAmount": 50000.00,
  "termDays": 365,
  "daysCompleted": 180,
  "interestRate": 6.50,
  "interestEarned": 1602.74,
  "penaltyPercentage": 3.00,
  "penaltyAmount": 48.08,
  "netInterest": 1554.66,
  "totalPayoutAmount": 51554.66,
  "maturityAmount": 53250.00,
  "lossAmount": 1695.34,
  "message": "Premature withdrawal will result in 3% penalty"
}
```

**Testing Scenarios:**

**✅ CUSTOMER - Check Withdrawal Impact**
```json
POST /transactions/premature-withdrawal/inquire
Authorization: Bearer <customer_token>

{
  "accountNumber": "FD20251108001",
  "withdrawalDate": "2026-05-08"
}

Expected: 200 OK - Shows penalty calculation
```

---

### 15. POST /transactions/premature-withdrawal/process - Process Premature Withdrawal

**Purpose:** Execute premature withdrawal with penalty

**Access:** 🔐 MANAGER, ADMIN, CUSTOMER (own accounts)

**Request Body:**
```json
{
  "accountNumber": "FD20251108001",
  "withdrawalAmount": 51554.66,
  "withdrawalDate": "2026-05-08",
  "performedBy": "customer01",
  "remarks": "Need funds for emergency"
}
```

---

### 16. POST /transactions - Create Transaction

**Purpose:** Create a manual transaction on FD account

**Access:** 🔐 MANAGER, ADMIN only

**Request Body:**
```json
{
  "accountNumber": "FD20251108001",
  "transactionType": "ADDITIONAL_DEPOSIT",
  "amount": 10000.00,
  "transactionDate": "2025-11-15",
  "valueDate": "2025-11-15",
  "description": "Additional deposit by customer",
  "performedBy": "manager01",
  "relatedTransactionId": null
}
```

**Transaction Types:**
- `INITIAL_DEPOSIT`: First deposit (system-generated)
- `ADDITIONAL_DEPOSIT`: Add more funds
- `WITHDRAWAL`: Regular withdrawal
- `PARTIAL_WITHDRAWAL`: Partial withdrawal
- `PREMATURE_WITHDRAWAL`: Early withdrawal
- `INTEREST_CREDIT`: Interest credited
- `INTEREST_ACCRUAL`: Interest accrued
- `INTEREST_CAPITALIZATION`: Interest to principal
- `FEE_DEBIT`: Fee charged
- `PENALTY`: Penalty charged
- `MATURITY_PAYOUT`: Maturity payout
- `MATURITY_TRANSFER`: Maturity transfer
- `MATURITY_RENEWAL`: Renewal
- `REVERSAL`: Reverse transaction
- `ADJUSTMENT`: Manual adjustment

---

### 17. GET /transactions/account/{accountNumber} - Get Account Transactions

**Purpose:** Get all transactions for an account

**Access:** 🔐 MANAGER, ADMIN (all accounts), CUSTOMER (own accounts only)

**Example:**
```
GET /transactions/account/FD20251108001
```

**Success Response (200 OK):**
```json
[
  {
    "transactionId": 1,
    "transactionReference": "TXN20251108001",
    "accountNumber": "FD20251108001",
    "transactionType": "INITIAL_DEPOSIT",
    "amount": 50000.00,
    "transactionDate": "2025-11-08",
    "description": "Initial deposit",
    "balanceAfter": 50000.00
  },
  {
    "transactionId": 2,
    "transactionReference": "TXN20251109001",
    "accountNumber": "FD20251108001",
    "transactionType": "INTEREST_CREDIT",
    "amount": 8.90,
    "transactionDate": "2025-11-09",
    "description": "Daily interest credit",
    "balanceAfter": 50008.90
  }
]
```

**Testing Scenarios:**

**✅ CUSTOMER - View Own Transactions**
```
GET /transactions/account/FD20251108001
Authorization: Bearer <customer_token> (customerId=1)

Expected: 200 OK - List of transactions
```

**❌ CUSTOMER - Try to View Other's Transactions**
```
GET /transactions/account/FD20251108005
Authorization: Bearer <customer_token> (customerId=1, doesn't own this)

Expected: 403 Forbidden
```

---

### 18. GET /transactions/account/{accountNumber}/paged - Get Account Transactions (Paged)

**Purpose:** Get paginated transaction history

**Access:** 🔐 MANAGER, ADMIN, CUSTOMER (own only)

**Query Parameters:**
- `page`: Page number (0-based)
- `size`: Results per page
- `sortBy`: Sort field
- `sortDirection`: ASC or DESC

**Example:**
```
GET /transactions/account/FD20251108001/paged?page=0&size=10&sortBy=transactionDate&sortDirection=DESC
```

---

### 19. GET /transactions/{transactionReference} - Get Transaction

**Purpose:** Get specific transaction by reference

**Access:** 🔐 MANAGER, ADMIN, CUSTOMER (own account transactions)

**Example:**
```
GET /transactions/TXN20251108001
```

---

### 20. POST /transactions/{transactionReference}/reverse - Reverse Transaction

**Purpose:** Reverse an erroneous transaction

**Access:** 🔐 ADMIN only

**Example:**
```
POST /transactions/TXN20251108001/reverse
Authorization: Bearer <admin_token>
```

**Request Body:**
```json
{
  "reason": "Entered wrong amount",
  "performedBy": "admin"
}
```

**Success Response (200 OK):**
```json
{
  "originalTransactionReference": "TXN20251108001",
  "reversalTransactionReference": "TXN20251108050",
  "message": "Transaction reversed successfully"
}
```

**Testing Scenarios:**

**✅ ADMIN - Reverse Transaction**
```json
POST /transactions/TXN20251108001/reverse
Authorization: Bearer <admin_token>

{
  "reason": "Duplicate entry",
  "performedBy": "admin"
}

Expected: 200 OK - Transaction reversed
```

**❌ MANAGER - Try to Reverse**
```
POST /transactions/TXN20251108001/reverse
Authorization: Bearer <manager_token>

Expected: 403 Forbidden - Only ADMIN can reverse
```

**❌ CUSTOMER - Try to Reverse**
```
POST /transactions/TXN20251108001/reverse
Authorization: Bearer <customer_token>

Expected: 403 Forbidden
```

---

**End of Part 1**

Continue to [SWAGGER-TESTING-GUIDE-PART-2.md](SWAGGER-TESTING-GUIDE-PART-2.md) for:
- Batch Operations
- FD Reporting
- Role Management
- Complete Testing Workflows
