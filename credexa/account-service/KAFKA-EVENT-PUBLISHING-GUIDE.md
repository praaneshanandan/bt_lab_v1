# Kafka Event Publishing - Implementation Guide

## Overview
Simple Kafka event publishing for account creation, transactions, and customer alerts. **Disabled by default** to prevent breaking existing functionality.

## Features Implemented

### 1. **Account Created Events**
When an FD account is created:
- ✅ `AccountCreatedEvent` published to `account-created` topic
- ✅ `AlertEvent` published to `customer-alert` topic (welcome notification)

### 2. **Transaction Events**
When a transaction occurs:
- ✅ `TransactionEvent` published to `account-transaction` topic
- ✅ `AlertEvent` published for customer-initiated transactions (not batch)

### 3. **Graceful Degradation**
- ✅ Kafka disabled by default (`kafka.enabled=false`)
- ✅ Service works normally without Kafka
- ✅ Event publishing failures don't break core operations
- ✅ Uses `@ConditionalOnProperty` to prevent bean creation when disabled

## Configuration

### application.yml
```yaml
kafka:
  enabled: false  # Change to true to enable
  bootstrap-servers: localhost:9092
  topics:
    account-created: account-created
    transaction: account-transaction
    alert: customer-alert
```

## Event Schemas

### AccountCreatedEvent
```json
{
  "accountNumber": "FD-20251109152512-1000-3",
  "accountName": "Test Account",
  "customerId": 1,
  "customerEmail": "customer@test.com",
  "productCode": "FD-STD-001",
  "principalAmount": 1000000,
  "interestRate": 6.5,
  "termMonths": 12,
  "status": "ACTIVE",
  "createdAt": "2025-11-09T15:25:13",
  "eventType": "ACCOUNT_CREATED"
}
```

### TransactionEvent
```json
{
  "transactionId": "TXN-20251109154323",
  "accountNumber": "FD-20251109152512-1000-3",
  "customerId": 1,
  "transactionType": "INTEREST_CREDIT",
  "amount": 178.08,
  "balanceBefore": 1000000,
  "balanceAfter": 1000178.08,
  "status": "COMPLETED",
  "description": "Interest accrual",
  "transactionDate": "2025-11-09T15:43:23",
  "eventType": "TRANSACTION_COMPLETED"
}
```

### AlertEvent
```json
{
  "customerId": 1,
  "customerEmail": "customer@test.com",
  "alertType": "ACCOUNT_CREATED",
  "subject": "FD Account Created Successfully",
  "message": "Your Fixed Deposit account FD-20251109... has been created with principal amount ₹1000000",
  "accountNumber": "FD-20251109152512-1000-3",
  "severity": "INFO",
  "timestamp": "2025-11-09T15:25:13",
  "eventType": "ALERT"
}
```

## How to Enable Kafka

### Option 1: Without Kafka (Current - No Changes Needed)
Service runs normally, events are not published.

### Option 2: With Kafka (For Production)

**Step 1: Install Kafka**
```bash
# Download Kafka from https://kafka.apache.org/downloads
# Start Zookeeper
bin\windows\zookeeper-server-start.bat config\zookeeper.properties

# Start Kafka (in another terminal)
bin\windows\kafka-server-start.bat config\server.properties
```

**Step 2: Enable in application.yml**
```yaml
kafka:
  enabled: true  # Changed from false
```

**Step 3: Restart Service**
```cmd
cd account-service
mvn spring-boot:run
```

**Step 4: Verify Topics Created**
```bash
bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```

Should show:
- account-created
- account-transaction
- customer-alert

## Testing Events (When Enabled)

### Test 1: Account Creation Event
```http
POST /api/accounts/accounts/create/default
```

**Expected Logs:**
```
✅ Account created successfully: FD-...
✅ Published AccountCreatedEvent for account: FD-...
✅ Published AlertEvent: ACCOUNT_CREATED for customer: 1
```

### Test 2: Transaction Event
```http
POST /api/accounts/transactions/account-number/FD-xxx/create
```

**Expected Logs:**
```
✅ Transaction created successfully: TXN-...
✅ Published TransactionEvent: INTEREST_CREDIT for account: FD-...
✅ Published AlertEvent: TRANSACTION_COMPLETED for customer: 1
```

### Test 3: View Events (Kafka Consumer)
```bash
# View account-created events
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic account-created --from-beginning

# View transaction events
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic account-transaction --from-beginning

# View alert events
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic customer-alert --from-beginning
```

## Alert Logic

### Account Alerts
- ✅ Sent for all account creations
- Severity: `INFO`

### Transaction Alerts
- ✅ Sent for customer-initiated transactions
- ✅ **Skipped** for batch system transactions (`SYSTEM-BATCH`)
- Severity: 
  - `WARNING` for amount > ₹1,00,000
  - `INFO` for smaller amounts

## Error Handling

### Event Publishing Failures
- ✅ Core operations continue (account/transaction still created)
- ✅ Error logged as `❌ Error publishing events (non-critical)`
- ✅ User receives successful response

### Kafka Unavailable
- ✅ Service starts normally
- ✅ Logs: `⏭️ Kafka disabled - skipping event publishing`

## Integration Points

### Modified Files
1. **pom.xml** - Added `spring-kafka` dependency
2. **AccountService.java** - Publishes events after account creation
3. **TransactionService.java** - Publishes events after transaction
4. **application.yml** - Added Kafka configuration

### New Files
1. **event/AccountCreatedEvent.java** - Account creation event DTO
2. **event/TransactionEvent.java** - Transaction event DTO
3. **event/AlertEvent.java** - Customer alert event DTO
4. **service/EventPublisher.java** - Kafka producer service
5. **config/KafkaTopicConfig.java** - Topic creation configuration

## Safety Features

✅ **No Breaking Changes**: Kafka disabled by default
✅ **Optional Dependency**: `@Autowired(required = false)` for EventPublisher
✅ **Conditional Beans**: `@ConditionalOnProperty` prevents errors when disabled
✅ **Error Isolation**: Publishing failures don't affect core operations
✅ **Graceful Degradation**: Service works with or without Kafka

## Next Steps (Optional)

1. **Notification Service**: Create consumer service to send emails/SMS from alerts
2. **Analytics Service**: Consume transaction events for real-time dashboards
3. **Audit Service**: Store all events for compliance
4. **Dead Letter Queue**: Handle failed event deliveries

## Troubleshooting

### Issue: Service won't start after enabling Kafka
**Solution**: Make sure Kafka is running on `localhost:9092`

### Issue: Topics not created automatically
**Solution**: Create manually:
```bash
bin\windows\kafka-topics.bat --create --topic account-created --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
bin\windows\kafka-topics.bat --create --topic account-transaction --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
bin\windows\kafka-topics.bat --create --topic customer-alert --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

### Issue: Events not visible in consumer
**Solution**: Check if events are being published (look for ✅ logs)

## Summary

✅ **Simple** - Minimal configuration needed
✅ **Safe** - Disabled by default, no breaking changes
✅ **Resilient** - Failures don't affect core functionality
✅ **Complete** - Account, Transaction, and Alert events covered
✅ **Production-Ready** - Proper error handling and logging

The implementation is **ready to use** with `kafka.enabled=false` (current setting). Enable Kafka only when needed for event-driven architecture.
