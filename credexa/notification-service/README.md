# Notification Service

Centralized notification service for Credexa Banking that consumes Kafka events and sends email notifications.

## Features

✅ **Kafka Event Consumers**
- Account Created Events (`account-created` topic)
- Transaction Events (`account-transaction` topic)  
- Alert Events (`customer-alert` topic)

✅ **Email Notifications**
- Beautiful HTML email templates (Thymeleaf)
- Gmail SMTP integration
- Async email sending
- Professional formatting with account details

✅ **Multi-Service Support**
- Account Service notifications
- Customer Service notifications (ready)
- Product Service notifications (ready)

## Setup

### 1. Gmail App Password Setup

**Important:** You need a Gmail **App Password**, not your regular password!

**Steps:**
1. Go to your Google Account: https://myaccount.google.com/
2. Click **Security** → **2-Step Verification** (enable if not already)
3. Scroll down to **App passwords**
4. Click **App passwords**
5. Select **Mail** and **Windows Computer**
6. Click **Generate**
7. **Copy the 16-character password** (e.g., `abcd efgh ijkl mnop`)

### 2. Configure Environment Variables

Create a file `.env` or set environment variables:

```bash
# Windows CMD
set GMAIL_USERNAME=your-email@gmail.com
set GMAIL_APP_PASSWORD=your-16-char-app-password
set EMAIL_FROM_ADDRESS=noreply@credexa.com

# Windows PowerShell
$env:GMAIL_USERNAME="your-email@gmail.com"
$env:GMAIL_APP_PASSWORD="your-16-char-app-password"
$env:EMAIL_FROM_ADDRESS="noreply@credexa.com"
```

**Or edit `application.yml` directly:**
```yaml
spring:
  mail:
    username: your-email@gmail.com
    password: your-app-password
```

### 3. Start Services

**Terminal 1: Start Kafka**
```cmd
cd C:\kafka
bin\windows\kafka-server-start.bat config\kraft\server.properties
```

**Terminal 2: Start Account Service**
```cmd
cd C:\Users\dhruv\Coding\bt_lab_v1\credexa\account-service
mvn spring-boot:run
```

**Terminal 3: Start Notification Service**
```cmd
cd C:\Users\dhruv\Coding\bt_lab_v1\credexa\notification-service
mvn spring-boot:run
```

## Testing

### Test 1: Create Account and Receive Email

1. Go to: http://localhost:8093/swagger-ui.html
2. Login as admin
3. POST `/api/accounts/accounts/create/default`

```json
{
  "customerId": 1,
  "productCode": "FD-STD-001",
  "principalAmount": 1000000,
  "termMonths": 12,
  "effectiveDate": "2025-11-09",
  "accountName": "Email Test Account",
  "branchCode": "BR001",
  "branchName": "Main Branch"
}
```

**Expected Flow:**
1. ✅ Account created in account-service
2. ✅ Kafka event published to `account-created` topic
3. ✅ Notification-service consumes event
4. ✅ Beautiful HTML email sent to customer

**Check Logs:**
```
account-service logs:
✅ Account created successfully: FD-...
✅ Published AccountCreatedEvent for account: FD-...

notification-service logs:
📩 Received AccountCreatedEvent from Kafka
✅ Account creation email queued for: customer1@test.com
✅ Email sent successfully to customer1@test.com
```

### Test 2: View Kafka Events

```cmd
cd C:\kafka
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic account-created --from-beginning
```

## Email Templates

### account-created.html
Beautiful email with:
- Account number
- Principal amount
- Interest rate
- Term months
- Created date
- Professional styling

### alert.html
Alert notifications with:
- Severity levels (INFO, WARNING, CRITICAL)
- Color-coded alerts
- Account details
- Timestamp

## Configuration

**application.yml**
```yaml
server:
  port: 8096

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${GMAIL_USERNAME}
    password: ${GMAIL_APP_PASSWORD}
```

## Ports

- **8096** - Notification Service
- **8093** - Account Service
- **9092** - Kafka

## Troubleshooting

### Email Not Sending

**Issue:** "Authentication failed"
**Solution:** Make sure you're using **App Password**, not regular password

**Issue:** "Connection timeout"
**Solution:** Check firewall, allow port 587

### Kafka Connection Failed

**Issue:** "Connection refused localhost:9092"
**Solution:** Make sure Kafka is running:
```cmd
netstat -ano | findstr :9092
```

### No Events Received

**Issue:** Notification service not receiving events
**Solution:** Check Kafka topics exist:
```cmd
bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

## Architecture

```
Account Service
    ↓
Publishes Event
    ↓
Kafka Topic (account-created)
    ↓
Notification Service (Consumer)
    ↓
Email Service (JavaMailSender)
    ↓
Gmail SMTP
    ↓
Customer Email Inbox
```

## Next Steps

1. ✅ Test account creation email
2. ⏳ Add customer service events
3. ⏳ Add product service events
4. ⏳ Add SMS notifications
5. ⏳ Add push notifications

## Summary

🎉 **Notification service is ready!**

- Consumes Kafka events
- Sends beautiful HTML emails via Gmail
- Supports multiple event types
- Easy to extend for other services
