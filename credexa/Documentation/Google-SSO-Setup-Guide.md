# Google SSO Integration Setup Guide

## Overview

This document provides step-by-step instructions to complete the Google Single Sign-On (SSO) integration for the Credexa Fixed Deposits Banking Application.

## Status

✅ **Backend Implementation**: Complete
✅ **Frontend Implementation**: Complete
⚠️ **Google OAuth Credentials**: Needs configuration

---

## Step 1: Get Google OAuth 2.0 Credentials

### 1.1 Go to Google Cloud Console

Visit: https://console.cloud.google.com/

### 1.2 Create or Select a Project

- Click on the project dropdown in the top navigation
- Click "New Project" or select an existing project
- Project name: `Credexa Banking App` (or your preferred name)

### 1.3 Enable Google+ API

- Navigate to **APIs & Services** → **Library**
- Search for "Google+ API"
- Click "Enable"

### 1.4 Configure OAuth Consent Screen

1. Navigate to **APIs & Services** → **OAuth consent screen**
2. Select **External** user type (for testing)
3. Click **Create**
4. Fill in the required fields:
   - App name: `Credexa Banking`
   - User support email: Your email
   - Developer contact information: Your email
5. Click **Save and Continue**
6. **Scopes**: Click "Add or Remove Scopes"
   - Select: `email`, `profile`, `openid`
7. Click **Save and Continue**
8. **Test users** (for External type):
   - Add your Google email addresses for testing
9. Click **Save and Continue**

### 1.5 Create OAuth 2.0 Credentials

1. Navigate to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth 2.0 Client ID**
3. Application type: **Web application**
4. Name: `Credexa Web Client`
5. **Authorized JavaScript origins**:
   - `http://localhost:5173` (Frontend)
   - `http://localhost:8080` (Gateway)
   - `http://localhost:8081` (Login Service)
6. **Authorized redirect URIs**:
   - `http://localhost:8081/api/auth/login/oauth2/code/google`
   - `http://localhost:8080/login/oauth2/code/google`
7. Click **Create**
8. **IMPORTANT**: Copy your **Client ID** and **Client Secret**

---

## Step 2: Configure Backend

### 2.1 Update application.yml

Open: `login-service/src/main/resources/application.yml`

Find the section:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID:YOUR_GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET:YOUR_GOOGLE_CLIENT_SECRET}
```

**Replace** `YOUR_GOOGLE_CLIENT_ID` and `YOUR_GOOGLE_CLIENT_SECRET` with your actual credentials from Step 1.5.

### 2.2 Using Environment Variables (Recommended for Production)

Instead of hardcoding, set environment variables:

**Windows PowerShell**:

```powershell
$env:GOOGLE_CLIENT_ID = "your-client-id-here"
$env:GOOGLE_CLIENT_SECRET = "your-client-secret-here"
```

**Windows CMD**:

```cmd
set GOOGLE_CLIENT_ID=your-client-id-here
set GOOGLE_CLIENT_SECRET=your-client-secret-here
```

**Linux/Mac**:

```bash
export GOOGLE_CLIENT_ID="your-client-id-here"
export GOOGLE_CLIENT_SECRET="your-client-secret-here"
```

---

## Step 3: Restart Services

### 3.1 Stop Login Service

- If running in terminal: Press `Ctrl+C`
- Or stop the process

### 3.2 Build and Start Login Service

```powershell
cd c:\Users\jaina\Downloads\finalbt\bt_lab_v1\credexa\login-service
mvn clean install -DskipTests
mvn spring-boot:run
```

### 3.3 Verify Login Service is Running

Check the console output for:

```
Started LoginServiceApplication in X seconds
```

---

## Step 4: Test SSO Integration

### 4.1 Access the Application

Open browser: http://localhost:5173

### 4.2 Click "Continue with Google"

- You should see a Google OAuth consent screen
- Select your Google account
- Grant permissions (email, profile)

### 4.3 Verify Successful Login

After authentication:

- You should be redirected to `http://localhost:5173/oauth2/redirect`
- Then automatically redirected to the dashboard
- Check if your username appears in the top-right

### 4.4 Verify User Created

The SSO will automatically create a user account:

- Username: Based on your email (part before @)
- Email: Your Google email
- Role: ROLE_CUSTOMER (by default)
- Active: true

Check MySQL database:

```sql
USE login_db;
SELECT * FROM users WHERE email = 'your-google-email@gmail.com';
```

---

## How It Works

### Authentication Flow

1. **User clicks "Continue with Google"**

   - Frontend redirects to: `http://localhost:8081/api/auth/oauth2/authorization/google`

2. **Google OAuth Authorization**

   - User is redirected to Google's login page
   - User authenticates with Google
   - Google redirects back to: `http://localhost:8081/api/auth/login/oauth2/code/google`

3. **Backend Processes OAuth Response**

   - `OAuth2LoginSuccessHandler` is triggered
   - Extracts user info (email, name) from Google
   - Checks if user exists in database by email
   - If not exists, creates new user account
   - Generates JWT token

4. **Frontend Receives Token**
   - Backend redirects to: `http://localhost:5173/oauth2/redirect?token=...&userId=...&username=...&role=...`
   - Frontend stores token in localStorage
   - User is logged in and redirected to dashboard

### Hybrid Authentication

- **Username/Password**: Still works as before
- **Google SSO**: New alternative login method
- Both methods share the same user database and JWT tokens

---

## Security Features

### ✅ Automatic Account Creation

- New users are automatically registered on first SSO login
- Default role: ROLE_CUSTOMER
- Password field: `OAUTH2_USER_NO_PASSWORD` (not used for SSO)

### ✅ Email Verification

- Google verifies email authenticity
- No need for manual email verification

### ✅ Token-Based Authentication

- SSO generates same JWT tokens as regular login
- Consistent authentication across all microservices

### ✅ Role-Based Access Control

- SSO users get ROLE_CUSTOMER by default
- Admins can upgrade user roles in database if needed

---

## Troubleshooting

### Error: "redirect_uri_mismatch"

**Solution**: Make sure redirect URI in Google Cloud Console exactly matches:

- `http://localhost:8081/api/auth/login/oauth2/code/google`

### Error: "Access blocked: This app's request is invalid"

**Solution**:

- Enable Google+ API in Google Cloud Console
- Configure OAuth consent screen properly
- Add test users if using "External" user type

### Error: "OAuth2 login failed. Missing required parameters"

**Solution**:

- Check backend logs for errors
- Verify Google Client ID and Secret are correct
- Ensure redirect URI is correct

### Users Can't Login with SSO

**Solution**:

- Check if Login Service is running on port 8081
- Verify network connectivity
- Check browser console for JavaScript errors
- Check backend logs: `login-service/logs/`

### SSO User Can't Access Features

**Solution**:

- Verify user was created in database
- Check user's role assignment
- Ensure JWT token was generated correctly

---

## Configuration Reference

### Backend Files Modified:

1. ✅ `login-service/pom.xml` - Added OAuth2 client dependency
2. ✅ `login-service/src/main/resources/application.yml` - OAuth2 configuration
3. ✅ `login-service/.../SecurityConfig.java` - Enabled OAuth2 login
4. ✅ `login-service/.../OAuth2LoginSuccessHandler.java` - SSO success handler

### Frontend Files Modified:

1. ✅ `gateway/credexa-ui/src/App.tsx` - Added OAuth2 redirect route
2. ✅ `gateway/credexa-ui/src/pages/Login.tsx` - Added Google Sign-In button
3. ✅ `gateway/credexa-ui/src/pages/OAuth2Redirect.tsx` - OAuth2 callback handler

---

## Testing Checklist

### ✅ Before Testing:

- [ ] Google OAuth credentials configured in application.yml
- [ ] Login Service restarted with new configuration
- [ ] Frontend running on port 5173
- [ ] Gateway running on port 8080

### ✅ SSO Login Test:

- [ ] Click "Continue with Google" button
- [ ] Google OAuth consent screen appears
- [ ] Select Google account successfully
- [ ] Redirected to dashboard after authentication
- [ ] Username appears in top-right corner
- [ ] User data stored in `login_db.users` table

### ✅ Regular Login Still Works:

- [ ] Can still login with username/password
- [ ] Existing users not affected
- [ ] Default admin/manager accounts work

### ✅ Role-Based Access:

- [ ] SSO users see Customer Dashboard
- [ ] SSO users can access "My 360° View"
- [ ] SSO users cannot see "Customers" menu

---

## Next Steps

### Optional Enhancements:

1. **Add More SSO Providers**:

   - Microsoft Azure AD
   - GitHub OAuth
   - Facebook Login

2. **Link SSO to Existing Accounts**:

   - Allow users to link Google account to existing username/password account

3. **Admin Role Assignment**:

   - Create UI for admins to change user roles
   - Allow promoting SSO users to MANAGER or ADMIN

4. **Profile Completion**:

   - Redirect SSO users to complete profile (address, PAN, Aadhar)
   - Required before creating FD accounts

5. **Production Deployment**:
   - Use environment variables for credentials
   - Change OAuth consent screen to "Internal" for company users
   - Add production redirect URIs

---

## Support

If you encounter any issues:

1. Check the backend logs in Login Service console
2. Check browser console (F12) for JavaScript errors
3. Verify Google Cloud Console configuration
4. Ensure all services are running correctly

---

**Author**: GitHub Copilot  
**Date**: January 2025  
**Status**: ✅ Implementation Complete - Awaiting Google OAuth Credentials
