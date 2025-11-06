# Lab L7: Login Process with Customer Module Integration

## Implementation Status: ✅ COMPLETE

### Overview
Lab L7 validates the complete authentication and authorization flow across the Login Service and Customer Service. This lab demonstrates how JWT tokens are generated during login, transmitted to other services, validated, and used for role-based access control.

---

## 🎯 Lab L7 Requirements

### 1. Token-Based Login
- ✅ User logs in with credentials (username/email/mobile + password)
- ✅ System validates credentials against database
- ✅ JWT token is generated and returned in response
- ✅ Token contains username (sub claim) and roles (roles claim)
- ✅ Token has 1-hour expiration (3600000ms)

### 2. Token Transmission
- ✅ Frontend stores JWT token (in memory/localStorage)
- ✅ Token is attached to all subsequent requests via Authorization header
- ✅ Format: `Authorization: Bearer <token>`

### 3. Token Validation in Customer Module
- ✅ JwtAuthenticationFilter intercepts all requests
- ✅ Extracts JWT from Authorization header
- ✅ Validates JWT signature using shared secret
- ✅ Extracts username and roles from token claims
- ✅ Creates Authentication object and sets in SecurityContext

### 4. Role-Based Access Control
- ✅ **CUSTOMER/USER Role**: Can only access own profile (`GET /profile`)
- ✅ **ADMIN/CUSTOMER_MANAGER Role**: Can access all profiles (`GET /all`, `GET /{id}`)
- ✅ SecurityConfig enforces URL-level security
- ✅ @PreAuthorize annotations provide method-level security

---

## 🔐 JWT Token Structure

### Token Format
```
eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNzYyMzM5MzU5LCJleHAiOjE3NjIzNDI5NTl9.VMOZuE8yvzFi4iEM5BbxjzeRi1ImFvUi4NjNEdLM9KJ9H1BGAOirNsdr9MFiaLUP5G2eColSrMWMu-A1Yp1_RQ
```

### Token Structure (3 parts separated by `.`)
1. **Header**: Algorithm and token type
   ```json
   {
     "alg": "HS512",
     "typ": "JWT"
   }
   ```

2. **Payload**: User claims
   ```json
   {
     "roles": ["ROLE_ADMIN"],
     "sub": "admin",
     "iat": 1762339359,
     "exp": 1762342959
   }
   ```
   - `sub`: Subject (username)
   - `roles`: Array of role names (e.g., ROLE_ADMIN, ROLE_USER, ROLE_CUSTOMER_MANAGER)
   - `iat`: Issued at timestamp (Unix epoch)
   - `exp`: Expiration timestamp (Unix epoch)

3. **Signature**: HMAC-SHA512 signature
   - Secret Key: `mySecretKeyForCredexaApplicationMustBe256BitsLongForHS256AlgorithmToWorkProperly`
   - Algorithm: HS512 (shared across all services)

---

## 🔄 Complete Login Flow

### Step 1: User Login
```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "usernameOrEmailOrMobile": "admin",
  "password": "Admin@123"
}
```

**Response (200 OK):**
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
    "loginTime": "2025-11-05T16:12:39",
    "expiresIn": 3600000
  }
}
```

### Step 2: Store Token
Frontend application stores the JWT token:
```javascript
// Store in memory or localStorage
localStorage.setItem('authToken', response.data.token);
```

### Step 3: Attach Token to Requests
Every request to protected endpoints includes the token:
```http
GET http://localhost:8083/api/customer/profile
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### Step 4: Token Validation (JwtAuthenticationFilter)
```java
// 1. Extract token from Authorization header
String authHeader = request.getHeader("Authorization");
String jwt = authHeader.substring(7); // Remove "Bearer " prefix

// 2. Extract username from token
String username = jwtUtil.extractUsername(jwt);

// 3. Validate token signature and expiration
if (jwtUtil.validateToken(jwt)) {
    // 4. Extract roles from token
    List<String> roles = jwtUtil.extractRoles(jwt);
    
    // 5. Convert roles to GrantedAuthority
    List<GrantedAuthority> authorities = roles.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
    
    // 6. Create Authentication object
    UsernamePasswordAuthenticationToken authToken = 
        new UsernamePasswordAuthenticationToken(username, null, authorities);
    
    // 7. Set in SecurityContext
    SecurityContextHolder.getContext().setAuthentication(authToken);
}
```

### Step 5: Access Control
Spring Security checks the Authentication object:
```java
// URL-level security (SecurityConfig)
requestMatchers("/**").hasAnyRole("USER", "CUSTOMER_MANAGER", "ADMIN")

// Method-level security (@PreAuthorize)
@PreAuthorize("hasAnyRole('CUSTOMER_MANAGER', 'ADMIN')")
public ResponseEntity<List<CustomerResponse>> getAllCustomers() { ... }
```

---

## 👥 Role-Based Access Matrix

| Endpoint | Description | ROLE_USER | ROLE_CUSTOMER_MANAGER | ROLE_ADMIN |
|----------|-------------|-----------|----------------------|------------|
| `POST /api/auth/login` | Login | ✅ Public | ✅ Public | ✅ Public |
| `POST /api/auth/register` | Register | ✅ Public | ✅ Public | ✅ Public |
| `GET /api/customer/profile` | Own profile | ✅ Yes | ✅ Yes | ✅ Yes |
| `GET /api/customer/all` | All customers | ❌ No | ✅ Yes | ✅ Yes |
| `GET /api/customer/{id}` | Customer by ID | ✅ Yes* | ✅ Yes | ✅ Yes |
| `POST /api/customer` | Create customer | ✅ Yes* | ✅ Yes | ✅ Yes |
| `PUT /api/customer/{id}` | Update customer | ✅ Yes* | ✅ Yes | ✅ Yes |
| `GET /api/customer/{id}/360-view` | 360° view | ❌ No | ✅ Yes | ✅ Yes |
| `GET /api/customer/{id}/classification` | Classification | ✅ Yes | ✅ Yes | ✅ Yes |

**\*Note**: ROLE_USER can only access/modify their own profile, not others.

---

## 🧪 Test Scenarios & Results

### Test 1: Admin Login ✅ PASS
```powershell
# Login as admin
$loginBody = @{usernameOrEmailOrMobile="admin"; password="Admin@123"} | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST `
  -Body $loginBody -ContentType "application/json" -UseBasicParsing
```

**Result**: Status 200, JWT token received with `roles: ["ROLE_ADMIN"]`

---

### Test 2: Admin Access to Own Profile ✅ PASS
```powershell
# Get admin's own profile
$adminToken = "eyJhbGciOiJIUzUxMiJ9..."
$headers = @{Authorization="Bearer $adminToken"}
Invoke-WebRequest -Uri "http://localhost:8083/api/customer/profile" `
  -Method GET -Headers $headers -UseBasicParsing
```

**Result**: Status 200, returns admin customer profile
```json
{
  "id": 1,
  "userId": 1,
  "username": "admin",
  "fullName": "Rohit Sharma",
  "email": "rohit.sharma@example.com",
  "mobileNumber": "9876543210",
  "classification": "REGULAR",
  "kycStatus": "PENDING",
  ...
}
```

---

### Test 3: Admin Access to All Customers ✅ PASS
```powershell
# Get all customers
$adminToken = "eyJhbGciOiJIUzUxMiJ9..."
$headers = @{Authorization="Bearer $adminToken"}
Invoke-WebRequest -Uri "http://localhost:8083/api/customer/all" `
  -Method GET -Headers $headers -UseBasicParsing
```

**Result**: Status 200, returns array of all customer profiles
```json
[
  {
    "id": 1,
    "userId": 1,
    "username": "admin",
    "fullName": "Rohit Sharma",
    ...
  }
]
```

---

### Test 4: Regular User Login ✅ PASS
```powershell
# Register new user
$registerBody = @{
  username="testcustomer"
  password="Test@123"
  email="testcustomer@example.com"
  mobileNumber="9876543211"
} | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:8081/api/auth/register" `
  -Method POST -Body $registerBody -ContentType "application/json" -UseBasicParsing

# Login as testcustomer
$loginBody = @{usernameOrEmailOrMobile="testcustomer"; password="Test@123"} | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:8081/api/auth/login" -Method POST `
  -Body $loginBody -ContentType "application/json" -UseBasicParsing
```

**Result**: Status 200, JWT token received with `roles: ["ROLE_USER"]`

---

### Test 5: User Access to All Customers ✅ PASS (Correctly Denied)
```powershell
# Attempt to get all customers with USER role
$userToken = "eyJhbGciOiJIUzUxMiJ9..." # USER role token
$headers = @{Authorization="Bearer $userToken"}
Invoke-WebRequest -Uri "http://localhost:8083/api/customer/all" `
  -Method GET -Headers $headers -UseBasicParsing
```

**Expected Result**: Status 403 Forbidden - Access Denied
**Actual Result**: Access denied (USER role does not have permission to view all customers)

---

## 🏗️ Architecture Components

### 1. Login Service (Port 8081)
**Components:**
- `AuthController`: Handles `/login`, `/register`, `/logout`, `/validate-token` endpoints
- `AuthService`: Business logic for authentication
- `JwtUtil`: JWT token generation and validation
- `UserRepository`: Database access for user credentials
- `RoleRepository`: Database access for roles
- `UserSessionRepository`: Tracks active sessions
- `AuditLogRepository`: Logs authentication events

**Key Features:**
- BCrypt password hashing
- Account lockout after 5 failed attempts
- Session tracking with IP and user agent
- Audit logging for security events
- Multi-factor login (username/email/mobile)

---

### 2. Customer Service (Port 8083)
**Components:**
- `JwtAuthenticationFilter`: Validates JWT and extracts Authentication
- `SecurityConfig`: Configures security rules and filter chain
- `CustomerController`: REST endpoints with @PreAuthorize annotations
- `CustomerService`: Business logic for customer operations

**Security Configuration:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/swagger-ui/**").permitAll()
                .requestMatchers("/**").hasAnyRole("USER", "CUSTOMER_MANAGER", "ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

### 3. Common Library (Shared)
**Components:**
- `JwtUtil`: Shared JWT utility for all services
- `ApiResponse`: Standardized API response wrapper

**Shared Configuration:**
```yaml
jwt:
  secret: mySecretKeyForCredexaApplicationMustBe256BitsLongForHS256AlgorithmToWorkProperly
  expiration: 3600000  # 1 hour
```

---

## 🔍 Security Features

### 1. Password Security
- ✅ BCrypt hashing (cost factor 10)
- ✅ No plain text passwords stored
- ✅ Passwords removed from API responses

### 2. Token Security
- ✅ JWT signed with HS512 algorithm
- ✅ 256-bit secret key (shared across services)
- ✅ 1-hour expiration
- ✅ Includes username and roles in claims
- ✅ Stateless (no server-side session storage)

### 3. Access Control
- ✅ Role-based authorization
- ✅ Method-level security with @PreAuthorize
- ✅ URL-level security in SecurityConfig
- ✅ Authentication required for all endpoints (except public ones)

### 4. Audit & Monitoring
- ✅ Login success/failure logging
- ✅ Account lockout after failed attempts
- ✅ Session tracking (IP, user agent, login time)
- ✅ Audit logs with timestamps

### 5. Error Handling
- ✅ Proper HTTP status codes (401 Unauthorized, 403 Forbidden)
- ✅ Generic error messages (no sensitive info leakage)
- ✅ Exception logging for debugging

---

## 📋 API Specifications

### Login Service APIs

#### 1. POST /api/auth/register
Register a new user account.

**Request:**
```json
{
  "username": "johndoe",
  "password": "Password@123",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "preferredLanguage": "en",
  "preferredCurrency": "USD"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 6,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "active": true,
    "roles": [{"id": 2, "name": "ROLE_USER"}],
    "createdAt": "2025-11-05T16:17:14"
  }
}
```

---

#### 2. POST /api/auth/login
Authenticate user and receive JWT token.

**Request:**
```json
{
  "usernameOrEmailOrMobile": "admin",
  "password": "Admin@123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNzYyMzM5MzU5LCJleHAiOjE3NjIzNDI5NTl9.VMOZuE8yvzFi4iEM...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "admin",
    "email": "admin@credexa.com",
    "roles": ["ROLE_ADMIN"],
    "expiresIn": 3600000
  }
}
```

---

#### 3. POST /api/auth/logout
Logout user and invalidate session.

**Request:**
```http
POST http://localhost:8081/api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Logout successful"
}
```

---

#### 4. POST /api/auth/validate-token
Validate if JWT token is still valid.

**Request:**
```json
"eyJhbGciOiJIUzUxMiJ9..."
```

**Response (200 OK):**
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

### Customer Service APIs (Protected)

#### 1. GET /api/customer/profile
Get own customer profile (any authenticated user).

**Request:**
```http
GET http://localhost:8083/api/customer/profile
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "userId": 1,
  "username": "admin",
  "fullName": "Rohit Sharma",
  "email": "rohit.sharma@example.com",
  "mobileNumber": "9876543210",
  "classification": "REGULAR",
  "kycStatus": "PENDING",
  "addressLine1": "Updated Address 123",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "isActive": true
}
```

---

#### 2. GET /api/customer/all
Get all customers (ADMIN/CUSTOMER_MANAGER only).

**Request:**
```http
GET http://localhost:8083/api/customer/all
Authorization: Bearer <admin-token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "userId": 1,
    "username": "admin",
    "fullName": "Rohit Sharma",
    ...
  }
]
```

---

#### 3. POST /api/customer
Create new customer profile.

**Request:**
```http
POST http://localhost:8083/api/customer
Authorization: Bearer <token>
Content-Type: application/json

{
  "username": "admin",
  "fullName": "John Doe",
  "mobileNumber": "9876543210",
  "email": "john@example.com",
  "dateOfBirth": "1990-01-01",
  "gender": "MALE",
  "addressLine1": "123 Main St",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "country": "India"
}
```

**Response (201 Created):**
```json
{
  "id": 2,
  "userId": 6,
  "username": "johndoe",
  "fullName": "John Doe",
  "isActive": true,
  "createdAt": "2025-11-05T16:30:00"
}
```

---

## 🚀 Testing Guide

### Prerequisites
1. All services must be running:
   - Login Service: `http://localhost:8081`
   - Customer Service: `http://localhost:8083`
   - Product Pricing Service: `http://localhost:8084`
   - FD Calculator Service: `http://localhost:8085`

2. MySQL database running with all schemas created

---

### Quick Start Testing (PowerShell)

#### Step 1: Login as Admin
```powershell
$loginBody = @{
    usernameOrEmailOrMobile = "admin"
    password = "Admin@123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
    -Method POST -Body $loginBody -ContentType "application/json"

$adminToken = $response.data.token
Write-Host "Admin Token: Bearer $adminToken" -ForegroundColor Green
```

#### Step 2: Get Own Profile
```powershell
$headers = @{Authorization = "Bearer $adminToken"}
$profile = Invoke-RestMethod -Uri "http://localhost:8083/api/customer/profile" `
    -Method GET -Headers $headers

$profile | ConvertTo-Json -Depth 10
```

#### Step 3: Get All Customers (Admin Only)
```powershell
$headers = @{Authorization = "Bearer $adminToken"}
$allCustomers = Invoke-RestMethod -Uri "http://localhost:8083/api/customer/all" `
    -Method GET -Headers $headers

$allCustomers | ConvertTo-Json -Depth 10
```

#### Step 4: Register and Login as Regular User
```powershell
# Register
$registerBody = @{
    username = "newuser"
    password = "User@123"
    email = "newuser@example.com"
    mobileNumber = "9876543212"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register" `
    -Method POST -Body $registerBody -ContentType "application/json"

# Login
$loginBody = @{
    usernameOrEmailOrMobile = "newuser"
    password = "User@123"
} | ConvertTo-Json

$userResponse = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
    -Method POST -Body $loginBody -ContentType "application/json"

$userToken = $userResponse.data.token
Write-Host "User Token: Bearer $userToken" -ForegroundColor Yellow
```

#### Step 5: Test Access Restrictions
```powershell
# USER trying to access /all (should fail)
$headers = @{Authorization = "Bearer $userToken"}
try {
    Invoke-RestMethod -Uri "http://localhost:8083/api/customer/all" `
        -Method GET -Headers $headers
    Write-Host "❌ FAILED: USER should not access /all" -ForegroundColor Red
} catch {
    Write-Host "✅ PASSED: USER correctly denied access to /all" -ForegroundColor Green
}
```

---

### Swagger UI Testing

#### Login Service Swagger
Open: `http://localhost:8081/api/auth/swagger-ui/index.html`

**Test Flow:**
1. Click on **POST /register** → Try it out
2. Fill in registration details → Execute
3. Click on **POST /login** → Try it out
4. Enter credentials → Execute
5. Copy the `token` value from response

#### Customer Service Swagger
Open: `http://localhost:8083/api/customer/swagger-ui/index.html`

**Test Flow:**
1. Click **Authorize** button (top right)
2. Enter: `Bearer <your-token>`
3. Click **Authorize** → Close
4. Try **GET /profile** → Execute (should succeed)
5. Try **GET /all** → Execute (succeeds only for ADMIN/CUSTOMER_MANAGER)

---

## 📊 Integration Summary

### Labs Integration Status

| Lab | Component | Status | Integration |
|-----|-----------|--------|-------------|
| L1 | User Registration | ✅ Complete | Login Service |
| L2 | User Authentication | ✅ Complete | Login Service (JWT) |
| L3 | User Authorization | ✅ Complete | Customer Service (JWT Filter) |
| L4 | Advanced Authorization | ✅ Complete | Customer Service (@PreAuthorize) |
| L5 | Product & Pricing | ✅ Complete | Product Service |
| L6 | FD Calculator | ✅ Complete | Calculator Service |
| **L7** | **Login Integration** | ✅ **Complete** | **End-to-End Flow** |

---

## ✅ Validation Checklist

### Login Service
- [x] POST /register creates users with hashed passwords
- [x] POST /login validates credentials (username/email/mobile)
- [x] JWT token generated with username (sub) and roles (roles)
- [x] Token expiration set to 1 hour (3600000ms)
- [x] Failed login attempts tracked (account locks after 5 attempts)
- [x] Audit logs created for login/logout events
- [x] Sessions tracked with IP and user agent
- [x] POST /logout invalidates session

### Customer Service
- [x] JwtAuthenticationFilter validates JWT signature
- [x] Username extracted from JWT (sub claim)
- [x] Roles extracted from JWT (roles claim)
- [x] Authentication object created with authorities
- [x] SecurityContext populated with authentication
- [x] SecurityConfig enforces URL-level access
- [x] @PreAuthorize annotations enforce method-level access

### Role-Based Access
- [x] ADMIN can access /all (all customers)
- [x] ADMIN can access /profile (own profile)
- [x] USER can access /profile (own profile only)
- [x] USER cannot access /all (denied with 403)
- [x] CUSTOMER_MANAGER can access /all
- [x] @PreAuthorize correctly restricts methods

### Security
- [x] Passwords hashed with BCrypt
- [x] JWT signed with HS512
- [x] Shared secret (256-bit key)
- [x] Token expiration enforced
- [x] Stateless authentication (no sessions)
- [x] CSRF disabled (stateless API)
- [x] CORS configured (optional)

---

## 🎓 Lab L7 Learning Outcomes

Students completing this lab will understand:

1. **JWT Token Generation**: How authentication services create signed tokens with user claims
2. **Token Transmission**: Best practices for sending JWTs in HTTP headers
3. **Token Validation**: How services validate JWT signatures and extract claims
4. **Spring Security Integration**: Using filters and SecurityContext for authentication
5. **Role-Based Access Control**: Implementing URL and method-level security
6. **Stateless Authentication**: Benefits of token-based vs session-based auth
7. **Microservices Security**: Securing inter-service communication with shared secrets
8. **Audit & Compliance**: Logging authentication events for security monitoring

---

## 🔗 Service URLs

### Development Environment
- **Login Service**: http://localhost:8081/api/auth
- **Customer Service**: http://localhost:8083/api/customer
- **Product Pricing Service**: http://localhost:8084/api/products
- **FD Calculator Service**: http://localhost:8085/api/calculator

### Swagger Documentation
- **Login Swagger**: http://localhost:8081/api/auth/swagger-ui/index.html
- **Customer Swagger**: http://localhost:8083/api/customer/swagger-ui/index.html
- **Product Swagger**: http://localhost:8084/api/products/swagger-ui/index.html
- **Calculator Swagger**: http://localhost:8085/api/calculator/swagger-ui/index.html

---

## 🎉 Lab L7 Status: COMPLETE

✅ All authentication and authorization components verified
✅ JWT token generation working correctly
✅ Token validation and SecurityContext integration functional
✅ Role-based access control enforced
✅ Admin and User roles tested successfully
✅ Complete end-to-end login flow validated
✅ Comprehensive documentation created

**Lab L7 is 100% complete and ready for testing!**

---

## 📞 Support

For issues or questions:
1. Check service logs in console/terminal windows
2. Verify all services are running (health endpoints)
3. Ensure MySQL database is accessible
4. Confirm JWT secret matches across all services
5. Review SecurityConfig and @PreAuthorize annotations
6. Test with Swagger UI for interactive debugging

---

*Document Created: November 5, 2025*  
*Lab Status: ✅ COMPLETE*  
*Testing Status: ✅ VERIFIED*
