@echo off
REM Test script for Login API Registration Endpoint
REM This script tests the user registration functionality

echo.
echo ===================================================================
echo Lab L1: User Registration API - Test Suite
echo ===================================================================
echo.

REM Base URL
set BASE_URL=http://localhost:8081/api/auth

echo [1] Testing Health Check
echo.
curl -X GET %BASE_URL%/health -H "Content-Type: application/json"
echo.
echo.

echo [2] Testing Registration - Successful Registration
echo.
curl -X POST %BASE_URL%/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"john.doe\",\"password\":\"SecurePass@123\",\"email\":\"john.doe@example.com\",\"mobileNumber\":\"9876543210\",\"preferredLanguage\":\"en\",\"preferredCurrency\":\"USD\"}"
echo.
echo.

echo [3] Testing Registration - Duplicate Username
echo.
curl -X POST %BASE_URL%/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"john.doe\",\"password\":\"DifferentPass@123\",\"email\":\"another.john@example.com\",\"mobileNumber\":\"9876543211\",\"preferredLanguage\":\"en\",\"preferredCurrency\":\"USD\"}"
echo.
echo.

echo [4] Testing Registration - Duplicate Email
echo.
curl -X POST %BASE_URL%/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"jane.doe\",\"password\":\"SecurePass@123\",\"email\":\"john.doe@example.com\",\"mobileNumber\":\"9876543212\",\"preferredLanguage\":\"en\",\"preferredCurrency\":\"USD\"}"
echo.
echo.

echo [5] Testing Registration - Missing Required Field
echo.
curl -X POST %BASE_URL%/register ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"mike.smith\",\"email\":\"mike@example.com\"}"
echo.
echo.

echo [6] Testing Login - Successful Login
echo.
curl -X POST %BASE_URL%/login ^
  -H "Content-Type: application/json" ^
  -d "{\"usernameOrEmailOrMobile\":\"john.doe\",\"password\":\"SecurePass@123\"}"
echo.
echo.

echo [7] Testing Login - Admin User
echo.
curl -X POST %BASE_URL%/login ^
  -H "Content-Type: application/json" ^
  -d "{\"usernameOrEmailOrMobile\":\"admin\",\"password\":\"Admin@123\"}"
echo.
echo.

echo [8] Testing Bank Configuration
echo.
curl -X GET %BASE_URL%/bank-config -H "Content-Type: application/json"
echo.
echo.

echo ===================================================================
echo Test Suite Complete!
echo ===================================================================
echo.
