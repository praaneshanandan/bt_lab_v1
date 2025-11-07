# ASSIGN MANAGER ROLE TO manager1

**Date:** 2025-11-07
**Task:** Change manager1 from ROLE_CUSTOMER to ROLE_MANAGER

---

## SQL Commands

### Step 1: Connect to MySQL

```bash
mysql -u root -p
```

Enter your MySQL root password.

### Step 2: Use the login_db database

```sql
USE login_db;
```

### Step 3: Check current roles for manager1

```sql
SELECT u.id, u.username, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1';
```

**Expected output:**
```
+----+-----------+---------------+
| id | username  | role          |
+----+-----------+---------------+
|  4 | manager1  | ROLE_CUSTOMER |
+----+-----------+---------------+
```

### Step 4: Check available roles and their IDs

```sql
SELECT id, name FROM roles ORDER BY id;
```

**Expected output:**
```
+----+---------------+
| id | name          |
+----+---------------+
|  1 | ROLE_ADMIN    |
|  2 | ROLE_MANAGER  |
|  3 | ROLE_CUSTOMER |
+----+---------------+
```

### Step 5: Add ROLE_MANAGER to manager1

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
CROSS JOIN roles r
WHERE u.username = 'manager1' AND r.name = 'ROLE_MANAGER';
```

**Expected output:**
```
Query OK, 1 row affected (0.XX sec)
```

### Step 6: Remove ROLE_CUSTOMER from manager1

```sql
DELETE ur FROM user_roles ur
JOIN users u ON ur.user_id = u.id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1' AND r.name = 'ROLE_CUSTOMER';
```

**Expected output:**
```
Query OK, 1 row affected (0.XX sec)
```

### Step 7: Verify the change

```sql
SELECT u.id, u.username, r.name as role
FROM users u
JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1';
```

**Expected output:**
```
+----+-----------+---------------+
| id | username  | role          |
+----+-----------+---------------+
|  4 | manager1  | ROLE_MANAGER  |
+----+-----------+---------------+
```

### Step 8: Exit MySQL

```sql
EXIT;
```

---

## After Role Assignment

### Test the new MANAGER role:

1. **Login as manager1** to get a new JWT token with MANAGER role:
   ```
   POST http://localhost:8081/api/auth/login
   Content-Type: application/json

   {
     "usernameOrEmailOrMobile": "manager1",
     "password": "Pass1234"
   }
   ```

2. **Save the new token** - The response should now include `"roles": ["ROLE_MANAGER"]`

3. **Test MANAGER permissions:**
   - ✅ View all customers: `GET /api/customer/all` (should work)
   - ✅ Create customer profiles for any user (should work)
   - ✅ View any customer's 360° view (should work)
   - ✅ View any customer profile (should work)
   - ❌ Unlock accounts: `POST /api/auth/admin/unlock-account/{username}` (should fail - Admin only)

---

## Quick One-Liner Commands

If you prefer to run all commands at once:

```sql
USE login_db;

-- Add MANAGER role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.username = 'manager1' AND r.name = 'ROLE_MANAGER';

-- Remove CUSTOMER role
DELETE ur FROM user_roles ur
JOIN users u ON ur.user_id = u.id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1' AND r.name = 'ROLE_CUSTOMER';

-- Verify
SELECT u.id, u.username, r.name as role
FROM users u JOIN user_roles ur ON u.id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE u.username = 'manager1';
```

---

## Note about /bank-config

You mentioned that `/bank-config` is accessible after logout. This is **CORRECT and by design**:

- `/bank-config` is a **public endpoint** (see SecurityConfig line 55 in login-service)
- It's intentionally accessible without authentication
- This allows the frontend/gateway to display bank information before users log in
- Public endpoints in login-service include:
  - `/register` - For new user signup
  - `/login` - For authentication
  - `/health` - For service monitoring
  - `/bank-config` - For displaying bank information
  - `/validate-token` - For inter-service token validation
  - `/user/**` - For inter-service user lookup

So the behavior you observed is expected and correct!

---

**End of Guide**
