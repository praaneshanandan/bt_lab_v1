# ROLE MIGRATION SUMMARY - CREDEXA FD BANKING

**Date:** 2025-11-07
**Migration:** From 6 roles to 3 simplified roles
**Services Updated:** Login Service, Customer Service

---

## Overview

The Credexa Fixed Deposit Banking application has been migrated from a complex 6-role system to a simplified 3-role system for better clarity and maintainability.

---

## Role Mapping

### Old Roles → New Roles

| Old Role | New Role | Purpose | Notes |
|----------|----------|---------|-------|
| ROLE_USER | **ROLE_CUSTOMER** | Regular customer access | Renamed for clarity |
| ROLE_CUSTOMER_MANAGER | **ROLE_MANAGER** | Operations manager | Simplified name |
| ROLE_PRODUCT_MANAGER | **ROLE_MANAGER** | Merged into ROLE_MANAGER | Consolidated |
| ROLE_FD_MANAGER | **ROLE_MANAGER** | Merged into ROLE_MANAGER | Consolidated |
| ROLE_REPORT_VIEWER | **ROLE_MANAGER** | Merged into ROLE_MANAGER | Consolidated |
| ROLE_ADMIN | **ROLE_ADMIN** | System administrator | No change |

---

## New Role System

### ROLE_CUSTOMER
**Description:** Regular customer with basic access to own data

**Permissions:**
- ✅ Create own customer profile
- ✅ View own customer profile
- ✅ Update own customer profile
- ✅ View own FD accounts
- ✅ Create FD accounts
- ✅ View own transactions
- ✅ Use calculator services
- ❌ Cannot view other customers' data
- ❌ Cannot access admin functions

**Default Role:** Assigned automatically on user registration

---

### ROLE_MANAGER
**Description:** Operations manager with access to manage customers, products, and FD accounts

**Permissions:**
- ✅ All CUSTOMER permissions
- ✅ View all customers
- ✅ Create customer profiles for any user
- ✅ Update any customer profile
- ✅ View customer 360° view
- ✅ View customer classifications
- ✅ Manage products and pricing
- ✅ Manage FD accounts
- ✅ View reports
- ✅ Manage interest rates
- ❌ Cannot manage users or roles
- ❌ Cannot access system configuration

**Assignment:** Manual assignment by admin

---

### ROLE_ADMIN
**Description:** System administrator with full access to all functions

**Permissions:**
- ✅ All MANAGER permissions
- ✅ Create/update/delete users
- ✅ Assign/revoke roles
- ✅ Unlock locked accounts
- ✅ View audit logs
- ✅ Modify system configuration
- ✅ Access all endpoints
- ✅ Override security restrictions

**Assignment:** Created during system initialization

---

## Files Modified

### Login Service (4 files)

#### 1. Role.java
**File:** `login-service/src/main/java/com/app/login/entity/Role.java`

**Changes:**
- Updated `RoleName` enum from 6 roles to 3 roles
- Added comments for each role

**Before:**
```java
public enum RoleName {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_CUSTOMER_MANAGER,
    ROLE_PRODUCT_MANAGER,
    ROLE_FD_MANAGER,
    ROLE_REPORT_VIEWER
}
```

**After:**
```java
public enum RoleName {
    ROLE_ADMIN,      // System administrator - full access
    ROLE_MANAGER,    // Operations manager - manage customers, products, FDs
    ROLE_CUSTOMER    // Regular customer - basic access to own data
}
```

#### 2. AuthService.java
**File:** `login-service/src/main/java/com/app/login/service/AuthService.java`

**Changes:**
- Updated default role assignment from `ROLE_USER` to `ROLE_CUSTOMER`

**Before:**
```java
Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
        .orElseThrow(() -> new RuntimeException("Default role not found"));
user.getRoles().add(userRole);
```

**After:**
```java
Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
        .orElseThrow(() -> new RuntimeException("Default role not found"));
user.getRoles().add(customerRole);
```

#### 3. application.yml
**File:** `login-service/src/main/resources/application.yml`

**Changes:**
- Externalized JWT secret
- Externalized database credentials
- Added admin configuration section

**Before:**
```yaml
datasource:
  username: root
  password: root

jwt:
  secret: mySecretKeyForCredexaApplicationMustBe256BitsLongForHS256AlgorithmToWorkProperly
  expiration: 3600000
```

**After:**
```yaml
datasource:
  username: ${DB_USERNAME:root}
  password: ${DB_PASSWORD:root}

jwt:
  secret: ${JWT_SECRET:mySecretKeyForCredexaApplicationMustBe256BitsLongForHS256AlgorithmToWorkProperly}
  expiration: ${JWT_EXPIRATION:3600000}

admin:
  create-default: ${ADMIN_CREATE_DEFAULT:true}
  username: ${ADMIN_USERNAME:admin}
  password: ${ADMIN_PASSWORD:Admin@123}
  email: ${ADMIN_EMAIL:admin@credexa.com}
```

#### 4. DataInitializer.java
**File:** `login-service/src/main/java/com/app/login/config/DataInitializer.java`

**Changes:**
- Made admin creation configurable
- Added security warnings
- Externalized admin credentials

**Impact:** Default admin can be disabled in production

---

### Customer Service (3 files)

#### 1. SecurityConfig.java
**File:** `customer-service/src/main/java/com/app/customer/config/SecurityConfig.java`

**Changes:**
- Updated base role requirements

**Before:**
```java
.requestMatchers("/**").hasAnyRole("USER", "CUSTOMER_MANAGER", "ADMIN")
```

**After:**
```java
.requestMatchers("/**").hasAnyRole("CUSTOMER", "MANAGER", "ADMIN")
```

#### 2. CustomerController.java
**File:** `customer-service/src/main/java/com/app/customer/controller/CustomerController.java`

**Changes:**
- Updated all `@PreAuthorize` annotations (7 endpoints)

**Endpoint Updates:**

| Endpoint | Old Roles | New Roles |
|----------|-----------|-----------|
| GET /all | CUSTOMER_MANAGER, ADMIN | MANAGER, ADMIN |
| POST / | USER, CUSTOMER_MANAGER, ADMIN | CUSTOMER, MANAGER, ADMIN |
| GET /{id} | USER, CUSTOMER_MANAGER, ADMIN | CUSTOMER, MANAGER, ADMIN |
| GET /user/{userId} | USER, CUSTOMER_MANAGER, ADMIN | CUSTOMER, MANAGER, ADMIN |
| PUT /{id} | USER, CUSTOMER_MANAGER, ADMIN | CUSTOMER, MANAGER, ADMIN |
| GET /{id}/classification | USER, CUSTOMER_MANAGER, ADMIN, FD_MANAGER | CUSTOMER, MANAGER, ADMIN |
| GET /{id}/360-view | CUSTOMER_MANAGER, ADMIN | MANAGER, ADMIN |

**Note:** ROLE_FD_MANAGER removed entirely as it's merged with ROLE_MANAGER

#### 3. application.yml
**File:** `customer-service/src/main/resources/application.yml`

**Changes:**
- Externalized JWT secret
- Externalized database credentials

**Before:**
```yaml
datasource:
  username: root
  password: root

jwt:
  secret: mySecretKeyForCredexaApplicationMustBe256BitsLongForHS256AlgorithmToWorkProperly
  expiration: 3600000
```

**After:**
```yaml
datasource:
  username: ${DB_USERNAME:root}
  password: ${DB_PASSWORD:root}

jwt:
  secret: ${JWT_SECRET:mySecretKeyForCredexaApplicationMustBe256BitsLongForHS256AlgorithmToWorkProperly}
  expiration: ${JWT_EXPIRATION:3600000}
```

---

## Database Migration

### Required Actions

**⚠️ IMPORTANT:** Before starting the updated services, the database must be migrated.

#### Option 1: Fresh Start (Development)
```sql
-- Drop existing databases
DROP DATABASE IF EXISTS login_db;
DROP DATABASE IF EXISTS customer_db;

-- Restart services - they will auto-create tables with new roles
```

#### Option 2: Manual Migration (Production)
```sql
-- Login Service Database
USE login_db;

-- Remove old roles
DELETE FROM user_roles WHERE role_id IN (
    SELECT id FROM roles WHERE name IN ('ROLE_PRODUCT_MANAGER', 'ROLE_FD_MANAGER', 'ROLE_REPORT_VIEWER')
);
DELETE FROM roles WHERE name IN ('ROLE_PRODUCT_MANAGER', 'ROLE_FD_MANAGER', 'ROLE_REPORT_VIEWER');

-- Rename ROLE_USER to ROLE_CUSTOMER
UPDATE roles SET name = 'ROLE_CUSTOMER' WHERE name = 'ROLE_USER';

-- Rename ROLE_CUSTOMER_MANAGER to ROLE_MANAGER
UPDATE roles SET name = 'ROLE_MANAGER' WHERE name = 'ROLE_CUSTOMER_MANAGER';

-- Verify
SELECT * FROM roles;
-- Should show: ROLE_ADMIN, ROLE_MANAGER, ROLE_CUSTOMER
```

---

## Testing Checklist

After migration, verify:

### Login Service
- [ ] User registration assigns ROLE_CUSTOMER
- [ ] Admin login works with ROLE_ADMIN
- [ ] JWT tokens contain correct roles
- [ ] DataInitializer creates only 3 roles

### Customer Service
- [ ] ROLE_CUSTOMER can create own profile
- [ ] ROLE_CUSTOMER cannot access /all endpoint
- [ ] ROLE_MANAGER can view all customers
- [ ] ROLE_MANAGER can access 360° view
- [ ] ROLE_ADMIN can update any profile

### Cross-Service
- [ ] JWT tokens from Login Service work in Customer Service
- [ ] Role checks function correctly
- [ ] No errors related to old role names

---

## Environment Variables (Production)

Set these environment variables in production:

```bash
# Database
export DB_USERNAME=credexa_user
export DB_PASSWORD=SecurePassword123!

# JWT
export JWT_SECRET=$(openssl rand -base64 64)
export JWT_EXPIRATION=3600000

# Admin
export ADMIN_CREATE_DEFAULT=false  # Disable auto-creation
# OR
export ADMIN_USERNAME=secureadmin
export ADMIN_PASSWORD=YourSecureAdminPassword123!
export ADMIN_EMAIL=admin@yourbank.com
```

---

## Breaking Changes

### For Existing Users

**Impact:** Users with old roles will need role reassignment

**Actions Required:**
1. All users with `ROLE_USER` → automatically become `ROLE_CUSTOMER`
2. All users with `ROLE_CUSTOMER_MANAGER`, `ROLE_PRODUCT_MANAGER`, `ROLE_FD_MANAGER`, or `ROLE_REPORT_VIEWER` → should be assigned `ROLE_MANAGER`
3. Users with `ROLE_ADMIN` → no change

### For Other Services (TODO)

The following services still need to be updated:
- [ ] Product & Pricing Service
- [ ] Calculator Service
- [ ] FD Account Service
- [ ] Gateway Service

**Note:** All services must be updated before deployment to avoid authentication errors.

---

## Rollback Plan

If issues arise, rollback procedure:

1. **Restore code:**
   ```bash
   git revert <commit-hash>
   ```

2. **Restore database:**
   ```sql
   -- Restore from backup
   mysql -u root -p login_db < backup_before_migration.sql
   mysql -u root -p customer_db < backup_before_migration.sql
   ```

3. **Restart services**

---

## Benefits of New Role System

### 1. Simplified Administration
- **Before:** 6 roles to manage
- **After:** 3 roles (50% reduction)

### 2. Clearer Permissions
- **Before:** Overlapping permissions between CUSTOMER_MANAGER, PRODUCT_MANAGER, FD_MANAGER
- **After:** Clear separation: CUSTOMER, MANAGER, ADMIN

### 3. Easier Onboarding
- New developers understand the role hierarchy immediately
- Less documentation needed

### 4. Better Security
- Principle of least privilege easier to enforce
- Fewer role combinations to test

### 5. Maintainability
- Single MANAGER role instead of 4 manager-type roles
- Easier to add new features

---

## Migration Status

| Service | Role Migration | Config Externalization | Status |
|---------|----------------|------------------------|--------|
| Login Service | ✅ Complete | ✅ Complete | Ready for testing |
| Customer Service | ✅ Complete | ✅ Complete | Ready for testing |
| Product & Pricing Service | ⏳ Pending | ⏳ Pending | Not started |
| Calculator Service | ⏳ Pending | ⏳ Pending | Not started |
| FD Account Service | ⏳ Pending | ⏳ Pending | Not started |
| Gateway Service | ⏳ Pending | ⏳ Pending | Not started |

---

## Next Steps

1. ✅ Update Login Service roles
2. ✅ Update Customer Service roles
3. ⏳ Test Login + Customer services together
4. ⏳ Update Product & Pricing Service
5. ⏳ Update Calculator Service
6. ⏳ Update FD Account Service
7. ⏳ Update Gateway Service
8. ⏳ Comprehensive integration testing
9. ⏳ Update all documentation
10. ⏳ Deploy to production

---

**Document Version:** 1.0
**Last Updated:** 2025-11-07
**Status:** Login + Customer Services Complete, Others Pending
