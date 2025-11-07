# UI Improvements Testing Checklist

## Test Environment Setup

### Prerequisites
- [ ] All backend services running (Customer Service, Login Service, Gateway)
- [ ] Frontend dev server running (`npm run dev`)
- [ ] Test data populated in database
- [ ] Multiple user accounts with different roles (Admin, Manager, Customer)
- [ ] Browser DevTools open (for console monitoring)

---

## Feature 1: Session Management & Auto-Logout

### Setup
- [ ] Login to the application
- [ ] Note the current time

### Test Cases

#### TC1.1: Idle Timeout Triggers
- [ ] Stop all mouse/keyboard activity
- [ ] Wait 4 minutes 30 seconds
- [ ] **Expected**: Warning modal should appear with countdown
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC1.2: Activity Resets Timer
- [ ] Trigger the warning modal (wait 4:30)
- [ ] Click "Continue Session"
- [ ] Wait 4 minutes 30 seconds again
- [ ] **Expected**: Warning modal appears again (timer was reset)
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC1.3: Countdown Display
- [ ] Trigger warning modal
- [ ] Observe countdown timer
- [ ] **Expected**: Counts down from 30 to 0 seconds
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC1.4: Manual Logout
- [ ] Trigger warning modal
- [ ] Click "Logout Now"
- [ ] **Expected**: Redirected to login page, localStorage cleared
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC1.5: Auto Logout
- [ ] Trigger warning modal
- [ ] Don't click anything for 30 seconds
- [ ] **Expected**: Automatically logged out after countdown
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC1.6: Token Expiration Detection
- [ ] Manually set token expiration to past time in localStorage
- [ ] Refresh page
- [ ] **Expected**: Automatically logged out
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Feature 2: Account Lockout Feedback

### Setup
- [ ] Logout if logged in
- [ ] On login page

### Test Cases

#### TC2.1: Failed Login Counter
- [ ] Enter incorrect credentials
- [ ] Submit form
- [ ] **Expected**: "4 attempts remaining" message appears
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC2.2: Counter Decrements
- [ ] Fail login 4 more times (total 5 failures)
- [ ] **Expected**: Counter shows "3, 2, 1, 0 attempts remaining"
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC2.3: Account Lockout Message
- [ ] After 5th failure
- [ ] **Expected**: "Account locked. Please contact support." message
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC2.4: Form Disabled When Locked
- [ ] After lockout
- [ ] Try to type in username/password fields
- [ ] **Expected**: Fields are disabled, submit button disabled
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Feature 3: Password Strength Indicator

### Setup
- [ ] Navigate to login page
- [ ] Click "Register" or switch to registration form

### Test Cases

#### TC3.1: Initial State (Empty Password)
- [ ] Password field is empty
- [ ] **Expected**: No strength indicator or shows "Weak" with 0 checks
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC3.2: Weak Password (0-1 checks)
- [ ] Enter "pass"
- [ ] **Expected**: Red bar, "Weak" label, only length check passed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC3.3: Medium Password (2-3 checks)
- [ ] Enter "Password1"
- [ ] **Expected**: Yellow bar, "Medium" label, 3-4 checks passed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC3.4: Good Password (4 checks)
- [ ] Enter "Password123"
- [ ] **Expected**: Blue bar, "Good" label, 4 checks passed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC3.5: Strong Password (5 checks)
- [ ] Enter "Password123!"
- [ ] **Expected**: Green bar, "Strong" label, all 5 checks passed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC3.6: Real-time Updates
- [ ] Type password character by character
- [ ] **Expected**: Indicator updates after each character
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC3.7: Checklist Accuracy
- [ ] Test each requirement:
  - [ ] 8+ characters: "12345678" ✅
  - [ ] Uppercase: "Password" ✅
  - [ ] Lowercase: "PASSWORD" ❌ then add lowercase
  - [ ] Number: "Password" ❌ then add "1"
  - [ ] Special char: "Password1" ❌ then add "!"
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Feature 4: Profile Completion Progress

### Setup
- [ ] Login as a customer
- [ ] Navigate to My Profile page

### Test Cases

#### TC4.1: Display on Non-Edit Mode
- [ ] View profile (not editing)
- [ ] **Expected**: ProfileCompletion component visible at top
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC4.2: Hide During Edit Mode
- [ ] Click "Edit Profile"
- [ ] **Expected**: ProfileCompletion component hidden
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC4.3: Percentage Calculation (0%)
- [ ] Create customer with only required fields
- [ ] **Expected**: Shows low percentage (20-40%)
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC4.4: Percentage Calculation (100%)
- [ ] Fill all profile fields completely
- [ ] **Expected**: Shows 100%, green color
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC4.5: Section Status Accuracy
- [ ] Check each section status:
  - [ ] Basic Info (name, DOB, gender)
  - [ ] Contact Info (email, mobile)
  - [ ] Identity Docs (PAN, Aadhar)
  - [ ] Address (all address fields)
  - [ ] Banking (account, IFSC)
- [ ] **Expected**: Correct checkmark/x for each section
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC4.6: Color Coding
- [ ] Test different completion levels:
  - [ ] < 60%: Orange
  - [ ] 60-99%: Blue
  - [ ] 100%: Green
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Feature 5: Role-Based Navigation

### Setup
- [ ] Create/have test accounts for each role:
  - Admin account
  - Manager account
  - Regular customer account

### Test Cases

#### TC5.1: Admin Access
- [ ] Login as Admin
- [ ] Check sidebar navigation
- [ ] **Expected**: "Customers" link visible
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC5.2: Manager Access
- [ ] Login as Manager
- [ ] Check sidebar navigation
- [ ] **Expected**: "Customers" link visible
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC5.3: Customer Access
- [ ] Login as Customer
- [ ] Check sidebar navigation
- [ ] **Expected**: "Customers" link NOT visible
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC5.4: Unauthorized Route Access
- [ ] Login as Customer
- [ ] Manually navigate to `/customers`
- [ ] **Expected**: Redirected to `/profile` with info toast
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC5.5: Token Role Validation
- [ ] Open DevTools Console
- [ ] Login and check token roles
- [ ] **Expected**: Roles array present in decoded token
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Feature 6: Customer 360° View Page

### Setup
- [ ] Login as Manager or Admin
- [ ] Navigate to Customers page
- [ ] Ensure test customer has complete data

### Test Cases

#### TC6.1: Navigation to 360° View
- [ ] Click "View 360°" button in Actions column
- [ ] **Expected**: Navigate to `/customers/{id}/360-view`
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.2: Customer Overview Section
- [ ] View customer overview card
- [ ] **Expected**: Name, username, ID, status badges visible
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.3: Contact Details Section
- [ ] View contact details
- [ ] **Expected**: Email, mobile, language, currency displayed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.4: Address Section
- [ ] View address information
- [ ] **Expected**: Full address with city, state, pincode, country
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.5: Banking Details Section
- [ ] View banking details
- [ ] **Expected**: Account number, IFSC code displayed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.6: Identity Documents Section
- [ ] View identity documents
- [ ] **Expected**: PAN and Aadhar numbers displayed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.7: Personal Information Section
- [ ] View personal information
- [ ] **Expected**: DOB formatted, age calculated, gender displayed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.8: Classification Card
- [ ] View classification information
- [ ] **Expected**: Type, rate percentage, benefit description
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.9: FD Account Summary
- [ ] View FD summary section
- [ ] **Expected**: 5 metrics displayed with proper formatting
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.10: Currency Formatting
- [ ] Check all currency amounts
- [ ] **Expected**: Indian Rupee format (₹1,23,456.78)
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.11: Back Navigation
- [ ] Click "Back to Customers" button
- [ ] **Expected**: Return to customers list
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC6.12: Missing Data Handling
- [ ] View customer with partial data
- [ ] **Expected**: Missing fields show "Not provided" or empty state
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Feature 7: Customer Search & Filters

### Setup
- [ ] Login as Manager or Admin
- [ ] Navigate to Customers page
- [ ] Ensure multiple customers with varied data exist

### Test Cases

#### TC7.1: Search by Full Name
- [ ] Enter customer name in search box
- [ ] **Expected**: Only matching customers displayed
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.2: Search by Email
- [ ] Enter email address
- [ ] **Expected**: Customer with that email appears
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.3: Search by Mobile Number
- [ ] Enter mobile number
- [ ] **Expected**: Customer with that mobile appears
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.4: Search by PAN Number
- [ ] Enter PAN number
- [ ] **Expected**: Customer with that PAN appears
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.5: Search by Aadhar Number
- [ ] Enter Aadhar number
- [ ] **Expected**: Customer with that Aadhar appears
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.6: Search by Username
- [ ] Enter username
- [ ] **Expected**: Customer with that username appears
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.7: Case-Insensitive Search
- [ ] Enter search term in different cases (UPPER, lower, MiXeD)
- [ ] **Expected**: Results same regardless of case
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.8: Classification Filter
- [ ] Select "Premium" from classification filter
- [ ] **Expected**: Only Premium customers shown
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.9: KYC Status Filter
- [ ] Select "Verified" from KYC status filter
- [ ] **Expected**: Only verified customers shown
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.10: Account Status Filter
- [ ] Select "Active" from status filter
- [ ] **Expected**: Only active customers shown
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.11: Combined Filters
- [ ] Apply search + classification + KYC + status filters
- [ ] **Expected**: Results match ALL criteria (AND logic)
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.12: Customer Count Display
- [ ] Apply various filters
- [ ] Check customer count in header
- [ ] **Expected**: Count matches displayed results
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.13: Active Filters Display
- [ ] Apply multiple filters
- [ ] Check active filters chips
- [ ] **Expected**: Chips show each active filter
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.14: Clear Filters Button
- [ ] Apply multiple filters
- [ ] Click "Clear Filters" button
- [ ] **Expected**: All filters reset to default, toast shown
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.15: Export to CSV
- [ ] Apply filters (optional)
- [ ] Click "Export CSV" button
- [ ] **Expected**: CSV file downloads with timestamp filename
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC7.16: CSV Content Verification
- [ ] Open downloaded CSV file
- [ ] **Expected**: 9 columns, correct headers, data matches filtered results
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Feature 8: Duplicate Detection Warnings

### Setup
- [ ] Login as Manager or Admin
- [ ] Navigate to Customers page
- [ ] Open "Add New Customer" dialog
- [ ] Note existing customer data (email, mobile, PAN, Aadhar)

### Test Cases

#### TC8.1: Email Duplicate Detection
- [ ] Enter existing customer's email
- [ ] **Expected**: Warning appears below email field
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.2: Mobile Duplicate Detection
- [ ] Enter existing customer's mobile number
- [ ] **Expected**: Warning appears below mobile field
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.3: PAN Duplicate Detection
- [ ] Enter existing customer's PAN number
- [ ] **Expected**: Warning appears below PAN field
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.4: Aadhar Duplicate Detection
- [ ] Enter existing customer's Aadhar number
- [ ] **Expected**: Warning appears below Aadhar field
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.5: Real-time Validation
- [ ] Type email character by character
- [ ] **Expected**: Warning appears immediately when match found
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.6: Warning Message Content
- [ ] Trigger duplicate warning
- [ ] Read warning message
- [ ] **Expected**: Shows field name, existing customer name, helpful suggestion
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.7: Multiple Duplicates
- [ ] Enter duplicates for email AND mobile
- [ ] **Expected**: Both warnings appear
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.8: Submit Button Disabled
- [ ] Have active duplicate warnings
- [ ] Check submit button
- [ ] **Expected**: "Create Customer" button disabled
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.9: Summary Alert Display
- [ ] Have active duplicate warnings
- [ ] Check bottom of form
- [ ] **Expected**: Red alert showing duplicate prevention message
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.10: Form Submission Prevention
- [ ] Have active duplicate warnings
- [ ] Try to submit form (if button somehow enabled)
- [ ] **Expected**: Error toast, form not submitted
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.11: Case-Insensitive Matching
- [ ] Enter duplicate email in different case
- [ ] **Expected**: Still detected as duplicate
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

#### TC8.12: Warning Clears When Fixed
- [ ] Trigger duplicate warning
- [ ] Change value to non-duplicate
- [ ] **Expected**: Warning disappears, submit enabled
- [ ] **Actual**: ___________________
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Cross-Feature Integration Tests

### ITC1: Session Timeout During Customer Creation
- [ ] Open customer creation dialog
- [ ] Fill half the form
- [ ] Wait for session timeout
- [ ] **Expected**: Session warning appears, form data preserved if continue
- [ ] **Status**: ✅ Pass / ❌ Fail

### ITC2: Role Change During Session
- [ ] Login as Manager
- [ ] Admin changes user role (backend)
- [ ] Wait for token expiration
- [ ] **Expected**: Logged out on next auth check
- [ ] **Status**: ✅ Pass / ❌ Fail

### ITC3: Search with Filters and Export
- [ ] Apply search term
- [ ] Apply multiple filters
- [ ] Export to CSV
- [ ] **Expected**: CSV contains only filtered results
- [ ] **Status**: ✅ Pass / ❌ Fail

### ITC4: Duplicate Detection with Auto-Complete
- [ ] Start typing existing customer's email
- [ ] Browser auto-complete suggests it
- [ ] Select auto-complete suggestion
- [ ] **Expected**: Duplicate warning appears
- [ ] **Status**: ✅ Pass / ❌ Fail

---

## Browser Compatibility Tests

### Chrome
- [ ] All features work
- [ ] No console errors
- [ ] UI renders correctly

### Firefox
- [ ] All features work
- [ ] No console errors
- [ ] UI renders correctly

### Safari
- [ ] All features work
- [ ] No console errors
- [ ] UI renders correctly

### Edge
- [ ] All features work
- [ ] No console errors
- [ ] UI renders correctly

---

## Responsive Design Tests

### Desktop (1920x1080)
- [ ] All components visible
- [ ] No layout issues
- [ ] Proper spacing

### Laptop (1366x768)
- [ ] All components visible
- [ ] No horizontal scroll
- [ ] Proper spacing

### Tablet (768x1024)
- [ ] Components adapt to width
- [ ] Navigation accessible
- [ ] Forms usable

### Mobile (375x667)
- [ ] Mobile-friendly layout
- [ ] Touch targets adequate
- [ ] No content cut off

---

## Accessibility Tests

### Keyboard Navigation
- [ ] All interactive elements focusable
- [ ] Tab order logical
- [ ] Enter/Space activate buttons

### Screen Reader
- [ ] Form labels read correctly
- [ ] Error messages announced
- [ ] Button purposes clear

### Color Contrast
- [ ] Text readable against backgrounds
- [ ] Status indicators have sufficient contrast
- [ ] Links distinguishable

---

## Performance Tests

### Page Load Time
- [ ] Initial load < 3 seconds
- [ ] Subsequent loads < 1 second

### Filter Performance
- [ ] 100 customers: instant
- [ ] 1000 customers: < 500ms
- [ ] 10000 customers: < 2s

### Duplicate Detection
- [ ] 100 customers: instant
- [ ] 1000 customers: < 100ms
- [ ] 10000 customers: < 500ms

---

## Test Summary

**Total Test Cases**: _____ / _____  
**Passed**: _____ ✅  
**Failed**: _____ ❌  
**Blocked**: _____ ⚠️  

**Critical Issues Found**: _____  
**Minor Issues Found**: _____  

**Overall Status**: ✅ Ready for Production / ❌ Needs Fixes / ⚠️ Needs Review

**Tested By**: _____________________  
**Date**: _____________________  
**Browser**: _____________________  
**OS**: _____________________  

**Notes**:
_____________________________________________________________
_____________________________________________________________
_____________________________________________________________
