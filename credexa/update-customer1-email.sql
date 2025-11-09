-- Update customer1 email to receive test emails
-- Run this in MySQL command line or MySQL Workbench

USE customer_db;

-- Check current email
SELECT id, username, email, full_name 
FROM customers 
WHERE username = 'customer1';

-- Update email to Gmail account for testing
UPDATE customers 
SET email = 'credexaservice.bt@gmail.com' 
WHERE username = 'customer1';

-- Verify the update
SELECT id, username, email, full_name 
FROM customers 
WHERE username = 'customer1';

-- Show the change
SELECT CONCAT('✅ Email updated successfully to: ', email) as result
FROM customers 
WHERE username = 'customer1';
