# CUSTOMER SERVICE - COMPREHENSIVE DOCUMENTATION

## Table of Contents
1. [Overview](#overview)
2. [Service Architecture](#service-architecture)
3. [API Endpoints](#api-endpoints)
4. [Security & Authentication](#security--authentication)
5. [Database Schema](#database-schema)
6. [Business Logic Flows](#business-logic-flows)
7. [Inter-Service Communication](#inter-service-communication)
8. [Configuration](#configuration)
9. [Issues & Bugs Found](#issues--bugs-found)
10. [Role Migration](#role-migration)
11. [Test Cases](#test-cases)
12. [Test Results](#test-results)

---

## Overview

**Service Name:** Customer Service
**Port:** 8083
**Context Path:** `/api/customer`
**Base URL:** `http://localhost:8083/api/customer`
**Database:** `customer_db` (MySQL)
**Primary Responsibility:** Customer Profile Management, Classification, 360° Customer View

### Purpose
The Customer Service manages customer profiles for the Credexa Fixed Deposit Banking application. It provides:
- Customer profile creation and management
- Customer classification for FD rate determination
- 360-degree customer view
- Integration with Login Service for user authentication
- KYC status tracking
- Communication preferences management

---

## Service Architecture

### Directory Structure
```
customer-service/
├── src/main/java/com/app/customer/
│   ├── CustomerServiceApplication.java        # Main entry point
│   ├── controller/
│   │   └── CustomerController.java            # REST API endpoints
│   ├── service/
│   │   └── CustomerService.java               # Business logic
│   ├── entity/
│   │   └── Customer.java                      # Customer entity
│   ├── repository/
│   │   └── CustomerRepository.java            # Data access layer
│   ├── dto/
│   │   ├── CreateCustomerRequest.java
│   │   ├── UpdateCustomerRequest.java
│   │   ├── CustomerResponse.java
│   │   ├── Customer360Response.java
│   │   ├── CustomerClassificationResponse.java
│   │   └── AuthenticatedUser.java
│   ├── client/
│   │   └── LoginServiceClient.java            # REST client to Login Service
│   ├── config/
│   │   ├── SecurityConfig.java                # Spring Security setup
│   │   ├── JwtAuthenticationFilter.java       # JWT filter
│   │   ├── RestClientConfig.java              # RestTemplate config
│   │   └── SwaggerConfig.java                 # API docs config
│   └── exception/
│       ├── CustomerNotFoundException.java
│       ├── DuplicateCustomerException.java
│       ├── UnauthorizedAccessException.java
│       ├── ErrorResponse.java
│       └── GlobalExceptionHandler.java
└── src/main/resources/
    └── application.yml                         # Configuration
```

### Component Overview

| Component | Count | Purpose |
|-----------|-------|---------|
| Controllers | 1 | REST API endpoints |
| Services | 1 | Business logic |
| Entities | 1 | Database models |
| Repositories | 1 | Data access |
| DTOs | 6 | Request/Response models |
| Clients | 1 | Inter-service communication |
| Configs | 4 | Security, REST, Swagger |
| Exception Handlers | 5 | Custom exceptions + global handler |

---

## API Endpoints

### Endpoint Summary

| Method | Endpoint | Auth Required | Roles | Description |
|--------|----------|---------------|-------|-------------|
| GET | `/health` | No | - | Health check |
| GET | `/all` | Yes | MANAGER, ADMIN | Get all customers |
| GET | `/profile` | Yes | Any | Get own profile |
| POST | `/` | Yes | CUSTOMER, MANAGER, ADMIN | Create customer |
| GET | `/{id}` | Yes | CUSTOMER, MANAGER, ADMIN | Get customer by ID |
| GET | `/user/{userId}` | Yes | CUSTOMER, MANAGER, ADMIN | Get customer by userId |
| PUT | `/{id}` | Yes | CUSTOMER, MANAGER, ADMIN | Update customer |
| GET | `/{id}/classification` | Yes | CUSTOMER, MANAGER, ADMIN | Get classification |
| GET | `/{id}/360-view` | Yes | MANAGER, ADMIN | Get 360° view |

---

### 1. Health Check

**Endpoint:** `GET /health`
**Authentication:** Not required
**Authorization:** Public access

**Success Response (200 OK):**
```
Customer Service is UP
```

**Use Case:** Service monitoring and health checks.

---

### 2. Get All Customers

**Endpoint:** `GET /all`
**Authentication:** Required (JWT)
**Authorization:** ROLE_MANAGER, ROLE_ADMIN

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK):**
```json
[
  {
    "id": 1,
    "userId": 2,
    "username": "john_doe",
    "fullName": "John Doe",
    "mobileNumber": "9876543210",
    "email": "john@example.com",
    "dateOfBirth": "1990-05-15",
    "gender": "MALE",
    "classification": "REGULAR",
    "kycStatus": "VERIFIED",
    "addressLine1": "123 Main Street",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400001",
    "country": "India",
    "isActive": true,
    "preferredLanguage": "en",
    "preferredCurrency": "INR",
    "createdAt": "2025-11-07T10:00:00",
    "updatedAt": "2025-11-07T10:00:00"
  }
]
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `403 Forbidden`: User doesn't have MANAGER or ADMIN role

**Business Logic:**
- Fetches all customers from database
- No pagination (⚠️ Issue: Should add pagination for large datasets)
- Returns list of CustomerResponse DTOs

---

### 3. Get Own Profile

**Endpoint:** `GET /profile`
**Authentication:** Required (JWT)
**Authorization:** Any authenticated user

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "userId": 2,
  "username": "john_doe",
  "fullName": "John Doe",
  "mobileNumber": "9876543210",
  "email": "john@example.com",
  "panNumber": "ABCDE1234F",
  "aadharNumber": "123456789012",
  "dateOfBirth": "1990-05-15",
  "gender": "MALE",
  "classification": "REGULAR",
  "kycStatus": "VERIFIED",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apt 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "country": "India",
  "accountNumber": "1234567890",
  "ifscCode": "SBIN0001234",
  "isActive": true,
  "preferredLanguage": "en",
  "preferredCurrency": "INR",
  "emailNotifications": true,
  "smsNotifications": true,
  "createdAt": "2025-11-07T10:00:00",
  "updatedAt": "2025-11-07T10:00:00"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `404 Not Found`: Customer profile not found for this user

**Business Logic:**
1. Extract username from JWT token
2. Find customer by username
3. Return CustomerResponse
4. Throw 404 if no profile found

**Use Case:** User views their own customer profile.

---

### 4. Create Customer

**Endpoint:** `POST /`
**Authentication:** Required (JWT)
**Authorization:** ROLE_CUSTOMER (own profile), ROLE_MANAGER, ROLE_ADMIN (any user)

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "fullName": "John Doe",
  "mobileNumber": "9876543210",
  "email": "john@example.com",
  "panNumber": "ABCDE1234F",
  "aadharNumber": "123456789012",
  "dateOfBirth": "1990-05-15",
  "gender": "MALE",
  "classification": "REGULAR",
  "addressLine1": "123 Main Street",
  "addressLine2": "Apt 4B",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "country": "India",
  "accountNumber": "1234567890",
  "ifscCode": "SBIN0001234",
  "preferredLanguage": "en",
  "preferredCurrency": "INR",
  "emailNotifications": true,
  "smsNotifications": true
}
```

**Field Validation:**
- `fullName`: Required, max 100 chars
- `mobileNumber`: Required, 10 digits
- `email`: Required, valid email format
- `panNumber`: Optional, format: ABCDE1234F
- `aadharNumber`: Optional, 12 digits
- `dateOfBirth`: Required, must be past date
- `gender`: Required, MALE|FEMALE|OTHER
- `classification`: Required, REGULAR|PREMIUM|VIP|SENIOR_CITIZEN|SUPER_SENIOR
- `addressLine1`: Required, max 255 chars
- `city`: Required, max 100 chars
- `state`: Required, max 100 chars
- `pincode`: Required, 6 digits
- `country`: Required, max 100 chars

**Success Response (201 Created):**
```json
{
  "id": 1,
  "userId": 2,
  "username": "john_doe",
  "fullName": "John Doe",
  ...
}
```

**Error Responses:**
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid or missing token
- `409 Conflict`: Duplicate customer (mobile, email, PAN, Aadhar, or userId)

**Business Logic:**
1. Extract username from JWT
2. Call LoginServiceClient to get userId for username
3. Check if user already has a profile (regular users only)
4. Check for duplicates: mobile, email, PAN, Aadhar, userId
5. Auto-classify based on age:
   - If age >= 80: SUPER_SENIOR
   - If age >= 60: SENIOR_CITIZEN
   - Otherwise: Use requested classification
6. Set defaults: kycStatus=PENDING, isActive=true
7. Save customer
8. Return CustomerResponse

**Use Case:** User creates their customer profile after registration, or admin creates profile for users.

---

### 5. Get Customer by ID

**Endpoint:** `GET /{id}`
**Authentication:** Required (JWT)
**Authorization:** ROLE_CUSTOMER, ROLE_MANAGER, ROLE_ADMIN

**Path Parameter:**
- `id`: Customer ID (Long)

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "userId": 2,
  "username": "john_doe",
  "fullName": "John Doe",
  ...
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `404 Not Found`: Customer not found with given ID

**Business Logic:**
- Fetch customer by ID
- Return CustomerResponse
- Throw 404 if not found

**Use Case:** View customer details by customer ID.

---

### 6. Get Customer by User ID

**Endpoint:** `GET /user/{userId}`
**Authentication:** Required (JWT)
**Authorization:** ROLE_CUSTOMER, ROLE_MANAGER, ROLE_ADMIN

**Path Parameter:**
- `userId`: User ID from login-service (Long)

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "userId": 2,
  "username": "john_doe",
  "fullName": "John Doe",
  ...
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `404 Not Found`: Customer not found with given userId

**Business Logic:**
- Fetch customer by userId
- Return CustomerResponse
- Throw 404 if not found

**Use Case:** Other services look up customer by userId from login-service.

---

### 7. Update Customer

**Endpoint:** `PUT /{id}`
**Authentication:** Required (JWT)
**Authorization:** ROLE_CUSTOMER (own profile only), ROLE_MANAGER, ROLE_ADMIN (any profile)

**Path Parameter:**
- `id`: Customer ID (Long)

**Request Headers:**
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

**Request Body (all fields optional):**
```json
{
  "fullName": "John Updated Doe",
  "mobileNumber": "9876543211",
  "email": "john.updated@example.com",
  "panNumber": "XYZZZ9999X",
  "aadharNumber": "999988887777",
  "dateOfBirth": "1990-05-16",
  "gender": "MALE",
  "classification": "PREMIUM",
  "addressLine1": "456 New Street",
  "addressLine2": null,
  "city": "Delhi",
  "state": "Delhi",
  "pincode": "110001",
  "country": "India",
  "accountNumber": "9999999999",
  "ifscCode": "ICIC0001234",
  "preferredLanguage": "hi",
  "preferredCurrency": "USD",
  "emailNotifications": false,
  "smsNotifications": true
}
```

**Success Response (200 OK):**
```json
{
  "id": 1,
  "userId": 2,
  "username": "john_doe",
  "fullName": "John Updated Doe",
  ...
}
```

**Error Responses:**
- `400 Bad Request`: Validation errors
- `401 Unauthorized`: Invalid or missing token
- `403 Forbidden`: Non-admin trying to update another user's profile
- `404 Not Found`: Customer not found with given ID
- `409 Conflict`: Duplicate mobile or email

**Business Logic:**
1. Extract username from JWT
2. Fetch customer by ID (throw 404 if not found)
3. **Authorization Check:**
   - Regular users: Can only update if customer.username == authenticated username
   - Admin: Can update any customer
   - Throw 403 if unauthorized
4. Update only non-null fields
5. Check for duplicates if mobile/email changed
6. Re-classify if DOB changed and classification is age-based
7. Save updated customer
8. Return CustomerResponse

**Use Case:** User updates their profile, or admin updates any customer profile.

---

### 8. Get Customer Classification

**Endpoint:** `GET /{id}/classification`
**Authentication:** Required (JWT)
**Authorization:** ROLE_CUSTOMER, ROLE_MANAGER, ROLE_ADMIN

**Path Parameter:**
- `id`: Customer ID (Long)

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK):**
```json
{
  "customerId": 1,
  "fullName": "John Doe",
  "classification": "SENIOR_CITIZEN",
  "additionalRatePercentage": 0.50,
  "classificationDescription": "Senior Citizen (60-79 years) - Eligible for 0.50% additional interest rate"
}
```

**Classification Types & Additional Rates:**
- **REGULAR**: 0.00%
- **PREMIUM**: 0.25%
- **VIP**: 0.50%
- **SENIOR_CITIZEN** (60-79 years): 0.50%
- **SUPER_SENIOR** (80+ years): 0.75%

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `404 Not Found`: Customer not found

**Business Logic:**
- Fetch customer by ID
- Calculate classification description
- Determine additional interest rate based on classification
- Return CustomerClassificationResponse

**Use Case:** FD service queries customer classification to determine interest rates.

---

### 9. Get Customer 360° View

**Endpoint:** `GET /{id}/360-view`
**Authentication:** Required (JWT)
**Authorization:** ROLE_MANAGER, ROLE_ADMIN

**Path Parameter:**
- `id`: Customer ID (Long)

**Request Headers:**
```
Authorization: Bearer <jwt_token>
```

**Success Response (200 OK):**
```json
{
  "customer": {
    "id": 1,
    "userId": 2,
    "username": "john_doe",
    "fullName": "John Doe",
    ...
  },
  "classification": {
    "customerId": 1,
    "fullName": "John Doe",
    "classification": "REGULAR",
    "additionalRatePercentage": 0.00,
    "classificationDescription": "Regular Customer - Standard interest rates apply"
  },
  "accountSummary": {
    "totalFdAccounts": 0,
    "totalInvestment": 0.00,
    "totalMaturityAmount": 0.00,
    "activeFdCount": 0,
    "maturedFdCount": 0
  },
  "fdAccounts": []
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid or missing token
- `403 Forbidden`: User doesn't have MANAGER or ADMIN role
- `404 Not Found`: Customer not found

**Business Logic:**
1. Fetch customer by ID
2. Convert to CustomerResponse
3. Generate CustomerClassificationResponse
4. Create AccountSummary (currently empty - pending FD service integration)
5. Fetch FD accounts (currently empty - pending integration)
6. Return comprehensive Customer360Response

**⚠️ Note:** FD accounts integration is **TODO** - currently returns empty data.

**Use Case:** Manager/Admin views comprehensive customer information including FD accounts.

---

## Security & Authentication

### JWT Configuration

**Secret Key:** 256-bit key stored in application.yml (⚠️ Should be externalized)
**Algorithm:** HS256 (HMAC with SHA-256)
**Expiration:** 1 hour (3600000 milliseconds)
**Token Type:** Bearer

**JWT Structure:**
```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "john_doe",           // username
  "roles": ["ROLE_CUSTOMER"],  // user roles
  "iat": 1699900000,           // issued at timestamp
  "exp": 1699903600            // expiration timestamp
}

Signature: HMACSHA256(...)
```

### Spring Security Configuration

**Session Management:** STATELESS (no server-side sessions)
**CSRF Protection:** Disabled (not needed for stateless JWT)
**CORS:** Disabled (⚠️ Should enable for production)

**Public Endpoints (No Authentication):**
- `/health`
- `/swagger-ui/**`
- `/v3/api-docs/**`
- `/swagger-resources/**`
- `/webjars/**`

**Protected Endpoints:**
All other endpoints require:
- Valid JWT token in `Authorization: Bearer <token>` header
- User must have one of: ROLE_CUSTOMER, ROLE_MANAGER, or ROLE_ADMIN

### JWT Authentication Filter

**Class:** `JwtAuthenticationFilter`
**Type:** `OncePerRequestFilter`
**Order:** Before `UsernamePasswordAuthenticationFilter`

**Flow:**
1. Extract JWT token from `Authorization` header
2. Validate token format: "Bearer <token>"
3. Extract username from token
4. Validate token signature and expiration
5. Extract roles from token claims
6. Create Spring Security authentication object
7. Set SecurityContext
8. Continue filter chain

### Role-Based Access Control (RBAC)

**Target Roles (After Migration):**
```java
ROLE_ADMIN      // System administrator
ROLE_MANAGER    // Customer operations manager
ROLE_CUSTOMER   // Regular customer
```

**Current Roles (Before Migration):**
```java
ROLE_ADMIN              // Admin (no change)
ROLE_CUSTOMER_MANAGER   // → ROLE_MANAGER
ROLE_USER               // → ROLE_CUSTOMER
ROLE_FD_MANAGER         // → Remove or merge with ROLE_MANAGER
```

### Endpoint Access Matrix

| Endpoint | CUSTOMER | MANAGER | ADMIN |
|----------|----------|---------|-------|
| GET /health | ✅ Public | ✅ Public | ✅ Public |
| GET /all | ❌ | ✅ | ✅ |
| GET /profile | ✅ | ✅ | ✅ |
| POST / | ✅ (own) | ✅ (any) | ✅ (any) |
| GET /{id} | ✅ | ✅ | ✅ |
| GET /user/{userId} | ✅ | ✅ | ✅ |
| PUT /{id} | ✅ (own) | ✅ (any) | ✅ (any) |
| GET /{id}/classification | ✅ | ✅ | ✅ |
| GET /{id}/360-view | ❌ | ✅ | ✅ |

**Ownership Validation:**
- Regular customers can only create/update their own profile
- Admins can create/update any profile
- Username from JWT must match customer.username for ownership

---

## Database Schema

**Database Name:** `customer_db`
**Connection URL:** `jdbc:mysql://localhost:3306/customer_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC`
**Username:** root
**Password:** root
**Schema Generation:** Hibernate (ddl-auto: update)

### Table: customers

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Customer ID |
| user_id | BIGINT | NOT NULL, UNIQUE | User ID from login-service |
| username | VARCHAR(50) | NOT NULL, UNIQUE | Username from login-service |
| full_name | VARCHAR(100) | NOT NULL | Full name |
| mobile_number | VARCHAR(15) | NOT NULL, UNIQUE | Mobile number |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email address |
| pan_number | VARCHAR(20) | NULL | PAN card number |
| aadhar_number | VARCHAR(20) | NULL | Aadhar number |
| date_of_birth | DATE | NOT NULL | Date of birth |
| gender | VARCHAR(20) | NOT NULL | MALE, FEMALE, OTHER |
| classification | VARCHAR(20) | NOT NULL | Customer classification |
| kyc_status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | KYC verification status |
| address_line1 | VARCHAR(255) | NULL | Address line 1 |
| address_line2 | VARCHAR(255) | NULL | Address line 2 |
| city | VARCHAR(100) | NULL | City |
| state | VARCHAR(100) | NULL | State |
| pincode | VARCHAR(10) | NULL | Postal code |
| country | VARCHAR(100) | NULL | Country |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | Active status |
| account_number | VARCHAR(50) | NULL | Bank account number |
| ifsc_code | VARCHAR(20) | NULL | IFSC code |
| preferred_language | VARCHAR(10) | DEFAULT 'en' | Preferred language |
| preferred_currency | VARCHAR(10) | DEFAULT 'INR' | Preferred currency |
| email_notifications | BOOLEAN | NOT NULL, DEFAULT TRUE | Email notification preference |
| sms_notifications | BOOLEAN | NOT NULL, DEFAULT TRUE | SMS notification preference |
| created_at | DATETIME | NOT NULL | Creation timestamp |
| updated_at | DATETIME | NOT NULL | Last update timestamp |
| created_by | BIGINT | NULL | Creator user ID |
| updated_by | BIGINT | NULL | Last updater user ID |

**Indexes:**
- PRIMARY KEY: id
- UNIQUE: user_id
- UNIQUE: username
- UNIQUE: mobile_number
- UNIQUE: email
- INDEX: classification (for queries)
- INDEX: kyc_status (for queries)

**Enums:**

*Gender:*
- MALE
- FEMALE
- OTHER

*Classification:*
- REGULAR
- PREMIUM
- VIP
- SENIOR_CITIZEN (60-79 years)
- SUPER_SENIOR (80+ years)

*KycStatus:*
- PENDING
- IN_PROGRESS
- VERIFIED
- REJECTED
- EXPIRED

**Entity Lifecycle Callbacks:**
- **@PrePersist**: Sets createdAt and updatedAt to current timestamp
- **@PreUpdate**: Updates updatedAt to current timestamp

---

## Business Logic Flows

### 1. Customer Creation Flow

```
Client Request (POST /)
    ↓
[Controller] CustomerController.createCustomer()
    ↓
[Authentication] JWT token validated, username extracted
    ↓
[Service] CustomerService.createCustomer()
    ↓
[Login Service Call] LoginServiceClient.getUserIdByUsername(username)
    ↓ Returns userId
[Authorization Check]
    - Regular users: Check if they already have a profile
      → If yes: Throw DuplicateCustomerException
    - Admin: Can create for any user
    ↓
[Duplicate Validation]
    - Check userId exists? → Throw DuplicateCustomerException
    - Check mobile exists? → Throw DuplicateCustomerException
    - Check email exists? → Throw DuplicateCustomerException
    - Check PAN exists? → Throw DuplicateCustomerException
    - Check Aadhar exists? → Throw DuplicateCustomerException
    ↓
[Age Calculation & Classification]
    age = current year - birth year
    ↓
    If age >= 80:
        classification = SUPER_SENIOR (override request)
    Else if age >= 60:
        classification = SENIOR_CITIZEN (override request)
    Else:
        classification = requested classification
    ↓
[Entity Creation]
    Customer customer = new Customer()
    - Set all fields from request
    - Set userId and username
    - Set classification (possibly overridden)
    - Set defaults: kycStatus=PENDING, isActive=true
    ↓
[Persistence]
    customerRepository.save(customer)
    ↓
[Response]
    Convert to CustomerResponse
    Return with HTTP 201 CREATED
```

**Key Points:**
- Each user can only have ONE customer profile
- Classification auto-determined for seniors (60+)
- Unique constraints on mobile, email, PAN, Aadhar, userId
- KYC status defaults to PENDING

---

### 2. Customer Update Flow

```
Client Request (PUT /{id})
    ↓
[Controller] CustomerController.updateCustomer()
    ↓
[Authentication] JWT token validated, username extracted
    ↓
[Service] CustomerService.updateCustomer()
    ↓
[Fetch Customer]
    customer = customerRepository.findById(id)
    → If not found: Throw CustomerNotFoundException
    ↓
[Authorization Check]
    isAdmin = roles.contains("ROLE_ADMIN")

    If !isAdmin:
        If customer.username != authenticated username:
            → Throw UnauthorizedAccessException
    ↓ Authorized
[Field Updates]
    For each non-null field in UpdateCustomerRequest:
        - Update customer entity field
    ↓
[Duplicate Validation]
    If mobileNumber changed:
        Check if another customer has this mobile
        → If yes: Throw DuplicateCustomerException

    If email changed:
        Check if another customer has this email
        → If yes: Throw DuplicateCustomerException
    ↓
[Classification Recalculation]
    If dateOfBirth changed AND
       (classification == SENIOR_CITIZEN OR SUPER_SENIOR):

        Recalculate age
        If age >= 80: classification = SUPER_SENIOR
        Else if age >= 60: classification = SENIOR_CITIZEN
    ↓
[Persistence]
    customerRepository.save(customer)
    ↓
[Response]
    Convert to CustomerResponse
    Return with HTTP 200 OK
```

**Key Points:**
- Ownership validation: users can only update their own profile
- Admins can update any profile
- Classification recalculated only for age-based types
- Partial updates supported (only non-null fields updated)

---

### 3. Customer Classification Logic

**Purpose:** Determine additional interest rates for FD accounts

**Classification to Rate Mapping:**
```java
switch (classification) {
    case REGULAR:
        additionalRate = 0.00%;
        break;
    case PREMIUM:
        additionalRate = 0.25%;
        break;
    case VIP:
        additionalRate = 0.50%;
        break;
    case SENIOR_CITIZEN:
        additionalRate = 0.50%;
        break;
    case SUPER_SENIOR:
        additionalRate = 0.75%;
        break;
}
```

**Auto-Classification Rules:**
```java
int age = calculateAge(dateOfBirth);

if (age >= 80) {
    // Override any requested classification
    classification = SUPER_SENIOR;
} else if (age >= 60) {
    // Override any requested classification
    classification = SENIOR_CITIZEN;
} else {
    // Use requested classification
    classification = request.getClassification();
}
```

**Classification Descriptions:**
- REGULAR: "Regular Customer - Standard interest rates apply"
- PREMIUM: "Premium Customer - Eligible for 0.25% additional interest rate"
- VIP: "VIP Customer - Eligible for 0.50% additional interest rate"
- SENIOR_CITIZEN: "Senior Citizen (60-79 years) - Eligible for 0.50% additional interest rate"
- SUPER_SENIOR: "Super Senior Citizen (80+ years) - Eligible for 0.75% additional interest rate"

---

### 4. Customer 360° View Logic

**Purpose:** Provide comprehensive customer information for managers/admins

**Current Implementation:**
```
GET /{id}/360-view
    ↓
Fetch customer by ID
    ↓
Generate CustomerResponse
    ↓
Generate CustomerClassificationResponse
    ↓
Create AccountSummary
    - totalFdAccounts: 0 (TODO)
    - totalInvestment: 0.00 (TODO)
    - totalMaturityAmount: 0.00 (TODO)
    - activeFdCount: 0 (TODO)
    - maturedFdCount: 0 (TODO)
    ↓
Fetch FD Accounts
    - Currently returns empty list (TODO)
    ↓
Return Customer360Response
```

**⚠️ TODO:** Integrate with FD Account Service to fetch:
- List of FD accounts
- Account balances
- Maturity dates
- Transaction history
- Account summary statistics

**Response Structure:**
```json
{
  "customer": {...},           // Full customer details
  "classification": {...},     // Classification and rates
  "accountSummary": {...},     // FD account summary (TODO)
  "fdAccounts": []             // List of FD accounts (TODO)
}
```

---

## Inter-Service Communication

### LoginServiceClient

**Purpose:** Fetch user information from Login Service

**Base URL:** `http://localhost:8081/api/auth` (configurable via `login-service.url`)

**Transport:** RestTemplate (synchronous HTTP)

**API Called:**
```
GET {login-service.url}/user/{username}
```

**Method Signature:**
```java
public Long getUserIdByUsername(String username)
```

**Request Flow:**
```
CustomerService
    ↓
LoginServiceClient.getUserIdByUsername(username)
    ↓
HTTP GET → http://localhost:8081/api/auth/user/{username}
    ↓
Login Service validates username
    ↓
Returns UserDTO with userId
    ↓
LoginServiceClient extracts userId from response
    ↓
Returns userId to CustomerService
```

**Response Structure:**
```json
{
  "success": true,
  "message": "User found",
  "data": {
    "id": 2,
    "username": "john_doe",
    "email": "john@example.com",
    "mobileNumber": "1234567890",
    "password": null
  },
  "timestamp": "2025-11-07T10:30:00"
}
```

**Error Handling:**
- Catches RestClientException
- Logs error with username
- Throws RuntimeException: "Failed to retrieve user ID from login service"

**⚠️ Improvements Needed:**
- Add circuit breaker (Resilience4j) for fault tolerance
- Implement retry mechanism
- Add timeout configuration
- Migrate to WebClient (non-blocking)

---

## Configuration

### application.yml

```yaml
# Server Configuration
server:
  port: 8083
  servlet:
    context-path: /api/customer

# Spring Application
spring:
  application:
    name: customer-service

  # Database Configuration
  datasource:
    url: jdbc:mysql://localhost:3306/customer_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  # JPA Configuration
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

# JWT Configuration
jwt:
  secret: mySecretKeyForCredexaApplicationMustBe256BitsLongForHS256AlgorithmToWorkProperly
  expiration: 3600000  # 1 hour in milliseconds

# External Service URLs
login-service:
  url: http://localhost:8081/api/auth

# Springdoc OpenAPI (Swagger)
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    operations-sorter: method

# Logging Configuration
logging:
  level:
    com.app.customer: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## Issues & Bugs Found

### 🔴 Critical Issues

#### 1. Role System Mismatch
**Current Roles:** ROLE_USER, ROLE_CUSTOMER_MANAGER, ROLE_ADMIN, ROLE_FD_MANAGER
**Target Roles:** ROLE_CUSTOMER, ROLE_MANAGER, ROLE_ADMIN

**Files Affected:**
- SecurityConfig.java:42
- CustomerController.java:47, 69, 88, 97, 106, 124, 133

**Impact:** Must update all role references

**Fix:** See [Role Migration](#role-migration) section

---

#### 2. Hard-Coded JWT Secret
**Location:** `application.yml:31`

**Problem:** JWT secret exposed in config file

**Fix:** Externalize to environment variable
```yaml
jwt:
  secret: ${JWT_SECRET:default-for-development}
```

---

#### 3. Hard-Coded Database Credentials
**Location:** `application.yml:9-10`

**Problem:** Database credentials in plain text

**Fix:** Externalize to environment variables
```yaml
datasource:
  username: ${DB_USERNAME:root}
  password: ${DB_PASSWORD:root}
```

---

### 🟠 High Priority Issues

#### 4. Incomplete 360° View
**Location:** `CustomerService.java:286-303`

**Problem:** FD accounts data not integrated

**Impact:** Feature returns empty/null data

**Fix:** Integrate with FD Account Service
```java
// TODO: Call FD Account Service
List<FdAccount> fdAccounts = fdAccountClient.getAccountsByCustomerId(customerId);
```

---

#### 5. No Pagination for GET /all
**Location:** `CustomerController.java:47`

**Problem:** Fetches all customers without pagination

**Impact:** Performance issues with large datasets

**Fix:** Add pagination
```java
@GetMapping("/all")
public ResponseEntity<Page<CustomerResponse>> getAllCustomers(Pageable pageable) {
    return ResponseEntity.ok(customerService.getAllCustomers(pageable));
}
```

---

#### 6. No Circuit Breaker for Login Service
**Location:** `LoginServiceClient.java`

**Problem:** If login-service is down, customer-service fails completely

**Fix:** Add Resilience4j circuit breaker
```java
@CircuitBreaker(name = "loginService", fallbackMethod = "getUserIdFallback")
public Long getUserIdByUsername(String username) {
    // existing code
}
```

---

### 🟡 Medium Priority Issues

#### 7. Audit Fields Not Populated
**Location:** `Customer` entity

**Problem:** `createdBy` and `updatedBy` fields never set

**Impact:** No audit trail for who created/updated records

**Fix:** Populate from authenticated userId

---

#### 8. CORS Disabled
**Location:** `SecurityConfig.java`

**Problem:** No CORS configuration

**Fix:** Enable CORS with specific origins

---

#### 9. RestTemplate Instead of WebClient
**Location:** `RestClientConfig.java`

**Problem:** RestTemplate is in maintenance mode

**Fix:** Migrate to WebClient

---

#### 10. No Rate Limiting
**Problem:** No protection against DoS

**Fix:** Implement rate limiting

---

## Role Migration

### Current vs Target Roles

| Current Role | Target Role | Usage |
|--------------|-------------|-------|
| ROLE_USER | ROLE_CUSTOMER | Regular customer access |
| ROLE_CUSTOMER_MANAGER | ROLE_MANAGER | Customer operations manager |
| ROLE_ADMIN | ROLE_ADMIN | System administrator (no change) |
| ROLE_FD_MANAGER | ~~Remove~~ or merge with ROLE_MANAGER | FD operations |

### Files to Update

#### 1. SecurityConfig.java
**Line 42:**
```java
// Before
.requestMatchers("/api/customer/**").hasAnyRole("USER", "CUSTOMER_MANAGER", "ADMIN")

// After
.requestMatchers("/api/customer/**").hasAnyRole("CUSTOMER", "MANAGER", "ADMIN")
```

#### 2. CustomerController.java

**Line 47 (GET /all):**
```java
// Before
@PreAuthorize("hasAnyRole('CUSTOMER_MANAGER', 'ADMIN')")

// After
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
```

**Lines 69, 88, 97, 106 (POST, GET endpoints):**
```java
// Before
@PreAuthorize("hasAnyRole('USER', 'CUSTOMER_MANAGER', 'ADMIN')")

// After
@PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
```

**Line 124 (GET /classification):**
```java
// Before
@PreAuthorize("hasAnyRole('USER', 'CUSTOMER_MANAGER', 'ADMIN', 'FD_MANAGER')")

// After
@PreAuthorize("hasAnyRole('CUSTOMER', 'MANAGER', 'ADMIN')")
```

**Line 133 (GET /360-view):**
```java
// Before
@PreAuthorize("hasAnyRole('CUSTOMER_MANAGER', 'ADMIN')")

// After
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
```

---

## Test Cases

### TC-CS-001: Health Check
**Endpoint:** GET /health
**Auth:** Not required
**Expected:** 200 OK, "Customer Service is UP"

### TC-CS-002: Create Customer (Success)
**Endpoint:** POST /
**Auth:** Required (CUSTOMER role)
**Precondition:** User has no existing profile
**Request:** Valid CreateCustomerRequest
**Expected:** 201 Created, CustomerResponse with auto-classification

### TC-CS-003: Create Customer (Duplicate)
**Endpoint:** POST /
**Auth:** Required
**Precondition:** User already has profile
**Expected:** 409 Conflict, "You already have a customer profile"

### TC-CS-004: Get Own Profile
**Endpoint:** GET /profile
**Auth:** Required
**Expected:** 200 OK, own CustomerResponse

### TC-CS-005: Update Own Profile
**Endpoint:** PUT /{id}
**Auth:** Required (CUSTOMER role)
**Precondition:** Updating own profile
**Expected:** 200 OK, updated CustomerResponse

### TC-CS-006: Update Other's Profile (Non-Admin)
**Endpoint:** PUT /{id}
**Auth:** Required (CUSTOMER role)
**Precondition:** Trying to update another user's profile
**Expected:** 403 Forbidden

### TC-CS-007: Update Other's Profile (Admin)
**Endpoint:** PUT /{id}
**Auth:** Required (ADMIN role)
**Expected:** 200 OK, updated CustomerResponse

### TC-CS-008: Get Customer Classification
**Endpoint:** GET /{id}/classification
**Auth:** Required
**Expected:** 200 OK, CustomerClassificationResponse with correct rates

### TC-CS-009: Auto-Classification (Senior Citizen)
**Endpoint:** POST /
**Request:** DOB makes age = 65
**Expected:** Classification = SENIOR_CITIZEN, additionalRate = 0.50%

### TC-CS-010: Auto-Classification (Super Senior)
**Endpoint:** POST /
**Request:** DOB makes age = 82
**Expected:** Classification = SUPER_SENIOR, additionalRate = 0.75%

### TC-CS-011: Get 360° View (Manager)
**Endpoint:** GET /{id}/360-view
**Auth:** Required (MANAGER role)
**Expected:** 200 OK, Customer360Response

### TC-CS-012: Get 360° View (Customer)
**Endpoint:** GET /{id}/360-view
**Auth:** Required (CUSTOMER role)
**Expected:** 403 Forbidden

### TC-CS-013: Get All Customers (Manager)
**Endpoint:** GET /all
**Auth:** Required (MANAGER role)
**Expected:** 200 OK, List<CustomerResponse>

### TC-CS-014: Get All Customers (Customer)
**Endpoint:** GET /all
**Auth:** Required (CUSTOMER role)
**Expected:** 403 Forbidden

---

## Test Results

**Execution Date:** [To be filled after testing]

| Test ID | Test Name | Status | Notes |
|---------|-----------|--------|-------|
| TC-CS-001 | Health Check | ⏳ Pending | |
| TC-CS-002 | Create Customer | ⏳ Pending | |
| TC-CS-003 | Duplicate Customer | ⏳ Pending | |
| TC-CS-004 | Get Own Profile | ⏳ Pending | |
| TC-CS-005 | Update Own Profile | ⏳ Pending | |
| TC-CS-006 | Update Other (Non-Admin) | ⏳ Pending | |
| TC-CS-007 | Update Other (Admin) | ⏳ Pending | |
| TC-CS-008 | Get Classification | ⏳ Pending | |
| TC-CS-009 | Auto-Classify Senior | ⏳ Pending | |
| TC-CS-010 | Auto-Classify Super Senior | ⏳ Pending | |
| TC-CS-011 | 360° View (Manager) | ⏳ Pending | |
| TC-CS-012 | 360° View (Customer) | ⏳ Pending | |
| TC-CS-013 | Get All (Manager) | ⏳ Pending | |
| TC-CS-014 | Get All (Customer) | ⏳ Pending | |

---

## Recommendations

### Immediate Actions
1. Update all roles to ROLE_CUSTOMER, ROLE_MANAGER, ROLE_ADMIN
2. Externalize JWT secret and database credentials
3. Add pagination to GET /all endpoint
4. Implement FD service integration for 360° view

### Security Enhancements
5. Enable CORS with specific origins
6. Add circuit breaker for login-service calls
7. Implement rate limiting
8. Mask PII in logs
9. Add API versioning

### Performance
10. Migrate to WebClient (non-blocking)
11. Add caching for frequently accessed customer data
12. Optimize database queries with proper indexes

### Data Integrity
13. Populate audit fields (createdBy, updatedBy)
14. Add soft-delete mechanism
15. Implement data retention policies

---

**Document Version:** 1.0
**Last Updated:** 2025-11-07
**Author:** AI-Generated Documentation
**Status:** Draft (Awaiting Fixes and Testing)
