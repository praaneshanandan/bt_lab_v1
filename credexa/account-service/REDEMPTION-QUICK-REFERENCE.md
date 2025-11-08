# Redemption Features - Quick Reference Card

## 🎯 Two Features Implemented

### Feature 11: Redemption Inquiry ✅
**Endpoint**: `POST /api/redemptions/inquiry`  
**Purpose**: Get complete redemption calculation  
**Roles**: CUSTOMER, MANAGER, ADMIN

**Request:**
```json
{
  "idType": "ACCOUNT_NUMBER",  // or IBAN, INTERNAL_ID
  "idValue": "FD-20251108120000-1234-5"
}
```

**Response Includes:**
- Current balance
- Interest earned (from transactions)
- TDS deducted (from transactions)
- Penalty amount (0.5% if premature)
- **Net redemption amount**
- Days elapsed/remaining
- Redemption type (PREMATURE/ON_MATURITY/POST_MATURITY)

---

### Feature 12: Redemption Process ✅
**Endpoint**: `POST /api/redemptions/process`  
**Purpose**: Execute full or partial redemption  
**Roles**: MANAGER, ADMIN (CUSTOMER ❌)

**Request (Full):**
```json
{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "FULL",
  "paymentReference": "PAY-001",
  "remarks": "Customer request"
}
```

**Request (Partial):**
```json
{
  "idValue": "FD-20251108120000-1234-5",
  "redemptionType": "PARTIAL",
  "redemptionAmount": 50000.00,
  "paymentReference": "PAY-002"
}
```

**What Happens:**
- **FULL**: Account → CLOSED, CLOSURE transaction created, balance → 0
- **PARTIAL**: Account → ACTIVE, WITHDRAWAL transaction created, balance reduced

---

## 📊 Quick Calculation

### Net Redemption Formula
```
Current Balance = Last transaction's balanceAfter (or principal)
Interest Earned = SUM(INTEREST_CREDIT transactions)
TDS Deducted   = SUM(TDS_DEDUCTION transactions)
Penalty        = Interest × 0.5% (if premature)

Net Redemption = Balance + Interest - TDS - Penalty
```

### Example
```
Balance:  100,000.00
Interest:   7,500.00
TDS:         -750.00
Penalty:      -37.50  (0.5% of 7,500)
─────────────────────
Net:      106,712.50
```

---

## 🔐 Access Control

| Feature | CUSTOMER | MANAGER | ADMIN |
|---------|----------|---------|-------|
| Inquiry | ✅ View  | ✅ View | ✅ View |
| Process | ❌ No    | ✅ Yes  | ✅ Yes  |

---

## ⚠️ Validations

### Inquiry
- ❌ Account must not be CLOSED
- ❌ Account must not be SUSPENDED

### Process (Full)
- ❌ Account must be ACTIVE
- ❌ CUSTOMER role cannot process

### Process (Partial)
- ❌ Redemption amount required
- ❌ Amount must be > 0
- ❌ Amount ≤ net redemption amount
- ❌ Remaining balance ≥ 10% of principal

---

## 📍 Redemption Types

| Type | Condition | Penalty |
|------|-----------|---------|
| **PREMATURE** | Before maturity | 0.5% on interest |
| **ON_MATURITY** | On maturity date | No penalty |
| **POST_MATURITY** | After maturity | No penalty |

---

## 🚀 Quick Test Flow

### Step 1: Setup Account
```bash
# Create account
POST /api/accounts/create/default
{ "customerId": 101, "productCode": "FD-5YR", "principalAmount": 100000, "termMonths": 60 }

# Add interest
POST /api/transactions/create?idValue=<accountNumber>
{ "transactionType": "INTEREST_CREDIT", "amount": 7500 }

# Add TDS
POST /api/transactions/create?idValue=<accountNumber>
{ "transactionType": "TDS_DEDUCTION", "amount": 750 }
```

### Step 2: Check Redemption
```bash
POST /api/redemptions/inquiry
{ "idValue": "<accountNumber>" }

# Note the netRedemptionAmount
```

### Step 3: Process Redemption
```bash
# Option A: Full redemption
POST /api/redemptions/process
{ "idValue": "<accountNumber>", "redemptionType": "FULL", "paymentReference": "PAY-001" }

# Option B: Partial redemption
POST /api/redemptions/process
{ "idValue": "<accountNumber>", "redemptionType": "PARTIAL", "redemptionAmount": 50000, "paymentReference": "PAY-002" }
```

### Step 4: Verify
```bash
# Check account status
GET /api/accounts/inquiry?idValue=<accountNumber>

# Check transactions
GET /api/transactions/list?idValue=<accountNumber>
```

---

## 📋 Transaction Types Created

| Redemption Type | Transaction Type | Account Status |
|----------------|------------------|----------------|
| FULL | CLOSURE | CLOSED |
| PARTIAL | WITHDRAWAL | ACTIVE |

---

## 💡 Tips

1. **Always inquiry first** to see exact amounts before processing
2. **Check penalty** - premature = 0.5% on interest
3. **Minimum balance** - partial redemption requires 10% principal remaining
4. **Account closure** - FULL sets closureDate and CLOSED status
5. **Use IBAN** - works same as account number for all redemption APIs

---

## 📚 Documentation

- **Full Testing Guide**: REDEMPTION-TESTING-GUIDE.md
- **Implementation Details**: REDEMPTION-IMPLEMENTATION-SUMMARY.md
- **API Documentation**: http://localhost:8087/swagger-ui.html

---

**Quick Access URLs** (when running):
- Swagger UI: http://localhost:8087/swagger-ui.html
- Health Check: http://localhost:8087/api/accounts/health

---

**Status**: ✅ Ready for Testing  
**Version**: 1.0.0  
**Last Updated**: November 8, 2025
