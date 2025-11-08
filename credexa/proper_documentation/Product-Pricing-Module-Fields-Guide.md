# Product Pricing Module - Complete Field Guide

## Overview
The Product Pricing module manages Fixed Deposit (FD) product configurations for the Credexa banking system. It defines product characteristics, interest rates, charges, and business rules that govern how FD accounts are created and managed.

---

## Table of Contents
1. [Product Entity](#1-product-entity)
2. [Product Role Entity](#2-product-role-entity)
3. [Product Charge Entity](#3-product-charge-entity)
4. [Interest Rate Matrix Entity](#4-interest-rate-matrix-entity)
5. [Enums](#5-enums)
6. [Data Flow & Usage](#6-data-flow--usage)

---

## 1. Product Entity
**Table:** `products`  
**Purpose:** Main entity that defines an FD product with all its characteristics and business rules.

### Basic Details

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | Long | Auto | Primary key | 1 |
| `productName` | String(200) | Yes | Display name of the product | "Senior Citizen Fixed Deposit" |
| `productCode` | String(50) | Yes (Unique) | Unique identifier code | "FD-SC-001" |
| `productType` | Enum | Yes | Type of FD product | SENIOR_CITIZEN_FD |
| `description` | String(1000) | No | Detailed product description | "Special FD for senior citizens..." |
| `effectiveDate` | LocalDate | Yes | Date from which product is valid | 2024-01-01 |
| `currencyCode` | String(3) | Yes | ISO currency code | "INR", "USD" |
| `status` | Enum | Yes | Current product status | ACTIVE, DRAFT, INACTIVE |

### Term Configuration (For FD Duration)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `minTermMonths` | BigDecimal(10,2) | No | Minimum FD term in months | 3.00 (3 months) |
| `maxTermMonths` | BigDecimal(10,2) | No | Maximum FD term in months | 120.00 (10 years) |

**Usage:** Enforced during FD account creation to ensure term is within allowed range.

### Amount Configuration

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `minAmount` | BigDecimal(19,2) | No | Minimum deposit amount | 10000.00 |
| `maxAmount` | BigDecimal(19,2) | No | Maximum deposit amount | 10000000.00 |
| `minBalanceRequired` | BigDecimal(19,2) | No | Minimum balance to maintain | 5000.00 |

**Usage:** Enforced during deposit and partial withdrawal operations.

### Interest Configuration

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `baseInterestRate` | BigDecimal(5,2) | No | Base annual interest rate | 6.50 (6.5% p.a.) |
| `interestCalculationMethod` | String(50) | No | How interest is calculated | "COMPOUND", "SIMPLE" |
| `interestPayoutFrequency` | String(50) | No | When interest is paid | "QUARTERLY", "ON_MATURITY" |

**Note:** Actual rates come from Interest Rate Matrix (see below).

### Feature Flags

| Field | Type | Default | Description | Usage |
|-------|------|---------|-------------|-------|
| `prematureWithdrawalAllowed` | Boolean | false | Can FD be closed before maturity? | Enforced in premature closure |
| `partialWithdrawalAllowed` | Boolean | false | Can partial amount be withdrawn? | Enforced in partial withdrawal |
| `autoRenewalAllowed` | Boolean | false | Should FD auto-renew on maturity? | Used during maturity processing |

### Tax Configuration

| Field | Type | Default | Description | Example |
|-------|------|---------|-------------|---------|
| `tdsRate` | BigDecimal(5,2) | null | Tax Deduction at Source rate | 10.00 (10%) |
| `tdsApplicable` | Boolean | true | Should TDS be deducted? | true |

**TDS Rules:**
- Only applied if `tdsApplicable = true`
- Only applied if interest >= ₹40,000 (threshold in code)
- Deducted from interest payout

### Relationships

| Relationship | Type | Description |
|--------------|------|-------------|
| `allowedRoles` | One-to-Many | List of roles allowed for this product (Owner, Co-Owner, etc.) |
| `charges` | One-to-Many | List of fees/charges associated with this product |
| `interestRateMatrix` | One-to-Many | Interest rate slabs based on amount/term/classification |

### Audit Fields

| Field | Type | Description |
|-------|------|-------------|
| `createdAt` | LocalDateTime | When product was created |
| `updatedAt` | LocalDateTime | When product was last updated |
| `createdBy` | String(100) | User who created the product |
| `updatedBy` | String(100) | User who last updated the product |

---

## 2. Product Role Entity
**Table:** `product_roles`  
**Purpose:** Defines which roles (Owner, Co-Owner, Guardian, etc.) are allowed for a product.

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | Long | Auto | Primary key | 1 |
| `product` | Product | Yes | Reference to parent product | Product(id=1) |
| `roleType` | Enum | Yes | Type of role | OWNER, CO_OWNER, GUARDIAN |
| `description` | String(500) | No | Additional notes about the role | "Primary account holder" |

### Role Validation Logic
During FD account creation, the system validates:
- **Only allowed roles:** No roles provided that aren't in the product's `allowedRoles` list

### Example Configuration
```
Product: Senior Citizen FD
- OWNER: "Primary account owner"
- NOMINEE: "Beneficiary in case of death"
- CO_OWNER: "Joint account holder"
```

---

## 3. Product Charge Entity
**Table:** `product_charges`  
**Purpose:** Defines fees and charges associated with a product (account opening fee, premature closure penalty, etc.)

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | Long | Auto | Primary key | 1 |
| `product` | Product | Yes | Reference to parent product | Product(id=1) |
| `chargeName` | String(200) | Yes | Name of the charge | "Premature Closure Penalty" |
| `chargeType` | String(50) | Yes | Category of charge | "FEE", "TAX", "PENALTY", "INTEREST" |
| `description` | String(500) | No | Detailed description | "Charged when FD is closed before maturity" |
| `fixedAmount` | BigDecimal(19,2) | No | Fixed charge amount | 500.00 |
| `percentageRate` | BigDecimal(5,2) | No | Percentage-based charge | 1.00 (1% of principal) |
| `frequency` | Enum | Yes | When charge is applied | ONE_TIME, MONTHLY, ON_MATURITY |
| `applicableTransactionTypes` | String(200) | No | Comma-separated transaction types | "PREMATURE_WITHDRAWAL,PARTIAL_WITHDRAWAL" |
| `active` | Boolean | No (true) | Is charge currently active? | true |

### Charge Calculation Logic
- **Either** `fixedAmount` **OR** `percentageRate` should be set, not both
- If `percentageRate` is used: `charge = (amount × percentageRate) / 100`
- Charge is only applied if `active = true`

### Common Charge Examples
| Charge Name | Type | Fixed/Percentage | Frequency | When Applied |
|-------------|------|------------------|-----------|--------------|
| Account Opening Fee | FEE | Fixed: ₹500 | ONE_TIME | Account creation |
| Premature Closure Penalty | PENALTY | Percentage: 1% | ONE_TIME | Premature withdrawal |
| Statement Generation Fee | FEE | Fixed: ₹50 | PER_TRANSACTION | Statement request |
| Cheque Book Charges | FEE | Fixed: ₹200 | ONE_TIME | Cheque book issuance |

---

## 4. Interest Rate Matrix Entity
**Table:** `interest_rate_matrix`  
**Purpose:** Defines interest rates based on customer classification.

| Field | Type | Required | Description | Example |
|-------|------|----------|-------------|---------|
| `id` | Long | Auto | Primary key | 1 |
| `product` | Product | Yes | Reference to parent product | Product(id=1) |
| `customerClassification` | String(50) | No | Customer category | "REGULAR", "SENIOR_CITIZEN", "SUPER_SENIOR", "VIP" |
| `interestRate` | BigDecimal(5,2) | Yes | Base interest rate | 6.50 (6.5% p.a.) |
| `additionalRate` | BigDecimal(5,2) | No (0) | Additional rate (senior citizen bonus, etc.) | 0.50 (extra 0.5%) |
| `effectiveDate` | LocalDate | Yes | Date from which this rate is valid | 2024-01-01 |
| `active` | Boolean | No (true) | Is this rate currently active? | true |

### Rate Selection Logic
When creating an FD account, the system:
1. Filters by **effective date** (`effectiveDate <= today`)
2. Filters by **customer classification** (exact match if specified)
3. Returns the **highest matching rate**
4. Final rate = `interestRate + additionalRate`

### Example Rate Matrix
```
Product: Regular Fixed Deposit

Slab 1: Classification: REGULAR
        Rate: 5.5% + 0% = 5.5%

Slab 2: Classification: SENIOR_CITIZEN
        Rate: 5.5% + 0.5% = 6.0%

Slab 3: Classification: SUPER_SENIOR
        Rate: 6.5% + 0.75% = 7.25%
```

### Customer Classifications
| Classification | Description | Typical Additional Rate |
|----------------|-------------|------------------------|
| `REGULAR` | Standard customer | Base rate (0%) |
| `PREMIUM` | High-value customer | +0.25% to +0.50% |
| `SENIOR_CITIZEN` | Age 60-79 years | +0.50% to +0.75% |
| `SUPER_SENIOR` | Age 80+ years | +0.75% to +1.00% |
| `VIP` | Special category | +0.50% to +1.00% |

---

## 5. Enums

### ProductType
Defines the type of Fixed Deposit product.

| Value | Description | Characteristics |
|-------|-------------|----------------|
| `FIXED_DEPOSIT` | Regular Fixed Deposit | Standard FD with fixed tenure and rate |
| `TAX_SAVER_FD` | Tax Saver Fixed Deposit | 5-year lock-in, Section 80C benefits |
| `SENIOR_CITIZEN_FD` | Senior Citizen FD | Higher rates for 60+ age group |
| `FLEXI_FD` | Flexi Fixed Deposit | Combines savings + FD features |
| `CUMULATIVE_FD` | Cumulative FD | Interest paid on maturity |
| `NON_CUMULATIVE_FD` | Non-Cumulative FD | Interest paid periodically |

### ProductStatus
Defines the lifecycle status of a product.

| Value | Description | Can Create Accounts? |
|-------|-------------|---------------------|
| `DRAFT` | Under development | ❌ No |
| `ACTIVE` | Available for account creation | ✅ Yes |
| `INACTIVE` | Not available for new accounts | ❌ No (existing accounts continue) |
| `SUSPENDED` | Temporarily unavailable | ❌ No |
| `CLOSED` | Discontinued | ❌ No |

### RoleType
Defines roles that can be associated with an FD account.

| Value | Description | Typical Usage |
|-------|-------------|---------------|
| `OWNER` | Primary Account Owner | Mandatory, 1 required |
| `CO_OWNER` | Joint Account Holder | Optional, 0-2 allowed |
| `GUARDIAN` | Guardian for Minor Account | For minor accounts only |
| `NOMINEE` | Beneficiary | Optional, 0-3 allowed |
| `BORROWER` | Primary Borrower | For loan-linked FDs |
| `CO_BORROWER` | Co-Borrower | For loan-linked FDs |
| `GUARANTOR` | Loan Guarantor | For loan-linked FDs |
| `AUTHORIZED_SIGNATORY` | Authorized Signatory | Optional, operational access |

### ChargeFrequency
Defines when a charge/fee is applied.

| Value | Description | Example Usage |
|-------|-------------|---------------|
| `ONE_TIME` | Charged once | Account opening fee |
| `DAILY` | Every day | Daily balance charges |
| `WEEKLY` | Every week | Weekly maintenance fee |
| `MONTHLY` | Every month | Monthly SMS charges |
| `QUARTERLY` | Every 3 months | Quarterly statement fee |
| `SEMI_ANNUALLY` | Every 6 months | Half-yearly charges |
| `ANNUALLY` | Every year | Annual maintenance fee |
| `ON_MATURITY` | When FD matures | Maturity processing fee |
| `ON_TRANSACTION` | Per transaction | Per-withdrawal charges |

---

## 6. Data Flow & Usage

### Product Configuration Flow
```
1. Bank Admin creates Product
   └─> Sets basic details (name, code, type, status)
   └─> Sets term limits (min/max months)
   └─> Sets amount limits (min/max amount)
   └─> Sets interest config (calculation method, payout frequency)
   └─> Sets feature flags (premature withdrawal, partial withdrawal, auto-renewal)
   └─> Sets tax config (TDS rate, applicable flag)

2. Admin adds Product Roles
   └─> OWNER (primary account holder)
   └─> CO_OWNER (optional joint holder)
   └─> NOMINEE (optional beneficiary)

3. Admin adds Product Charges
   └─> Account Opening Fee (₹500, ONE_TIME)
   └─> Premature Closure Penalty (1%, ONE_TIME)
   └─> Statement Fee (₹50, PER_TRANSACTION)

4. Admin adds Interest Rate Matrix
   └─> Multiple rates for different customer classifications
   └─> Regular customers: 5.5%
   └─> Senior citizens: 6.0% (5.5% + 0.5% additional)
   └─> Super senior: 7.25% (6.5% + 0.75% additional)

5. Admin activates Product (status = ACTIVE)
```

### FD Account Creation Flow (Using Product Configuration)
```
Customer requests FD account
   |
   v
1. Fetch Product by productCode
   └─> Validate: status = ACTIVE
   └─> Validate: effectiveDate <= today
   |
   v
2. Validate Amount
   └─> Check: depositAmount >= product.minAmount
   └─> Check: depositAmount <= product.maxAmount
   |
   v
3. Validate Term
   └─> Check: termMonths >= product.minTermMonths
   └─> Check: termMonths <= product.maxTermMonths
   |
   v
4. Validate Roles (from customer request)
   └─> Fetch product.allowedRoles
   └─> Check: All provided roles are in allowed list
   |
   v
5. Calculate Interest Rate
   └─> Fetch product.interestRateMatrix
   └─> Filter by customer classification
   └─> Filter by effective date
   └─> Select highest matching rate
   └─> Apply rate = interestRate + additionalRate
   |
   v
6. Calculate Charges (if any)
   └─> Fetch product.charges where applicableTransactionTypes contains "INITIAL_DEPOSIT"
   └─> Calculate fixed or percentage-based charges
   |
   v
7. Calculate TDS (if applicable)
   └─> If product.tdsApplicable = true
   └─> If interest >= ₹40,000 threshold
   └─> Calculate TDS = (interest × product.tdsRate) / 100
   |
   v
8. Create FD Account
   └─> Store productId, calculated rate, term, amount
   └─> Create account roles from validated roles
   └─> Apply charges (deduct from deposit or add to fee ledger)
```

### Key Validations Enforced by Product Module

| Validation | Field(s) Used | When Checked |
|------------|---------------|--------------|
| Minimum deposit amount | `minAmount` | Account creation, additional deposit |
| Maximum deposit amount | `maxAmount` | Account creation, additional deposit |
| Minimum term | `minTermMonths` | Account creation |
| Maximum term | `maxTermMonths` | Account creation |
| Premature withdrawal allowed | `prematureWithdrawalAllowed` | Premature closure request |
| Partial withdrawal allowed | `partialWithdrawalAllowed` | Partial withdrawal request |
| Minimum balance for withdrawal | `minBalanceRequired` | Partial withdrawal calculation |
| Role requirements | `allowedRoles` | Account creation |
| TDS applicability | `tdsApplicable`, `tdsRate` | Interest calculation |
| Product active status | `status`, `effectiveDate` | All operations |

---

## 7. Important Notes

### ✅ What This Module DOES
- ✅ Defines product templates for FD accounts
- ✅ Stores business rules and constraints
- ✅ Manages interest rate configurations
- ✅ Defines allowed roles and their validations
- ✅ Specifies charges/fees structure
- ✅ Validates account creation requests
- ✅ Provides rate calculation logic

### ❌ What This Module DOES NOT DO
- ❌ Create actual FD accounts (done by fd-account-service)
- ❌ Process transactions (done by fd-account-service)
- ❌ Calculate daily interest accruals (done by fd-account-service)
- ❌ Handle maturity processing (done by fd-account-service)
- ❌ Manage customer data (done by customer-service)
- ❌ Track account balances (done by fd-account-service)

### Recently Removed Features

#### 1. Balance Type and Transaction Type Entities (Removed in Cleanup #1)
The following entities were removed as they were over-engineered for FD products and not used by fd-account-service:
- ❌ `ProductBalanceType` - Configuration for which balance types to track
- ❌ `ProductTransactionType` - Configuration for allowed transaction types
- ❌ `TransactionBalanceRelationship` - Mapping of transactions to balance impacts
- ❌ `BalanceType` enum - 9 balance type definitions
- ❌ `TransactionType` enum - 12 transaction type definitions

**Reason for removal:** FD-account-service uses its own hardcoded transaction types and balance tracking logic. These configuration entities added complexity without providing value for simple FD products.

#### 2. Validation-Related Fields (Removed in Cleanup #2)
The following fields were removed to simplify product configuration and reduce unnecessary validation complexity:

**From ProductRole entity:**
- ❌ `mandatory` (Boolean) - Whether role is mandatory for account creation
- ❌ `minCount` (Integer) - Minimum number of this role type required
- ❌ `maxCount` (Integer) - Maximum number of this role type allowed

**From ProductCharge entity:**
- ❌ `minCharge` (BigDecimal) - Minimum charge amount for capping
- ❌ `maxCharge` (BigDecimal) - Maximum charge amount for capping

**From InterestRateMatrix entity:**
- ❌ `minAmount` (BigDecimal) - Minimum deposit amount for rate slab
- ❌ `maxAmount` (BigDecimal) - Maximum deposit amount for rate slab
- ❌ `minTermMonths` (BigDecimal) - Minimum term duration for rate slab
- ❌ `maxTermMonths` (BigDecimal) - Maximum term duration for rate slab
- ❌ `endDate` (LocalDate) - Rate expiry date
- ❌ `remarks` (String) - Additional notes about the rate

**From Product entity:**
- ❌ `endDate` (LocalDate) - Product expiry date

**Impact on Services:**
- AccountCreationService.validateAccountRoles() simplified from 56 lines to 24 lines (57% reduction)
- Role validation now only checks if provided roles are in the allowed list
- Removed mandatory role checking, min/max count enforcement
- Interest rate selection simplified from 4 parameters to 2 (removed amount/term range filtering)
- Charge calculation simplified (removed min/max capping logic)
- Product lifecycle simplified (only effectiveDate, no endDate)

**Reason for removal:** These fields added over-engineered validation complexity that wasn't providing value:
- Role validation (mandatory/min/max counts) was too restrictive and not used by business
- Charge min/max capping was unnecessary - charges are either fixed or percentage-based
- Interest rate slabs by amount/term added complexity without business need (rates vary only by customer classification)
- Product/rate endDate field caused confusion with effectiveDate and wasn't actively used
- Remarks field in InterestRateMatrix was never populated or used

---

## 8. API Endpoints (Quick Reference)

### Product Management
- `POST /api/products` - Create new product (ADMIN, MANAGER)
- `GET /api/products/{id}` - Get product by ID (ALL ROLES)
- `PUT /api/products/{id}` - Update product (ADMIN, MANAGER)
- `DELETE /api/products/{id}` - Soft delete (ADMIN, MANAGER)
- `DELETE /api/products/{id}/hard` - Hard delete (ADMIN only)
- `GET /api/products` - List all products (ALL ROLES)
- `GET /api/products/active` - List active products (ALL ROLES)
- `GET /api/products/code/{code}` - Get by product code (ALL ROLES)
- `POST /api/products/search` - Search products (ALL ROLES)

### Interest Rate Matrix
- `POST /api/interest-rates/product/{productId}` - Add rate slab (ADMIN, MANAGER)
- `GET /api/interest-rates/product/{productId}` - Get all rates for product (ALL ROLES)
- `PUT /api/interest-rates/{id}` - Update rate slab (ADMIN, MANAGER)
- `DELETE /api/interest-rates/{id}` - Delete rate slab (ADMIN, MANAGER)

### Product Charges
- `POST /api/charges/product/{productId}` - Add charge (ADMIN, MANAGER)
- `GET /api/charges/product/{productId}` - Get charges for product (ALL ROLES)
- `PUT /api/charges/{id}` - Update charge (ADMIN, MANAGER)
- `DELETE /api/charges/{id}` - Delete charge (ADMIN, MANAGER)

### Product Roles
- `POST /api/roles/product/{productId}` - Add role (ADMIN, MANAGER)
- `GET /api/roles/product/{productId}` - Get roles for product (ALL ROLES)
- `PUT /api/roles/{id}` - Update role (ADMIN, MANAGER)
- `DELETE /api/roles/{id}` - Delete role (ADMIN, MANAGER)

---

## 9. Example: Complete Product Configuration

```json
{
  "productName": "Premium Senior Citizen Fixed Deposit",
  "productCode": "FD-SC-PREMIUM-001",
  "productType": "SENIOR_CITIZEN_FD",
  "description": "Special FD for senior citizens with higher interest rates",
  "effectiveDate": "2024-01-01",
  "currencyCode": "INR",
  "status": "ACTIVE",
  
  "minTermMonths": 6,
  "maxTermMonths": 120,
  "minAmount": 25000.00,
  "maxAmount": 50000000.00,
  "minBalanceRequired": 10000.00,
  
  "baseInterestRate": 6.50,
  "interestCalculationMethod": "COMPOUND",
  "interestPayoutFrequency": "QUARTERLY",
  
  "prematureWithdrawalAllowed": true,
  "partialWithdrawalAllowed": true,
  "autoRenewalAllowed": true,
  
  "tdsRate": 10.00,
  "tdsApplicable": true,
  
  "allowedRoles": [
    {
      "roleType": "OWNER",
      "description": "Primary account owner"
    },
    {
      "roleType": "CO_OWNER",
      "description": "Optional co-owner for joint account"
    },
    {
      "roleType": "NOMINEE",
      "description": "Optional nominees for succession"
    }
  ],
  
  "charges": [
    {
      "chargeName": "Account Opening Fee",
      "chargeType": "FEE",
      "fixedAmount": 500.00,
      "frequency": "ONE_TIME",
      "active": true
    },
    {
      "chargeName": "Premature Closure Penalty",
      "chargeType": "PENALTY",
      "percentageRate": 1.00,
      "frequency": "ONE_TIME",
      "applicableTransactionTypes": "PREMATURE_WITHDRAWAL",
      "active": true
    }
  ],
  
  "interestRateMatrix": [
    {
      "customerClassification": "SENIOR_CITIZEN",
      "interestRate": 6.00,
      "additionalRate": 0.50,
      "effectiveDate": "2024-01-01",
      "active": true
    },
    {
      "customerClassification": "SUPER_SENIOR",
      "interestRate": 6.50,
      "additionalRate": 0.75,
      "effectiveDate": "2024-01-01",
      "active": true
    },
    {
      "customerClassification": "REGULAR",
      "interestRate": 5.50,
      "additionalRate": 0.00,
      "effectiveDate": "2024-01-01",
      "active": true
    }
  ]
}
```

**Result:**
- Senior citizens get 6.5% p.a. (6.0% base + 0.5% additional)
- Super senior citizens get 7.25% p.a. (6.5% base + 0.75% additional)
- Regular customers get 5.5% p.a.
- Account opening fee: ₹500
- Premature closure penalty: 1% of principal
- TDS @ 10% if interest >= ₹40,000

---

## Revision History
- **Version 1.0** - Initial documentation (2024-11-08)
  - Created comprehensive field guide for product-pricing module
  
- **Version 1.1** - Balance/Transaction Type Cleanup (2024-11-08)
  - Removed: Balance types and transaction types configuration entities (20 files deleted)
  - Reason: Over-engineered for FD products, not used by fd-account-service
  
- **Version 1.2** - Validation Field Simplification (2024-11-08)
  - Removed 12 validation-related fields across 4 entities:
    - ProductRole: mandatory, minCount, maxCount
    - ProductCharge: minCharge, maxCharge
    - InterestRateMatrix: minAmount, maxAmount, minTermMonths, maxTermMonths, endDate, remarks
    - Product: endDate
  - Simplified AccountCreationService.validateAccountRoles() by 57% (56 lines → 24 lines)
  - Simplified interest rate selection from 4 parameters to 2 parameters
  - Reason: Removed over-engineered validation complexity that wasn't providing business value

---

**For More Information:**
- See `ARCHITECTURE.md` in product-pricing-service for technical architecture
- See `TESTING-GUIDE.md` for API testing examples
- See `RBAC-TEST-CASES.md` for role-based access control testing
