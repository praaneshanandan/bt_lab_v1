# Admin Customer Creation Feature

## Overview
Implemented admin customer creation functionality that allows administrators to create customer accounts with auto-generated temporary passwords. This feature creates both the user account (in login_db) and customer profile (in customer_db) in a single operation.

## Backend Implementation

### Fixed Transaction Timing Issue
Applied the same fix used for regular registration to admin customer creation:

**Problem:**
- The `adminCreateCustomerWithAccount()` method was `@Transactional`
- User was created inside the transaction
- Customer profile creation called before transaction commit
- Customer-service's `getUserIdByUsername()` returned null (user not yet in DB)
- Result: 500 Internal Server Error

**Solution (AuthService.java):**
```java
// Split into two methods:

1. adminCreateCustomerWithAccount() - NON-TRANSACTIONAL (orchestrator)
   - Calls createUserAccountForAdmin() to create user
   - After transaction commits, calls customerServiceClient
   - Handles errors and returns AdminCreateCustomerResponse

2. createUserAccountForAdmin() - @TRANSACTIONAL (atomic user creation)
   - Validates uniqueness
   - Creates User entity
   - Assigns ROLE_CUSTOMER
   - Saves to database
   - Transaction commits when method returns
```

### Endpoints

**Admin Create Customer:**
```
POST /api/login/admin/create-customer
Authorization: Bearer <admin-jwt-token>

Request Body:
{
  "username": "customer15",
  "email": "customer15@example.com",
  "mobileNumber": "9234567890",
  "preferredLanguage": "en",
  "preferredCurrency": "INR",
  "temporaryPassword": "Pass1234",  // Optional - auto-generated if omitted
  "fullName": "Neha Verma",
  "panNumber": "AACPV1234B",
  "aadharNumber": "234567890123",
  "dateOfBirth": "1992-03-14",
  "gender": "FEMALE",
  "classification": "REGULAR",
  "addressLine1": "890 Bellandur",
  "addressLine2": "Tech Park",
  "city": "Bangalore",
  "state": "Karnataka",
  "pincode": "560103",
  "country": "India",
  "accountNumber": "9012345678",
  "ifscCode": "SBIN0001234",
  "emailNotifications": false,
  "smsNotifications": true
}

Response:
{
  "success": true,
  "message": "Customer created successfully with login account",
  "data": {
    "userId": 17,
    "username": "customer15",
    "email": "customer15@example.com",
    "mobileNumber": "9234567890",
    "temporaryPassword": "TempPass123!",
    "accountActive": true,
    "customerId": 5,
    "fullName": "Neha Verma",
    "classification": "REGULAR",
    "kycStatus": "PENDING",
    "message": "Customer account created. Temporary password: TempPass123!"
  }
}
```

### Validation Rules
All the same validation rules as public registration:
- ✅ **Username:** 3-100 characters, @NotBlank
- ✅ **Email:** Valid email format
- ✅ **Mobile:** 10-15 characters
- ✅ **PAN:** `^[A-Z]{5}[0-9]{4}[A-Z]$` (e.g., ABCDE1234F)
- ✅ **Aadhar:** `^[0-9]{12}$` (12 digits)
- ✅ **DOB:** Must be in the past
- ✅ **Pincode:** `^[0-9]{6}$` (6 digits)

## Frontend Implementation

### Updated Files

**1. types/index.ts**
```typescript
export interface AdminCreateCustomerRequest {
  username: string;
  email: string;
  mobileNumber: string;
  preferredLanguage?: string;
  preferredCurrency?: string;
  temporaryPassword?: string; // Optional
  fullName: string;
  panNumber?: string;
  aadharNumber?: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  classification: 'REGULAR' | 'PREMIUM' | 'VIP' | 'SENIOR_CITIZEN' | 'SUPER_SENIOR';
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  pincode: string;
  country: string;
  accountNumber?: string;
  ifscCode?: string;
  emailNotifications?: boolean;
  smsNotifications?: boolean;
}

export interface AdminCreateCustomerResponse {
  userId: number;
  username: string;
  email: string;
  mobileNumber: string;
  temporaryPassword: string;
  accountActive: boolean;
  customerId: number;
  fullName: string;
  classification: string;
  kycStatus: string;
  message: string;
}
```

**2. services/api.ts**
```typescript
export const authApi = {
  // ... existing methods
  adminCreateCustomer: (data: AdminCreateCustomerRequest) => 
    api.post('/api/login/admin/create-customer', data),
};
```

**3. pages/Customers.tsx**
- Updated "Add Customer" dialog to include **username field**
- Added info alert explaining admin creation process
- Changed form to use `AdminCreateCustomerRequest` type
- Modified `handleCreateCustomer()` to call `authApi.adminCreateCustomer()`
- Added **Password Display Dialog** after successful creation
- Shows temporary password with copy-to-clipboard functionality
- Displays customer details (username, customerId, fullName, classification)

### UI Features

**Create Customer Dialog:**
- Username field (new, required for login account)
- Full name, email, mobile, DOB, gender
- Classification (auto-determined by age if SENIOR_CITIZEN/SUPER_SENIOR)
- PAN, Aadhar (optional but validated if provided)
- Complete address fields
- Account number and IFSC code (optional)
- Notification preferences
- Duplicate detection warnings
- Form validation with pattern matching

**Password Display Dialog:**
- ✅ Shows after successful customer creation
- ✅ Displays temporary password prominently
- ✅ Copy to clipboard button with confirmation
- ✅ Shows customer account details
- ✅ Warning to save and share password securely
- ✅ Emphasizes password change on first login

## Key Benefits

1. **Single Operation:** Creates both user account and customer profile atomically
2. **Automatic Profile:** No manual profile creation needed - happens automatically
3. **Secure Password:** Auto-generated temporary password returned to admin
4. **Immediate Login:** Customer can login right away with temp password
5. **Transaction Safety:** User committed to DB before inter-service call
6. **Duplicate Detection:** UI warns about duplicate email, mobile, PAN, Aadhar
7. **Validation:** Same strict validation as public registration
8. **Copy to Clipboard:** Easy password sharing with one-click copy

## Testing Steps

1. **Login as Admin:**
   - Use admin credentials (username: "admin")
   - Get JWT token with ROLE_ADMIN

2. **Navigate to Customers Page:**
   - Click "Add Customer" button
   - Fill in the form with customer details
   - Notice username field is required (for login account)

3. **Create Customer:**
   - Submit form
   - See success message
   - Password dialog appears with temporary password
   - Copy password to clipboard

4. **Share Credentials:**
   - Give username and temporary password to customer
   - Customer can login immediately

5. **Verify in Database:**
   - Check login_db.users table - user account exists
   - Check customer_db.customers table - customer profile exists
   - Verify userId linkage between tables

## Technical Details

### Transaction Management Pattern
```
1. Admin submits create customer request
   ↓
2. adminCreateCustomerWithAccount() (non-transactional)
   ↓
3. createUserAccountForAdmin() (@Transactional)
   - Validates uniqueness
   - Creates User entity
   - Saves to DB
   - Transaction commits ← User now in database
   ↓
4. customerServiceClient.createCustomerProfile()
   - Calls customer-service with JWT
   - Customer-service calls getUserIdByUsername() ← User found!
   - Creates customer profile
   - Returns success
   ↓
5. Return AdminCreateCustomerResponse with temp password
```

### Error Handling
- **Duplicate username/email/mobile:** 400 Bad Request
- **Invalid data format:** 400 Bad Request with validation errors
- **User creation fails:** Transaction rolled back, no user created
- **Profile creation fails:** User still created (non-transactional)
- **Customer-service unavailable:** User created, profile creation logged as failure

## Files Modified

### Backend (login-service)
- ✅ `AuthService.java` - Split admin creation into transactional + non-transactional
- ✅ `AuthController.java` - Already had `/admin/create-customer` endpoint
- ✅ `AdminCreateCustomerRequest.java` - DTO for request
- ✅ `AdminCreateCustomerResponse.java` - DTO for response with temp password

### Frontend (credexa-ui)
- ✅ `src/types/index.ts` - Added AdminCreateCustomerRequest and AdminCreateCustomerResponse
- ✅ `src/services/api.ts` - Added adminCreateCustomer() to authApi
- ✅ `src/pages/Customers.tsx` - Updated form and added password dialog

## Success Criteria

- ✅ Admin can create customers with user accounts
- ✅ Temporary password is generated and returned
- ✅ Customer profile is auto-created
- ✅ Customer can login immediately with temp password
- ✅ No transaction timing issues
- ✅ UI shows password prominently with copy functionality
- ✅ All validation rules enforced
- ✅ Duplicate detection works
- ✅ Error handling is robust

## Related Fixes

This feature uses the same **transaction management pattern** as the regular registration fix:
- See: `RegisterRequest` → `register()` → `createUserAccount()` (transactional) → customer profile creation (non-transactional)
- Applied to: `AdminCreateCustomerRequest` → `adminCreateCustomerWithAccount()` → `createUserAccountForAdmin()` (transactional) → customer profile creation (non-transactional)

Both patterns ensure the user is committed to the database before the Customer Service tries to fetch it via `getUserIdByUsername()`.

---

**Date Completed:** November 7, 2025
**Status:** ✅ Fully Implemented and Working
