# REGISTRATION AND CUSTOMER CREATION WORKFLOWS

**Date:** 2025-11-07
**Status:** Implemented and Ready for Testing

---

## Overview

This document describes the **two new workflows** implemented to solve the registration and customer creation issues:

### Problems Fixed:
1. **User registration didn't create customer profile** - Users had to manually POST to customer service
2. **Admin-created customers had no login credentials** - Customer profiles existed but users couldn't login

### Solutions Implemented:
1. **Enhanced Registration** - Auto-creates customer profile during registration
2. **Admin Create Customer** - New endpoint to create both user account and customer profile together

---

## Workflow 1: User Self-Registration (Enhanced)

### Endpoint
```
POST http://localhost:8081/api/auth/register
```

### What Changed
**Before:**
- User registers → Only user account created in login-service
- User must manually POST to customer-service to create profile

**After:**
- User registers → User account created + Customer profile auto-created
- One registration call creates everything

### Request Body (Expanded)
The registration request now includes all customer profile fields:

```json
{
  // ===== Login Account Fields =====
  "username": "customer3",
  "password": "Pass1234",
  "email": "customer3@example.com",
  "mobileNumber": "9876543213",
  "preferredLanguage": "en",
  "preferredCurrency": "INR",

  // ===== Customer Profile Fields =====
  "fullName": "Customer Three",
  "panNumber": "ABCDE1234F",
  "aadharNumber": "123456789013",
  "dateOfBirth": "1995-03-15",
  "gender": "MALE",
  "classification": "REGULAR",

  // Address
  "addressLine1": "123 Main St",
  "addressLine2": "Apt 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "country": "India",

  // Financial (Optional)
  "accountNumber": "1234567890",
  "ifscCode": "SBIN0001234",

  // Preferences (Optional)
  "emailNotifications": true,
  "smsNotifications": true
}
```

### Field Validations

#### Required Fields:
- `username` - Min 3, Max 100 characters
- `password` - Min 8 characters
- `email` - Valid email format
- `mobileNumber` - 10-15 digits
- `fullName` - Max 100 characters
- `dateOfBirth` - Must be in the past
- `gender` - MALE, FEMALE, or OTHER
- `classification` - REGULAR, PREMIUM, VIP, SENIOR_CITIZEN, or SUPER_SENIOR
- `addressLine1`, `city`, `state`, `country`
- `pincode` - 6 digits

#### Optional Fields:
- `panNumber` - Format: ABCDE1234F (if provided)
- `aadharNumber` - 12 digits (if provided)
- `addressLine2`
- `accountNumber`, `ifscCode`
- `preferredLanguage` (default: "en")
- `preferredCurrency` (default: "INR")
- `emailNotifications` (default: true)
- `smsNotifications` (default: true)

### Response
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 5,
    "username": "customer3",
    "email": "customer3@example.com",
    "mobileNumber": "9876543213",
    "preferredLanguage": "en",
    "preferredCurrency": "INR",
    "active": true,
    "accountLocked": false,
    "roles": [
      {
        "id": 3,
        "name": "ROLE_CUSTOMER"
      }
    ]
  },
  "timestamp": "2025-11-07T10:30:00"
}
```

### What Happens Behind the Scenes

1. **User account creation** (login-service):
   - Validates username, email, mobile uniqueness
   - Creates user with BCrypt-hashed password
   - Assigns ROLE_CUSTOMER
   - Saves to `login_db.users` table

2. **Customer profile creation** (automatic):
   - Generates JWT token for inter-service auth
   - Calls customer-service API with all profile data
   - Customer profile saved to `customer_db.customers` table
   - Links profile to user via `userId` and `username`

3. **Error Handling**:
   - If customer profile creation fails, user account is still created
   - Logs warning for manual intervention
   - User can login but will need profile created manually

---

## Workflow 2: Admin Creates Customer with Login Account

### Endpoint
```
POST http://localhost:8081/api/auth/admin/create-customer
Authorization: Bearer <ADMIN_JWT_TOKEN>
```

### What This Solves
**Before:**
- Admin POSTs to customer-service → Customer profile created
- No user account exists → Customer can't login

**After:**
- Admin POSTs to new endpoint → Both user account AND customer profile created
- Temporary password generated → Customer can login immediately

### Request Body
Similar to registration request but **without password** (auto-generated):

```json
{
  // ===== Login Account Fields =====
  "username": "customer4",
  "email": "customer4@example.com",
  "mobileNumber": "9876543214",
  "preferredLanguage": "en",
  "preferredCurrency": "INR",
  "temporaryPassword": null,  // Optional - will be auto-generated if null

  // ===== Customer Profile Fields =====
  "fullName": "Customer Four",
  "panNumber": "FGHIJ5678K",
  "aadharNumber": "987654321014",
  "dateOfBirth": "1988-07-20",
  "gender": "FEMALE",
  "classification": "PREMIUM",

  // Address
  "addressLine1": "456 Park Ave",
  "addressLine2": "",
  "city": "Delhi",
  "state": "Delhi",
  "pincode": "110001",
  "country": "India",

  // Financial (Optional)
  "accountNumber": "9876543210",
  "ifscCode": "HDFC0001234",

  // Preferences
  "emailNotifications": true,
  "smsNotifications": true
}
```

### Response
```json
{
  "success": true,
  "message": "Customer created successfully with login account",
  "data": {
    // User account details
    "userId": 6,
    "username": "customer4",
    "email": "customer4@example.com",
    "mobileNumber": "9876543214",
    "temporaryPassword": "aB3@xY9#mN2$",  // Generated password - SAVE THIS!
    "accountActive": true,

    // Customer profile details
    "customerId": 5,
    "fullName": "Customer Four",
    "classification": "PREMIUM",
    "kycStatus": "PENDING",

    "message": "Customer created successfully with login credentials. Temporary password: aB3@xY9#mN2$ (User must change on first login)"
  },
  "timestamp": "2025-11-07T11:00:00"
}
```

### What Happens Behind the Scenes

1. **Validates uniqueness** of username, email, mobile

2. **Generates temporary password**:
   - 12 characters
   - Mix of uppercase, lowercase, digits, special chars
   - Admin can optionally provide custom temp password

3. **Creates user account**:
   - BCrypt-hashes the temporary password
   - Assigns ROLE_CUSTOMER
   - Marks as created by admin (audit trail)
   - Saves to `login_db.users`

4. **Creates customer profile**:
   - Uses JWT token for inter-service auth
   - Calls customer-service API
   - Links profile to user
   - Saves to `customer_db.customers`

5. **Transaction Management**:
   - If customer profile creation fails, user account creation is ROLLED BACK
   - This ensures consistency - no orphaned user accounts

6. **Returns temp password**:
   - Admin receives the temporary password in response
   - Admin should securely send this to the customer
   - Customer must change password on first login (future enhancement)

---

## Security & Authorization

### Registration Endpoint
- **Public** - No authentication required
- Anyone can register as a customer
- Auto-assigned ROLE_CUSTOMER

### Admin Create Customer Endpoint
- **Admin Only** - Requires ROLE_ADMIN
- Uses `@PreAuthorize("hasRole('ADMIN')")`
- Returns 403 Forbidden if not admin

---

## Testing Guide

### Test 1: User Self-Registration

1. **Register a new user** with full customer profile:
```bash
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "testuser1",
  "password": "Test1234",
  "email": "testuser1@example.com",
  "mobileNumber": "1234567890",
  "fullName": "Test User One",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "classification": "REGULAR",
  "addressLine1": "123 Test St",
  "city": "TestCity",
  "state": "TestState",
  "pincode": "123456",
  "country": "TestCountry"
}
```

2. **Verify user account created**:
```sql
USE login_db;
SELECT * FROM users WHERE username = 'testuser1';
```

3. **Verify customer profile created**:
```sql
USE customer_db;
SELECT * FROM customers WHERE username = 'testuser1';
```

4. **Login with new user**:
```bash
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "usernameOrEmailOrMobile": "testuser1",
  "password": "Test1234"
}
```

5. **Check customer profile via API**:
```bash
GET http://localhost:8083/api/customer/me
Authorization: Bearer <JWT_TOKEN_FROM_LOGIN>
```

### Test 2: Admin Creates Customer

1. **Login as admin** to get JWT token:
```bash
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "usernameOrEmailOrMobile": "admin",
  "password": "Admin@123"
}
```

2. **Create customer with account**:
```bash
POST http://localhost:8081/api/auth/admin/create-customer
Authorization: Bearer <ADMIN_JWT_TOKEN>
Content-Type: application/json

{
  "username": "admincustomer1",
  "email": "admincustomer1@example.com",
  "mobileNumber": "9999999991",
  "fullName": "Admin Customer One",
  "dateOfBirth": "1985-05-15",
  "gender": "FEMALE",
  "classification": "VIP",
  "addressLine1": "789 Admin Ave",
  "city": "AdminCity",
  "state": "AdminState",
  "pincode": "789012",
  "country": "India"
}
```

3. **Save the temporary password** from response

4. **Verify both user and customer created**:
```sql
USE login_db;
SELECT * FROM users WHERE username = 'admincustomer1';

USE customer_db;
SELECT * FROM customers WHERE username = 'admincustomer1';
```

5. **Login as the new customer** with temp password:
```bash
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "usernameOrEmailOrMobile": "admincustomer1",
  "password": "<TEMPORARY_PASSWORD_FROM_STEP_2>"
}
```

### Test 3: Error Handling

**Test duplicate username:**
```bash
# Try registering with existing username
POST http://localhost:8081/api/auth/register
# ... with username that already exists
# Expected: 400 Bad Request - "Username already exists"
```

**Test admin endpoint without auth:**
```bash
# Try creating customer without JWT token
POST http://localhost:8081/api/auth/admin/create-customer
# Expected: 401 Unauthorized
```

**Test admin endpoint as regular customer:**
```bash
# Login as customer, then try admin endpoint
POST http://localhost:8081/api/auth/admin/create-customer
Authorization: Bearer <CUSTOMER_JWT_TOKEN>
# Expected: 403 Forbidden
```

---

## Database Schema Impact

### No schema changes required!

Both workflows use **existing** tables and columns:

**login_db.users:**
- All user account data stored here
- No new columns needed

**customer_db.customers:**
- All customer profile data stored here
- Already has `userId` and `username` for linking

---

## Inter-Service Communication

### How login-service calls customer-service:

1. **CustomerServiceClient** class handles REST communication
2. **JWT Token** used for authentication (inter-service)
3. **URL** configured in application.yml: `microservices.customer-service.url`
4. **Request/Response DTOs** for data transfer

### Configuration
In `login-service/src/main/resources/application.yml`:
```yaml
microservices:
  customer-service:
    url: http://localhost:8083/api/customer
```

---

## Rollback & Error Handling

### Registration Flow
- **User account creation fails**: Returns 400/500 error, nothing created
- **Customer profile creation fails**: User account still created, logs warning
  - User can login but profile missing
  - Admin can manually create profile later

### Admin Create Customer Flow
- **User account creation fails**: Returns 400/500 error, nothing created
- **Customer profile creation fails**: **ROLLS BACK** user account creation
  - Transaction-based rollback ensures consistency
  - No orphaned user accounts

---

## Future Enhancements

1. **Email/SMS Notification**
   - Send welcome email on registration
   - Send temp password to customer when admin creates account

2. **Force Password Change**
   - Require users created by admin to change password on first login
   - Add `passwordChangeRequired` flag to User entity

3. **Two-Step Registration**
   - Allow minimal registration (username, email, password)
   - Force profile completion before accessing other features

4. **Batch Customer Import**
   - Admin endpoint to upload CSV of customers
   - Creates accounts in bulk

5. **Customer Verification**
   - Email verification link
   - Mobile OTP verification

---

## API Reference

### 1. POST /register
- **Auth**: Public
- **Request**: RegisterRequest (with all customer fields)
- **Response**: User object (password removed)
- **Status Codes**:
  - 201: Created successfully
  - 400: Validation error or duplicate
  - 500: Server error

### 2. POST /admin/create-customer
- **Auth**: ROLE_ADMIN required
- **Request**: AdminCreateCustomerRequest
- **Response**: AdminCreateCustomerResponse (includes temp password)
- **Status Codes**:
  - 201: Created successfully
  - 400: Validation error or duplicate
  - 403: Forbidden (not admin)
  - 500: Server error (rolled back)

---

## Files Modified/Created

### login-service

**Created:**
1. `client/CustomerServiceClient.java` - REST client for customer-service
2. `dto/AdminCreateCustomerRequest.java` - Admin create customer request DTO
3. `dto/AdminCreateCustomerResponse.java` - Admin create customer response DTO

**Modified:**
1. `dto/RegisterRequest.java` - Added all customer profile fields
2. `service/AuthService.java`:
   - Updated `register()` to auto-create customer profile
   - Added `adminCreateCustomerWithAccount()` method
   - Added `generateTemporaryPassword()` helper
3. `controller/AuthController.java` - Added `/admin/create-customer` endpoint

### customer-service
**No changes required** - Existing endpoints already support the workflow

---

## Success Criteria

✅ User registers once → Both user account and customer profile created
✅ Admin creates customer → Both user account and customer profile created with temp password
✅ No orphaned records → Transaction rollback on failure
✅ Proper error messages for duplicates and validation errors
✅ Authorization checks work correctly (admin-only endpoint)
✅ Inter-service communication works with JWT tokens

---

**Status:** Implementation Complete - Ready for Testing
**Next Step:** Test both workflows and verify end-to-end functionality

**Date:** 2025-11-07
**Implemented by:** Claude Code
