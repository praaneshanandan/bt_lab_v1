@echo off
REM Test customer creation with curl

echo === Step 1: Login ===
curl -X POST http://localhost:8081/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"usernameOrEmailOrMobile\":\"customer1\",\"password\":\"Pass1234\"}" ^
  -s -o login-response.json

echo.
echo Login response saved to login-response.json

REM Extract token (this is tricky in batch, so we'll do it manually)
echo.
echo === Step 2: Please extract the token from login-response.json and paste it here ===
set /p TOKEN="Enter token: "

echo.
echo === Step 3: Create customer profile ===
curl -v -X POST http://localhost:8083/api/customer ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer %TOKEN%" ^
  -d "{\"fullName\":\"John Doe\",\"mobileNumber\":\"9876543210\",\"email\":\"customer1@test.com\",\"panNumber\":\"ABCDE1234F\",\"aadharNumber\":\"123456789012\",\"dateOfBirth\":\"1990-01-15\",\"gender\":\"MALE\",\"classification\":\"REGULAR\",\"addressLine1\":\"123 Main Street\",\"city\":\"Mumbai\",\"state\":\"Maharashtra\",\"pincode\":\"400001\",\"country\":\"India\",\"preferredLanguage\":\"en\",\"preferredCurrency\":\"INR\",\"emailNotifications\":true,\"smsNotifications\":true}"

echo.
echo.
pause
