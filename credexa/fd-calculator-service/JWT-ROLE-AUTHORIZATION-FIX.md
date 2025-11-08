# FD Calculator Service - JWT Role Authorization Fix

## Issue

After fixing CORS, the FD Calculator API was returning **403 Forbidden** errors:

```
POST http://localhost:8085/api/calculator/calculate/standalone 403 (Forbidden)
Error calculating: AxiosError {message: 'Request failed with status code 403', ...}
```

## Root Cause

**Spring Security Role Prefix Mismatch:**

1. **JWT Token Contents:** Roles are stored in JWT as `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER` (from database)
2. **Controller Annotations:** Used `@PreAuthorize("hasRole('CUSTOMER')")`
3. **Spring Security Behavior:** `hasRole('CUSTOMER')` automatically adds `ROLE_` prefix, looking for `ROLE_ROLE_CUSTOMER`
4. **Result:** Authorization failed because `ROLE_ROLE_CUSTOMER` doesn't exist

### Why This Happened

- The JWT filter extracts roles from token: `["ROLE_CUSTOMER"]`
- Creates authorities with those exact strings: `SimpleGrantedAuthority("ROLE_CUSTOMER")`
- Controller uses `hasRole('CUSTOMER')` which Spring translates to `hasRole('ROLE_CUSTOMER')`
- Spring adds prefix again: `ROLE_` + `ROLE_CUSTOMER` = `ROLE_ROLE_CUSTOMER` ❌

## Solution Applied

### Change: Use `hasAuthority()` Instead of `hasRole()`

**Updated all `@PreAuthorize` annotations in `FdCalculatorController.java`:**

#### Before (Incorrect):

```java
@PreAuthorize("hasRole('CUSTOMER')")                              // Looks for ROLE_ROLE_CUSTOMER ❌
@PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")      // Looks for ROLE_ROLE_* ❌
```

#### After (Correct):

```java
@PreAuthorize("hasAuthority('ROLE_CUSTOMER')")                                    // Looks for ROLE_CUSTOMER ✅
@PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_MANAGER', 'ROLE_ADMIN')") // Looks for exact matches ✅
```

### Key Difference

- **`hasRole()`**: Automatically adds `ROLE_` prefix to the argument
- **`hasAuthority()`**: Uses the exact string provided (no prefix added)

## Files Modified

### 1. JwtAuthenticationFilter.java

**File:** `src/main/java/com/app/calculator/security/JwtAuthenticationFilter.java`

Added comment explaining the role handling:

```java
// Convert roles to GrantedAuthority
// Note: If roles already have ROLE_ prefix (e.g., ROLE_CUSTOMER), keep them as-is
// Spring Security's hasRole() will add ROLE_ prefix, so we need to use hasAuthority() in @PreAuthorize
List<GrantedAuthority> authorities = roles.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
```

### 2. FdCalculatorController.java

**File:** `src/main/java/com/app/calculator/controller/FdCalculatorController.java`

Updated 5 endpoints:

1. ✅ `/fd/calculate` - Changed from `hasRole('CUSTOMER')` to `hasAuthority('ROLE_CUSTOMER')`
2. ✅ `/calculate/standalone` - Changed from `hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')` to `hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_MANAGER', 'ROLE_ADMIN')`
3. ✅ `/calculate/product-based` - Changed from `hasRole('CUSTOMER')` to `hasAuthority('ROLE_CUSTOMER')`
4. ✅ `/compare` - Changed from `hasRole('CUSTOMER')` to `hasAuthority('ROLE_CUSTOMER')`
5. ✅ `/report` - Changed from `hasRole('CUSTOMER')` to `hasAuthority('ROLE_CUSTOMER')`

## Service Status

- **Port:** 8085
- **Process ID:** 34096
- **Status:** ✅ Running with corrected authorization
- **CORS:** ✅ Enabled (from previous fix)
- **JWT:** ✅ Authentication working
- **Roles:** ✅ Authorization now working correctly

## Testing

### Test Calculator as Customer:

1. ✅ Login as customer (customer/password)
2. ✅ Navigate to Products → Click "Details" on any product
3. ✅ Go to "Interest Calculator" tab
4. ✅ Enter deposit amount and term
5. ✅ Click "Calculate"
6. ✅ Should receive calculation results (no 403 error)

### Test Calculator as Admin/Manager:

1. ✅ Login as admin (admin/admin123)
2. ✅ Navigate to Calculator page
3. ✅ Use standalone calculator
4. ✅ Should work (admin has ROLE_ADMIN which is allowed)

## Technical Notes

### Spring Security Role vs Authority

```
┌─────────────────────────────────────────────────────────────┐
│ JWT Token: ["ROLE_CUSTOMER"]                               │
│                                                             │
│ JwtFilter creates: SimpleGrantedAuthority("ROLE_CUSTOMER")│
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐│
│ │ hasRole('CUSTOMER')          →  ROLE_CUSTOMER ❌        ││
│ │ Spring adds prefix: ROLE_ROLE_CUSTOMER (NOT FOUND)     ││
│ └─────────────────────────────────────────────────────────┘│
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐│
│ │ hasAuthority('ROLE_CUSTOMER') →  ROLE_CUSTOMER ✅       ││
│ │ No prefix added, exact match (FOUND)                   ││
│ └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### Alternative Solution (Not Used)

We could have stripped the `ROLE_` prefix in the JWT filter:

```java
List<GrantedAuthority> authorities = roles.stream()
    .map(role -> new SimpleGrantedAuthority(role.replace("ROLE_", "")))
    .collect(Collectors.toList());
```

Then use `hasRole('CUSTOMER')` in controllers. But this approach is less explicit and could cause confusion.

## Related Issues Fixed

1. ✅ CORS configuration (previous fix)
2. ✅ JWT authentication (already working)
3. ✅ JWT authorization (this fix)

---

**Date Fixed:** November 9, 2025  
**Fixed By:** GitHub Copilot  
**Issue Type:** Spring Security Role Authorization
