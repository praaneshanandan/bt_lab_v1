# Account Service UI - Quick Reference Guide

## 🚀 Quick Start

### Access the New Features

1. **My Accounts:** Navigate to `/fd-accounts` or click "My Accounts" in sidebar
2. **Transactions:** Navigate to `/transactions` or click "Transactions" in sidebar
3. **Redemptions:** Navigate to `/redemptions` or click "Redemptions" in sidebar
4. **Batch Management (Admin):** Navigate to `/batch-management` or click "Batch Management" in sidebar

### User Roles & Access

- **CUSTOMER:** Can view own accounts, transactions, inquire redemptions
- **MANAGER/ADMIN:** Full access to all features including batch management

## 📡 API Reference

### Import API Service

```typescript
import { accountServiceApi } from "@/services/api";
```

### Example API Calls

#### Get Customer's Accounts

```typescript
const customerId = localStorage.getItem("customerId");
const response = await accountServiceApi.getAccountsByCustomerId(
  parseInt(customerId)
);
const accounts = response.data;
```

#### Create Default Account

```typescript
const data = {
  customerId: parseInt(localStorage.getItem("customerId")),
  productCode: "FD001",
  principalAmount: 50000,
  nomineeDetails: "John Doe - Son",
};
const response = await accountServiceApi.createDefaultAccount(data);
```

#### Get Account Balance

```typescript
const response = await accountServiceApi.getAccountBalance("FD123456789");
const balance = response.data;
// { principalAmount, accruedInterest, totalBalance, maturityAmount, accountStatus }
```

#### Inquire Redemption

```typescript
const data = {
  accountNumber: "FD123456789",
  redemptionDate: "2024-01-15", // Optional
};
const response = await accountServiceApi.inquireRedemption(data);
// Returns: { principalAmount, accruedInterest, penaltyAmount, netRedemptionAmount, isPremature }
```

#### Process Redemption (Admin Only)

```typescript
const data = {
  accountNumber: "FD123456789",
  redemptionDate: "2024-01-15",
  reason: "Medical emergency",
};
const response = await accountServiceApi.processRedemption(data);
// Returns: { accountNumber, transactionReference, redemptionAmount, penaltyApplied, status }
```

#### Get All Transactions (Admin)

```typescript
const response = await accountServiceApi.getAllTransactions();
const transactions = response.data;
```

#### Get Transactions by Account

```typescript
const response = await accountServiceApi.getTransactionsByAccount(
  "FD123456789"
);
const transactions = response.data;
```

#### Trigger Batch Processing (Admin Only)

```typescript
// Maturity Processing
await accountServiceApi.triggerMaturityProcessing();

// Interest Capitalization
await accountServiceApi.triggerInterestCapitalization();

// Interest Accrual
await accountServiceApi.triggerInterestAccrual();
```

#### Time Travel (Admin Only)

```typescript
// Set time travel
await accountServiceApi.setTimeTravel({ targetDate: "2024-06-01" });

// Get status
const status = await accountServiceApi.getTimeTravelStatus();

// Clear time travel
await accountServiceApi.clearTimeTravel();
```

## 🎨 UI Components Patterns

### Loading State

```typescript
const [loading, setLoading] = useState(false);

// In render
{
  loading && (
    <div className="flex items-center justify-center h-96">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
    </div>
  );
}
```

### Error Display

```typescript
const [error, setError] = useState<string | null>(null);

// In render
{
  error && (
    <div className="bg-destructive/10 border border-destructive/30 text-destructive rounded-lg p-4 flex items-center gap-3">
      <AlertCircle className="w-5 h-5 flex-shrink-0" />
      <p>{error}</p>
    </div>
  );
}
```

### Currency Formatting

```typescript
const formatCurrency = (amount: number) => {
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(amount);
};

// Usage: formatCurrency(50000) => "₹50,000.00"
```

### Date Formatting

```typescript
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString("en-IN", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
};

// Usage: formatDate('2024-01-15') => "Jan 15, 2024"
```

### Status Badge

```typescript
const getStatusColor = (status: string) => {
  switch (status) {
    case "ACTIVE":
      return "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400";
    case "MATURED":
      return "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400";
    case "CLOSED":
      return "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400";
    default:
      return "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400";
  }
};

// In render
<span
  className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(
    status
  )}`}
>
  {status}
</span>;
```

## 🔐 RBAC Checks

### Check User Role

```typescript
const userRole = localStorage.getItem("userRole") || "CUSTOMER";
const isAdmin = userRole === "ADMIN";
const isManager = userRole === "MANAGER";
const canAccessAdminFeatures = isAdmin || isManager;
```

### Conditional Rendering

```typescript
{
  canAccessAdminFeatures && (
    <button onClick={triggerBatch}>Trigger Batch Processing</button>
  );
}

{
  !canAccessAdminFeatures && (
    <p>Contact administrator to process this action</p>
  );
}
```

### Page-Level Protection

```typescript
useEffect(() => {
  if (userRole !== "ADMIN" && userRole !== "MANAGER") {
    window.location.href = "/dashboard";
  }
}, [userRole]);
```

## 📋 TypeScript Types

### Import Types

```typescript
import type {
  FDAccount,
  FDTransaction,
  AccountBalanceResponse,
  RedemptionInquiryResponse,
  CreateDefaultAccountRequest,
} from "@/types";
```

### Type Usage Examples

```typescript
// Account
const account: FDAccount = {
  accountNumber: "FD123456789",
  customerId: 1,
  productCode: "FD001",
  principalAmount: 50000,
  interestRate: 6.5,
  tenure: 12,
  tenureUnit: "MONTHS",
  maturityDate: "2025-01-15",
  maturityAmount: 53250,
  accountStatus: "ACTIVE",
  openingDate: "2024-01-15",
  createdAt: "2024-01-15T10:00:00",
  updatedAt: "2024-01-15T10:00:00",
};

// Transaction
const transaction: FDTransaction = {
  id: 1,
  transactionReference: "TXN123456",
  accountNumber: "FD123456789",
  transactionType: "OPENING",
  transactionAmount: 50000,
  transactionDate: "2024-01-15T10:00:00",
  transactionStatus: "COMPLETED",
  description: "FD Account Opening",
  createdAt: "2024-01-15T10:00:00",
  updatedAt: "2024-01-15T10:00:00",
};
```

## 🎯 Common Patterns

### Fetch Data on Mount

```typescript
useEffect(() => {
  const fetchData = async () => {
    try {
      setLoading(true);
      const response = await accountServiceApi.getAccountsByCustomerId(
        customerId
      );
      setAccounts(response.data);
    } catch (err) {
      setError("Failed to load accounts");
    } finally {
      setLoading(false);
    }
  };

  fetchData();
}, [customerId]);
```

### Form Submission

```typescript
const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();
  setLoading(true);
  setError(null);

  try {
    const data = {
      customerId: parseInt(localStorage.getItem("customerId")),
      productCode: formData.productCode,
      principalAmount: parseFloat(formData.principalAmount),
    };

    await accountServiceApi.createDefaultAccount(data);

    // Show success message
    toast.success("Account created successfully");

    // Refresh data or navigate
    await fetchAccounts();
  } catch (err: any) {
    setError(err.response?.data?.message || "Failed to create account");
  } finally {
    setLoading(false);
  }
};
```

### Confirmation Dialog

```typescript
const handleDelete = async (accountNumber: string) => {
  if (!confirm("Are you sure you want to process this redemption?")) {
    return;
  }

  // Proceed with deletion
  await accountServiceApi.processRedemption({ accountNumber });
};
```

## 🔄 Kafka Integration (Future)

### Event Publishing Structure (Ready to Use)

```typescript
// Will be implemented when backend Kafka is ready
interface KafkaEvent {
  eventType: "ACCOUNT_CREATED" | "REDEMPTION_PROCESSED";
  payload: any;
  timestamp: string;
  userId: number;
}

const publishEvent = async (event: KafkaEvent) => {
  // Backend will publish to Kafka
  // Frontend will receive via WebSocket
  console.log("Event ready:", event);
};

// Usage example (when implemented)
await accountServiceApi.createDefaultAccount(data);
publishEvent({
  eventType: "ACCOUNT_CREATED",
  payload: data,
  timestamp: new Date().toISOString(),
  userId: parseInt(localStorage.getItem("userId")),
});
```

## 🐛 Debugging Tips

### Check API Responses

```typescript
try {
  const response = await accountServiceApi.getAccountBalance(accountNumber);
  console.log("API Response:", response);
  console.log("Balance Data:", response.data);
} catch (err) {
  console.error("API Error:", err);
  console.error("Error Response:", err.response?.data);
}
```

### Verify Token

```typescript
const token = localStorage.getItem("authToken");
console.log("Auth Token:", token);
console.log("Token Length:", token?.length);
```

### Check Backend Connection

```bash
# Test Account Service health
curl http://localhost:8087/accounts/health

# Test with authentication
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost:8087/accounts
```

### Browser DevTools

1. **Network Tab:** Monitor API calls and responses
2. **Console:** Check for errors and logs
3. **Application Tab:** Verify localStorage values

## 📊 Performance Tips

### Pagination (To Implement)

```typescript
const [page, setPage] = useState(1);
const [pageSize] = useState(20);

// Filter data for current page
const paginatedData = filteredAccounts.slice(
  (page - 1) * pageSize,
  page * pageSize
);
```

### Debounce Search

```typescript
import { useState, useEffect } from "react";

const [searchTerm, setSearchTerm] = useState("");
const [debouncedSearch, setDebouncedSearch] = useState("");

useEffect(() => {
  const timer = setTimeout(() => {
    setDebouncedSearch(searchTerm);
  }, 300);

  return () => clearTimeout(timer);
}, [searchTerm]);

// Use debouncedSearch for filtering
```

## 📝 Common Error Codes

| Status Code | Meaning      | Solution                          |
| ----------- | ------------ | --------------------------------- |
| 401         | Unauthorized | Check if JWT token is valid       |
| 403         | Forbidden    | User doesn't have required role   |
| 404         | Not Found    | Account/transaction doesn't exist |
| 500         | Server Error | Backend service issue, check logs |

## 🔗 Useful Links

- **Account Service Port:** http://localhost:8087
- **Account Service Health:** http://localhost:8087/accounts/health
- **Swagger Docs:** (If available at `/swagger-ui.html`)
- **Backend Logs:** Check service console for errors

## 💡 Best Practices

1. **Always validate input** before API calls
2. **Use loading states** for better UX
3. **Display clear error messages** to users
4. **Implement confirmation dialogs** for destructive actions
5. **Log errors** to console for debugging
6. **Use TypeScript types** for type safety
7. **Follow RBAC rules** strictly
8. **Test in both themes** (light and dark)
9. **Handle edge cases** (empty lists, network errors)
10. **Keep API calls in try-catch blocks**

---

**Last Updated:** Current Session
**Version:** 1.0.0
**Maintained By:** Development Team
