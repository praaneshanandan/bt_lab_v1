# Quick Start Guide - Lab L8 Integration Testing

## Prerequisites
1. ✅ MySQL running on port 3306
2. ✅ Java 17+ installed
3. ✅ Maven installed (or use mvnw wrapper)
4. ✅ All databases created (login_db, customer_db, product_db, calculator_db)

## Step 1: Start All Services

### Option A: Automatic (Batch Script)
```batch
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
start-all-services.bat
```

**Note**: Wait 60-90 seconds for all services to fully start.

---

### Option B: Manual (If Maven is in PATH)

Open 4 separate PowerShell terminals:

**Terminal 1 - Login Service**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa\login-service"
mvn spring-boot:run
```

**Terminal 2 - Customer Service**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa\customer-service"
mvn spring-boot:run
```

**Terminal 3 - Product Service**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa\product-pricing-service"
mvn spring-boot:run
```

**Terminal 4 - Calculator Service**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa\fd-calculator-service"
mvn spring-boot:run
```

---

### Option C: Using Maven Wrapper (if Maven not in PATH)

**Terminal 1**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
cd login-service
..\mvnw.cmd spring-boot:run
```

**Terminal 2**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
cd customer-service
..\mvnw.cmd spring-boot:run
```

**Terminal 3**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
cd product-pricing-service
..\mvnw.cmd spring-boot:run
```

**Terminal 4**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
cd fd-calculator-service
..\mvnw.cmd spring-boot:run
```

---

## Step 2: Verify Services are Running

```powershell
# Check all services
@(8081, 8083, 8084, 8085) | ForEach-Object {
    $port = $_
    $services = @{8081="Login";8083="Customer";8084="Product";8085="Calculator"}
    $paths = @{8081="api/auth/health";8083="api/customer/health";8084="api/products/health";8085="api/calculator/health"}
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:$port/$($paths[$port])" -Method GET -UseBasicParsing -TimeoutSec 5
        Write-Host "✓ $($services[$port]) Service ($port) - RUNNING" -ForegroundColor Green
    } catch {
        Write-Host "✗ $($services[$port]) Service ($port) - DOWN" -ForegroundColor Red
    }
}
```

Expected output:
```
✓ Login Service (8081) - RUNNING
✓ Customer Service (8083) - RUNNING
✓ Product Service (8084) - RUNNING
✓ Calculator Service (8085) - RUNNING
```

---

## Step 3: Run Integration Tests

```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
.\Lab-L8-Integration-Test.ps1
```

This will test:
- ✅ User Registration & Login
- ✅ Customer Profile Management
- ✅ Product Service Integration
- ✅ FD Calculator (Standalone)
- ✅ FD Calculator (Product-based)
- ✅ Security (Invalid Access Scenarios)
- ✅ Admin Privileges

---

## Step 4: Interactive Testing with Swagger

### 4.1 Login and Get JWT Token
1. Open: http://localhost:8081/api/auth/swagger-ui/index.html
2. Find **POST /login** endpoint
3. Click "Try it out"
4. Enter credentials:
   ```json
   {
     "usernameOrEmailOrMobile": "admin",
     "password": "Admin@123"
   }
   ```
5. Click "Execute"
6. Copy the `token` value from the response

### 4.2 Use Token in Other Services
1. Open any service Swagger (e.g., http://localhost:8083/api/customer/swagger-ui/index.html)
2. Click the **Authorize** button (top right, lock icon)
3. Enter: `Bearer <your-token-here>`
4. Click "Authorize" then "Close"
5. Now you can test all endpoints!

### 4.3 Test Customer Endpoints
- **GET /profile** - Your own profile (any role)
- **GET /all** - All customers (ADMIN/CUSTOMER_MANAGER only)
- **POST /** - Create customer profile
- **PUT /{id}** - Update customer profile

### 4.4 Test Product Endpoints
- **GET /active** - All active products
- **GET /code/{code}** - Get product by code (e.g., FD-STD-001)
- **POST /** - Create new product (ADMIN only)

### 4.5 Test Calculator Endpoints
- **POST /calculate/standalone** - Calculate without product
- **POST /fd/calculate** - Calculate using product code
- **POST /calculate/product-based** - Calculate with customer categories

---

## Quick Manual Test (PowerShell)

### Test 1: Login as Admin
```powershell
$loginBody = @{
    usernameOrEmailOrMobile = "admin"
    password = "Admin@123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
    -Method POST -Body $loginBody -ContentType "application/json"

$token = $response.data.token
Write-Host "JWT Token: $token" -ForegroundColor Green
```

### Test 2: Get Customer Profile
```powershell
$headers = @{Authorization = "Bearer $token"}
$profile = Invoke-RestMethod -Uri "http://localhost:8083/api/customer/profile" `
    -Method GET -Headers $headers

$profile | ConvertTo-Json
```

### Test 3: Get Active Products
```powershell
$products = Invoke-RestMethod -Uri "http://localhost:8084/api/products/active" `
    -Method GET -Headers $headers

Write-Host "Found $($products.Count) active products" -ForegroundColor Cyan
$products | Select-Object productCode, productName, interestRate | Format-Table
```

### Test 4: Calculate FD
```powershell
$calcBody = @{
    productCode = "FD-STD-001"
    principalAmount = 100000
    tenureInMonths = 12
} | ConvertTo-Json

$calcResult = Invoke-RestMethod -Uri "http://localhost:8085/api/calculator/fd/calculate" `
    -Method POST -Body $calcBody -Headers $headers -ContentType "application/json"

Write-Host "Principal: ₹$($calcResult.principalAmount)" -ForegroundColor Cyan
Write-Host "Rate: $($calcResult.interestRate)%" -ForegroundColor Cyan
Write-Host "Interest: ₹$($calcResult.interestAmount)" -ForegroundColor Green
Write-Host "Maturity: ₹$($calcResult.maturityAmount)" -ForegroundColor Green
```

---

## Service URLs

### Health Checks
- Login: http://localhost:8081/api/auth/health
- Customer: http://localhost:8083/api/customer/health
- Product: http://localhost:8084/api/products/health
- Calculator: http://localhost:8085/api/calculator/health

### Swagger UIs
- Login: http://localhost:8081/api/auth/swagger-ui/index.html
- Customer: http://localhost:8083/api/customer/swagger-ui/index.html
- Product: http://localhost:8084/api/products/swagger-ui/index.html
- Calculator: http://localhost:8085/api/calculator/swagger-ui/index.html

---

## Troubleshooting

### Issue: Services won't start
**Solution**: 
- Check MySQL is running: `Get-Service MySQL*`
- Check ports not in use: `netstat -ano | findstr "8081 8083 8084 8085"`
- Verify Java installed: `java -version`

### Issue: "mvn: command not found"
**Solution**: 
- Use Maven wrapper: `..\mvnw.cmd spring-boot:run`
- Or install Maven and add to PATH

### Issue: Database connection error
**Solution**:
- Verify MySQL credentials in `application.yml`
- Ensure databases exist: login_db, customer_db, product_db, calculator_db
- Test connection: `mysql -u root -p`

### Issue: JWT token rejected (401)
**Solution**:
- Check token hasn't expired (1 hour validity)
- Verify JWT secret matches across all services
- Ensure format: `Authorization: Bearer <token>` (with space after Bearer)

### Issue: Calculator returns 500 error
**Solution**:
- Verify Product Service is running
- Check product code exists: GET http://localhost:8084/api/products/code/FD-STD-001
- Review calculator service logs

---

## Default Credentials

### Admin User
- **Username**: admin
- **Password**: Admin@123
- **Roles**: ROLE_ADMIN
- **Permissions**: Full access to all endpoints

### Test Users (if needed)
You can register new users via:
```
POST http://localhost:8081/api/auth/register
```

---

## Documentation

### Lab Documentation
- **Lab L5**: `Documentation/Lab-L5-Implementation-Status.md`
- **Lab L6**: `Documentation/Lab-L6-Implementation-Status.md`
- **Lab L7**: `Documentation/Lab-L7-Implementation-Status.md`
- **Lab L8**: `Documentation/Lab-L8-Implementation-Status.md`

### Test Scripts
- **Integration Test**: `Lab-L8-Integration-Test.ps1`

---

## Next Steps

1. ✅ Start all 4 services
2. ✅ Run health checks to verify services are up
3. ✅ Execute integration test script
4. ✅ Explore Swagger UIs for interactive testing
5. ✅ Review service logs for event tracking
6. ✅ Read Lab-L8-Implementation-Status.md for detailed documentation

---

**Lab L8 Setup Complete! 🎉**

All backend services are implemented and ready for testing!
