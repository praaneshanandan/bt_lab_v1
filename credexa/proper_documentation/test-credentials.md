# TEST CREDENTIALS FOR CREDEXA

**Generated:** 2025-11-07 12:05:34
**Test Status:** 11/18 tests passed (61.11%)

---

## Admin User

- **Username:** admin
- **Password:** Admin@123
- **Email:** admin@credexa.com
- **Mobile:** 9999999999
- **Role:** ROLE_ADMIN
- **User ID:** 1
- **Token:** `eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX0FETUlOIl0sInN1YiI6ImFkbWluIiwiaWF0IjoxNzYyNDk3MzMxLCJleHAiOjE3NjI1MDA5MzF9.t-lVvJ-YjDBjr3A4Qr4NuZZjG9fGHUx2hqIUMmKPXKo3kSedgsNtnyNjI4IrTdk15l1a8UinOKP02GLaoW3KfA`

---

## Customer 1

- **Username:** customer1
- **Password:** Pass1234
- **Email:** customer1@test.com
- **Mobile:** 9876543210
- **Role:** ROLE_CUSTOMER
- **Customer Profile ID:** 0
- **Token:** `eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX0NVU1RPTUVSIl0sInN1YiI6ImN1c3RvbWVyMSIsImlhdCI6MTc2MjQ5NzMzMiwiZXhwIjoxNzYyNTAwOTMyfQ.mtBgzzcFr9wGwcAqeNX_XVUiZhbID5djboMs3rKNNlDGCq4cGehJjFBsX7MOhaRuK5AjxfuJ4n_cU-TO-ug_eQ`

**Profile Details:**
- Full Name: John Doe (Updated to John Doe Updated)
- PAN: ABCDE1234F
- Aadhar: 123456789012
- DOB: 1990-01-15
- Gender: MALE
- Classification: PREMIUM (upgraded from REGULAR)
- City: Mumbai, Maharashtra
- Account: ACC1234567890, IFSC: HDFC0001234

---

## Customer 2

- **Username:** customer2
- **Password:** Pass1234
- **Email:** customer2@test.com
- **Mobile:** 9876543211
- **Role:** ROLE_CUSTOMER
- **Customer Profile ID:** 0
- **Token:** `eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX0NVU1RPTUVSIl0sInN1YiI6ImN1c3RvbWVyMiIsImlhdCI6MTc2MjQ5NzMzMiwiZXhwIjoxNzYyNTAwOTMyfQ.762vkHWgpGYblqFvzxz5ONQH7zeQNg5d6zYTsTC9VQ2e_2c40TvYIYCN4xZEF8liHCombjBsNxzEPJE_ZT4C7Q` *(logged out during testing)*
- **Status:** LOGGED OUT

**Profile Details:**
- Full Name: Jane Smith
- PAN: FGHIJ5678K
- Aadhar: 987654321098
- DOB: 1985-05-20
- Gender: FEMALE
- Classification: PREMIUM
- City: Delhi, Delhi
- Account: ACC9876543210, IFSC: ICIC0005678

---

## Manager 1

- **Username:** manager1
- **Password:** Pass1234
- **Email:** manager1@test.com
- **Mobile:** 9876543212
- **Role:** ROLE_CUSTOMER (registered, not yet upgraded to MANAGER)
- **Token:** `eyJhbGciOiJIUzUxMiJ9.eyJyb2xlcyI6WyJST0xFX0NVU1RPTUVSIl0sInN1YiI6Im1hbmFnZXIxIiwiaWF0IjoxNzYyNDk3MzMzLCJleHAiOjE3NjI1MDA5MzN9.fchHosSk-CSXEQZOh2XwBe9jNkyZnODOYO-jnywqpU9rpTHWIMwAC7QMb4yr0_ESGstuL4Euh8SKU35QKLyEpA`

**Note:** To test MANAGER role, this user needs to be manually assigned ROLE_MANAGER in the database.

---

## SQL Commands to Upgrade manager1 to MANAGER Role

```sql
-- Check current roles
SELECT u.id, u.username, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1';

-- Get role IDs
SELECT id, name FROM roles;

-- Add MANAGER role to manager1 (assuming ROLE_MANAGER has id=2)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'manager1' AND r.name = 'ROLE_MANAGER';

-- Remove CUSTOMER role if needed
DELETE ur FROM user_roles ur
JOIN users u ON ur.user_id = u.id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1' AND r.name = 'ROLE_CUSTOMER';

-- Verify
SELECT u.id, u.username, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1';
```

---

## Quick Test Commands

### Login as Admin
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmailOrMobile":"admin","password":"Admin@123"}'
```

### Login as Customer1
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmailOrMobile":"customer1","password":"Pass1234"}'
```

### Get All Customers (Admin)
```bash
curl -X GET http://localhost:8083/api/customer/all \
  -H "Authorization: Bearer <admin-token>"
```

---

**End of Test Credentials**
