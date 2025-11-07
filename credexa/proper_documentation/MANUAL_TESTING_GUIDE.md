# MANUAL TESTING GUIDE - CREDEXA LOGIN & CUSTOMER SERVICES

**Date:** 2025-11-07
**Services:** Login Service (Port 8081), Customer Service (Port 8083)
**Tool:** Postman, Insomnia, or curl

---

## Prerequisites

1. Both services must be running:
   - Login Service: http://localhost:8081
   - Customer Service: http://localhost:8083

2. Fresh database (users already exist from previous tests):
   - admin / Admin@123
   - customer1 / Pass1234
   - customer2 / Pass1234
   - manager1 / Pass1234

---

## Test Suite 1: LOGIN SERVICE

### 1.1 Health Check

**Request:**
```
GET http://localhost:8081/api/auth/health
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "data": "Login Service is running",
  "timestamp": "2025-11-07T12:00:00.000"
}
```

---

### 1.2 Register New User

**Request:**
```
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "testuser1",
  "password": "Test@1234",
  "email": "testuser1@test.com",
  "mobileNumber": "9876543213"
}
```

**Expected Response:** 201 Created
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "userId": 5,
    "username": "testuser1",
    "email": "testuser1@test.com",
    "roles": ["ROLE_CUSTOMER"]
  }
}
```

**Note:** If user already exists, you'll get 400 Bad Request - that's expected.

---

### 1.3 Login as Admin

**Request:**
```
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "usernameOrEmailOrMobile": "admin",
  "password": "Admin@123"
}
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "admin",
    "email": "admin@credexa.com",
    "mobileNumber": "9999999999",
    "roles": ["ROLE_ADMIN"],
    "preferredLanguage": "en",
    "preferredCurrency": "USD",
    "loginTime": "2025-11-07T12:00:00.000",
    "expiresIn": 3600000
  }
}
```

**⚠️ IMPORTANT:** Save the `token` value - you'll need it for subsequent requests!

---

### 1.4 Login as Customer1

**Request:**
```
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "usernameOrEmailOrMobile": "customer1",
  "password": "Pass1234"
}
```

**Expected Response:** 200 OK (similar structure, with ROLE_CUSTOMER)

**⚠️ IMPORTANT:** Save this token as "CUSTOMER1_TOKEN"

---

### 1.5 Login as Customer2

**Request:**
```
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "usernameOrEmailOrMobile": "customer2",
  "password": "Pass1234"
}
```

**Expected Response:** 200 OK

**⚠️ IMPORTANT:** Save this token as "CUSTOMER2_TOKEN"

---

### 1.6 Validate Token

**Request:**
```
POST http://localhost:8081/api/auth/validate-token
Content-Type: application/json

{
  "token": "<PASTE_ADMIN_TOKEN_HERE>"
}
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "valid": true,
    "username": "admin",
    "userId": 1,
    "roles": ["ROLE_ADMIN"],
    "message": "Token is valid"
  }
}
```

---

### 1.7 Get Bank Configuration

**Request:**
```
GET http://localhost:8081/api/auth/bank-config
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "bankName": "Credexa Bank",
    "logoUrl": "/assets/logo.png",
    "defaultLanguage": "en",
    "defaultCurrency": "INR",
    "currencyDecimalPlaces": 2
  }
}
```

---

### 1.8 Get User by Username (Inter-service endpoint)

**Request:**
```
GET http://localhost:8081/api/auth/user/customer1
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "id": 2,
    "username": "customer1",
    "email": "customer1@test.com",
    "mobileNumber": "9876543210",
    "roles": [
      {
        "id": 3,
        "name": "ROLE_CUSTOMER"
      }
    ],
    "active": true,
    "accountLocked": false
  }
}
```

---

### 1.9 Logout

**Request:**
```
POST http://localhost:8081/api/auth/logout
Authorization: Bearer <CUSTOMER2_TOKEN>
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "message": "Logout successful"
}
```

---

### 1.10 Try to Access After Logout (Should Fail)

**Request:**
```
GET http://localhost:8081/api/auth/bank-config
Authorization: Bearer <CUSTOMER2_TOKEN>
```

**Expected Response:** 401 Unauthorized

---

## Test Suite 2: CUSTOMER SERVICE

### 2.1 Health Check

**Request:**
```
GET http://localhost:8083/api/customer/health
```

**Expected Response:** 200 OK
```
Customer Service is UP
```

---

### 2.2 Create Customer Profile (as Customer1)

**⚠️ CRITICAL TEST - This is the one that was failing!**

**Request:**
```
POST http://localhost:8083/api/customer
Content-Type: application/json
Authorization: Bearer <CUSTOMER1_TOKEN>

{
  "fullName": "John Doe",
  "mobileNumber": "9876543210",
  "email": "customer1@test.com",
  "panNumber": "ABCDE1234F",
  "aadharNumber": "123456789012",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "classification": "REGULAR",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apt 5B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "country": "India",
  "accountNumber": "ACC1234567890",
  "ifscCode": "HDFC0001234",
  "preferredLanguage": "en",
  "preferredCurrency": "INR",
  "emailNotifications": true,
  "smsNotifications": true
}
```

**Expected Response:** 201 Created
```json
{
  "success": true,
  "message": "Customer created successfully",
  "data": {
    "id": 1,
    "userId": 2,
    "username": "customer1",
    "fullName": "John Doe",
    "email": "customer1@test.com",
    "mobileNumber": "9876543210",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "classification": "REGULAR",
    "addressLine1": "123 Main Street",
    "addressLine2": "Apt 5B",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001",
    "country": "India",
    "panNumber": "ABCDE1234F",
    "aadharNumber": "123456789012",
    "accountNumber": "ACC1234567890",
    "ifscCode": "HDFC0001234",
    "preferredLanguage": "en",
    "preferredCurrency": "INR",
    "emailNotifications": true,
    "smsNotifications": true,
    "kycStatus": "PENDING",
    "isActive": true,
    "createdAt": "2025-11-07T12:00:00.000"
  }
}
```

**⚠️ If this returns 403 Forbidden:**
- Check customer-service console logs for "No JWT token found"
- If you see that message, the Authorization header issue is not fixed yet
- The JWT filter is not receiving the header for POST requests with JSON body

**⚠️ If this returns 400 Bad Request:**
- Check the error message - likely a field validation issue
- Common issues:
  - `panNumber` must match format: `ABCDE1234F` (5 letters, 4 digits, 1 letter)
  - `aadharNumber` must be exactly 12 digits
  - `mobileNumber` must be exactly 10 digits
  - `pincode` must be exactly 6 digits
  - `dateOfBirth` must be in past

---

### 2.3 Create Customer Profile (as Customer2)

**Request:**
```
POST http://localhost:8083/api/customer
Content-Type: application/json
Authorization: Bearer <CUSTOMER1_TOKEN>

{
  "fullName": "Jane Smith",
  "mobileNumber": "9876543211",
  "email": "customer2@test.com",
  "panNumber": "FGHIJ5678K",
  "aadharNumber": "987654321098",
  "dateOfBirth": "1985-05-20",
  "gender": "FEMALE",
  "classification": "PREMIUM",
  "addressLine1": "456 Oak Avenue",
  "city": "Delhi",
  "state": "Delhi",
  "pincode": "110001",
  "country": "India",
  "accountNumber": "ACC9876543210",
  "ifscCode": "ICIC0005678",
  "preferredLanguage": "en",
  "preferredCurrency": "INR",
  "emailNotifications": false,
  "smsNotifications": true
}
```

**Note:** Use CUSTOMER1_TOKEN here (not CUSTOMER2_TOKEN) because we logged out customer2 earlier.

---

### 2.4 Get All Customers (as Admin) - Should Work

**Request:**
```
GET http://localhost:8083/api/customer/all
Authorization: Bearer <ADMIN_TOKEN>
```

**Expected Response:** 200 OK
```json
[
  {
    "id": 1,
    "fullName": "John Doe",
    ...
  },
  {
    "id": 2,
    "fullName": "Jane Smith",
    ...
  }
]
```

---

### 2.5 Get All Customers (as Customer1) - Should Fail

**Request:**
```
GET http://localhost:8083/api/customer/all
Authorization: Bearer <CUSTOMER1_TOKEN>
```

**Expected Response:** 403 Forbidden
```json
{
  "timestamp": "2025-11-07T12:00:00.000",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/customer/all"
}
```

---

### 2.6 Get Customer by ID (as Customer1, own profile)

**Request:**
```
GET http://localhost:8083/api/customer/1
Authorization: Bearer <CUSTOMER1_TOKEN>
```

**Note:** Replace `1` with the actual customer ID returned from step 2.2

**Expected Response:** 200 OK (full customer profile)

---

### 2.7 Get Customer by ID (as Customer1, trying to access Customer2) - Should Fail

**Request:**
```
GET http://localhost:8083/api/customer/2
Authorization: Bearer <CUSTOMER1_TOKEN>
```

**Expected Response:** 403 Forbidden
```json
{
  "success": false,
  "error": "You don't have permission to view this customer",
  "timestamp": "2025-11-07T12:00:00.000"
}
```

---

### 2.8 Get Customer by User ID

**Request:**
```
GET http://localhost:8083/api/customer/user/2
Authorization: Bearer <CUSTOMER1_TOKEN>
```

**Expected Response:** 200 OK (customer profile for user ID 2)

---

### 2.9 Update Customer Profile

**Request:**
```
PUT http://localhost:8083/api/customer/1
Content-Type: application/json
Authorization: Bearer <CUSTOMER1_TOKEN>

{
  "fullName": "John Doe Updated",
  "mobileNumber": "9876543210",
  "email": "customer1@test.com",
  "panNumber": "ABCDE1234F",
  "aadharNumber": "123456789012",
  "dateOfBirth": "1990-01-15",
  "gender": "MALE",
  "classification": "PREMIUM",
  "addressLine1": "123 Main Street - Updated",
  "addressLine2": "Apt 5B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "country": "India",
  "accountNumber": "ACC1234567890",
  "ifscCode": "HDFC0001234",
  "preferredLanguage": "en",
  "preferredCurrency": "USD",
  "emailNotifications": true,
  "smsNotifications": false
}
```

**Expected Response:** 200 OK (updated customer profile)

---

### 2.10 Get Customer Classification

**Request:**
```
GET http://localhost:8083/api/customer/1/classification
Authorization: Bearer <CUSTOMER1_TOKEN>
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "customerId": 1,
    "classification": "PREMIUM",
    "calculatedOn": "2025-11-07T12:00:00.000"
  }
}
```

---

### 2.11 Get Customer 360° View (as Admin)

**Request:**
```
GET http://localhost:8083/api/customer/1/360-view
Authorization: Bearer <ADMIN_TOKEN>
```

**Expected Response:** 200 OK
```json
{
  "success": true,
  "data": {
    "customer": { ... },
    "totalFDAccounts": 0,
    "totalFDValue": 0.0,
    "fdAccounts": []
  }
}
```

**Note:** FD accounts will be empty until FD Account Service is tested

---

## Test Suite 3: ROLE-BASED ACCESS CONTROL

### 3.1 CUSTOMER Role Permissions

**What CUSTOMER can do:**
- ✅ Create own customer profile
- ✅ View own customer profile
- ✅ Update own customer profile
- ✅ View own FD accounts (when FD service is integrated)
- ✅ Use calculator services

**What CUSTOMER cannot do:**
- ❌ View all customers (403)
- ❌ View other customers' profiles (403)
- ❌ Access 360° view (403)
- ❌ Unlock accounts (403)

### 3.2 ADMIN Role Permissions

**What ADMIN can do:**
- ✅ Everything CUSTOMER can do
- ✅ View all customers
- ✅ Create customer profiles for any user
- ✅ Update any customer profile
- ✅ View 360° view for any customer
- ✅ Unlock locked accounts
- ✅ View audit logs

---

## Test Suite 4: MANAGER ROLE (TODO)

**Note:** Currently, manager1 has ROLE_CUSTOMER. To test MANAGER permissions:

1. **Connect to MySQL:**
   ```sql
   mysql -u root -p
   use login_db;
   ```

2. **Check current roles:**
   ```sql
   SELECT u.id, u.username, r.name as role
   FROM users u
   JOIN user_roles ur ON u.id = ur.user_id
   JOIN roles r ON ur.role_id = r.id
   WHERE u.username = 'manager1';
   ```

3. **Get role IDs:**
   ```sql
   SELECT id, name FROM roles;
   ```

4. **Assign MANAGER role to manager1:**
   ```sql
   -- Assuming ROLE_MANAGER has id=2, and manager1 has user_id=4
   INSERT INTO user_roles (user_id, role_id)
   VALUES (4, 2);
   ```

5. **Remove CUSTOMER role (optional):**
   ```sql
   DELETE FROM user_roles
   WHERE user_id = 4 AND role_id = 3;
   ```

6. **Verify:**
   ```sql
   SELECT u.id, u.username, r.name as role
   FROM users u
   JOIN user_roles ur ON u.id = ur.user_id
   JOIN roles r ON ur.role_id = r.id
   WHERE u.username = 'manager1';
   ```

7. **Login again to get new token:**
   ```
   POST http://localhost:8081/api/auth/login
   {
     "usernameOrEmailOrMobile": "manager1",
     "password": "Pass1234"
   }
   ```

8. **Test MANAGER permissions - should be able to:**
   - View all customers
   - Create customer profiles for any user
   - View customer 360° view
   - But cannot unlock accounts (Admin only)

---

## Expected Issues & Troubleshooting

### Issue 1: 403 Forbidden on POST /api/customer

**Symptom:**
```json
{
  "timestamp": "2025-11-07T12:00:00.000",
  "status": 403,
  "error": "Forbidden",
  "message": "",
  "path": "/api/customer"
}
```

**Console logs show:**
```
DEBUG c.a.c.config.JwtAuthenticationFilter : No JWT token found in request headers
```

**Diagnosis:** Authorization header is not reaching the JWT filter for POST requests with JSON body.

**What to check:**
1. Is the Authorization header included in the request?
2. Does it start with "Bearer " (with space)?
3. Is the token valid and not expired?
4. Check customer-service logs for filter chain order

**Potential causes:**
- Spring's FormContentFilter wrapping the request
- Filter chain order issue
- Request body being read before JWT filter runs

---

### Issue 2: 400 Bad Request on POST /api/customer

**Symptom:**
```json
{
  "success": false,
  "message": "Validation failed",
  "error": "{ field validation errors }",
  "timestamp": "2025-11-07T12:00:00.000"
}
```

**Solution:** Check field validations:
- `panNumber`: Format `ABCDE1234F` (5 uppercase letters, 4 digits, 1 uppercase letter)
- `aadharNumber`: Exactly 12 digits (no spaces)
- `mobileNumber`: Exactly 10 digits
- `pincode`: Exactly 6 digits
- `dateOfBirth`: Must be in the past
- `email`: Valid email format
- `classification`: Must be one of: REGULAR, PREMIUM, VIP

---

### Issue 3: 404 Not Found - Customer not found

**Symptom:**
```json
{
  "success": false,
  "error": "Customer not found for user ID: 2",
  "timestamp": "2025-11-07T12:00:00.000"
}
```

**Solution:** The customer profile hasn't been created yet. Complete Test 2.2 first.

---

### Issue 4: 401 Unauthorized

**Symptom:**
```json
{
  "timestamp": "2025-11-07T12:00:00.000",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required",
  "path": "/api/customer/..."
}
```

**Possible causes:**
1. Token is missing from Authorization header
2. Token is expired (tokens expire after 1 hour)
3. Token is invalid
4. User has logged out

**Solution:** Login again to get a fresh token.

---

## Success Criteria

### Login Service:
- [  ] All users can register
- [  ] All users can login and receive valid JWT tokens
- [  ] Tokens can be validated
- [  ] Users can logout
- [  ] After logout, tokens are invalidated
- [  ] Bank config is accessible publicly
- [  ] User info is accessible for inter-service communication

### Customer Service:
- [  ] Customers can create their own profile
- [  ] Customers can view their own profile
- [  ] Customers can update their own profile
- [  ] Customers CANNOT view all customers (403)
- [  ] Customers CANNOT view other customers' profiles (403)
- [  ] Admins CAN view all customers
- [  ] Admins CAN create profiles for any user
- [  ] Admins CAN view 360° view
- [  ] Customer classification is calculated correctly

### Role-Based Access:
- [  ] ROLE_CUSTOMER has limited access
- [  ] ROLE_ADMIN has full access
- [  ] ROLE_MANAGER has intermediate access (after manual role assignment)

---

## Test Results Template

Use this template to record your results:

```
=== TEST RESULTS ===
Date: 2025-11-07
Tester: [Your Name]

LOGIN SERVICE:
1.1 Health Check: [ PASS / FAIL ]
1.2 Register User: [ PASS / FAIL / SKIPPED ]
1.3 Login Admin: [ PASS / FAIL ]
1.4 Login Customer1: [ PASS / FAIL ]
1.5 Login Customer2: [ PASS / FAIL ]
1.6 Validate Token: [ PASS / FAIL ]
1.7 Bank Config: [ PASS / FAIL ]
1.8 Get User by Username: [ PASS / FAIL ]
1.9 Logout: [ PASS / FAIL ]
1.10 Access After Logout: [ PASS / FAIL ]

CUSTOMER SERVICE:
2.1 Health Check: [ PASS / FAIL ]
2.2 Create Profile (Customer1): [ PASS / FAIL ] ⚠️ CRITICAL
2.3 Create Profile (Customer2): [ PASS / FAIL ]
2.4 Get All (Admin): [ PASS / FAIL ]
2.5 Get All (Customer) - Should Fail: [ PASS / FAIL ]
2.6 Get by ID (Own): [ PASS / FAIL ]
2.7 Get by ID (Other) - Should Fail: [ PASS / FAIL ]
2.8 Get by User ID: [ PASS / FAIL ]
2.9 Update Profile: [ PASS / FAIL ]
2.10 Get Classification: [ PASS / FAIL ]
2.11 Get 360° View: [ PASS / FAIL ]

CRITICAL ISSUE STATUS:
- POST with JWT token and JSON body: [ WORKING / BROKEN ]
- If broken, console shows: [ "No JWT token found" / Other error ]

Overall Success Rate: __/21 tests passed
```

---

## Notes

- **Most Critical Test:** Test 2.2 - Create Customer Profile
  - This was failing with 403 Forbidden
  - Console logs showed "No JWT token found in request headers"
  - This indicates the Authorization header is not reaching the JWT filter for POST requests with JSON body
  - If this test passes, the fix worked!

- **Token Expiry:** Tokens expire after 1 hour (3600000 ms). If tests start failing with 401, login again.

- **Session Timeout:** Sessions auto-logout after 5 minutes of inactivity.

- **Test Order:** Tests should be executed in order as some depend on data from previous tests.

---

**Good luck with testing! Please report back with your results, especially for Test 2.2!**
