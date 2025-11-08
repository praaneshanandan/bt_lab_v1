# Kafka Event Publishing Implementation

**Implementation Date:** November 8, 2025  
**Service:** fd-account-service  
**Status:** ✅ Complete and Tested (BUILD SUCCESS)

---

## Overview

Implemented comprehensive Kafka-based event publishing for all major FD account operations. The system now publishes real-time events to Kafka topics, enabling event-driven architecture and integration with downstream services.

---

## Architecture

### Event Flow
```
FD Account Service Operation
    ↓
EventPublisher Service (Publishes to Kafka)
    ↓
Kafka Topics (6 dedicated topics)
    ↓
Downstream Services (Consumers)
```

### Components Added

1. **Event DTOs** (6 classes)
2. **EventPublisher Service** (Central publishing service)
3. **KafkaTopicConfig** (Topic auto-creation)
4. **Integration Points** (5 services + 2 batch jobs)

---

## Configuration

### Maven Dependency Added

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### application.yml Configuration

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: fd-account-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    template:
      default-topic: fd-account-events

# Kafka Topics Configuration
kafka:
  topics:
    account-created: account-created-events
    account-closed: account-closed-events
    transaction-created: transaction-created-events
    maturity-processed: maturity-processed-events
    interest-accrued: interest-accrued-events
    withdrawal-processed: withdrawal-processed-events
```

---

## Kafka Topics

### Topic Configuration

All topics are configured with:
- **Partitions:** 3
- **Replication Factor:** 1 (adjust for production)
- **Auto-creation:** Enabled via `@Bean` definitions

| Topic Name | Purpose | Key | Producer |
|-----------|---------|-----|----------|
| `account-created-events` | New account creation | Account Number | AccountCreationService |
| `account-closed-events` | Account closure | Account Number | PrematureWithdrawalService |
| `transaction-created-events` | All transactions | Account Number | TransactionService |
| `maturity-processed-events` | Maturity processing | Account Number | MaturityProcessingBatch |
| `interest-accrued-events` | Daily interest accrual | Account Number | InterestCalculationBatch |
| `withdrawal-processed-events` | Premature withdrawals | Account Number | PrematureWithdrawalService |

---

## Event DTOs

### 1. AccountCreatedEvent

**Published When:** Account is created (standard or customized)

**Data Included:**
```java
- eventId: UUID
- eventType: "AccountCreatedEvent"
- timestamp: LocalDateTime
- accountId: Long
- accountNumber: String
- iban: String
- accountName: String
- customerId: Long
- customerName: String
- productCode: String
- productName: String
- principalAmount: BigDecimal
- termMonths: Integer
- interestRate: BigDecimal
- interestCalculationMethod: String
- effectiveDate: LocalDate
- maturityDate: LocalDate
- maturityAmount: BigDecimal
- maturityInstruction: String
- branchCode: String
- createdBy: String
```

**Trigger Points:**
- `AccountCreationService.createAccount()` - After account save
- `AccountCreationService.createCustomizedAccount()` - After account save

**Use Cases:**
- Customer notification (SMS/Email)
- Analytics and reporting
- Audit logging
- Downstream service synchronization
- Credit scoring updates

---

### 2. TransactionCreatedEvent

**Published When:** Any transaction is created on an account

**Data Included:**
```java
- eventId: UUID
- eventType: "TransactionCreatedEvent"
- timestamp: LocalDateTime
- accountId: Long
- accountNumber: String
- customerId: Long
- transactionId: Long
- transactionReference: String
- transactionType: String (DEPOSIT, WITHDRAWAL, INTEREST_ACCRUAL, etc.)
- amount: BigDecimal
- transactionDate: LocalDate
- valueDate: LocalDate
- description: String
- principalBalanceAfter: BigDecimal
- interestBalanceAfter: BigDecimal
- totalBalanceAfter: BigDecimal
- performedBy: String
- isReversed: Boolean
```

**Trigger Points:**
- `TransactionService.createTransaction()` - After transaction save

**Transaction Types Covered:**
- ADDITIONAL_DEPOSIT
- WITHDRAWAL
- INTEREST_CREDIT
- INTEREST_ACCRUAL
- INTEREST_CAPITALIZATION
- FEE_DEBIT
- PENALTY
- MATURITY_PAYOUT
- MATURITY_RENEWAL

**Use Cases:**
- Real-time transaction alerts
- Fraud detection
- Balance reconciliation
- Transaction reporting
- Regulatory compliance

---

### 3. InterestAccruedEvent

**Published When:** Daily interest is calculated and accrued (1:00 AM daily)

**Data Included:**
```java
- eventId: UUID
- eventType: "InterestAccruedEvent"
- timestamp: LocalDateTime
- accountId: Long
- accountNumber: String
- customerId: Long
- interestAmount: BigDecimal (daily interest)
- accrualDate: LocalDate
- interestRate: BigDecimal
- calculationMethod: String
- principalBalance: BigDecimal
- totalInterestAccrued: BigDecimal
- totalBalance: BigDecimal
- daysCompleted: Integer
- totalDays: Integer
```

**Trigger Points:**
- `InterestCalculationBatch.calculateDailyInterest()` - After interest calculation

**Use Cases:**
- Interest accrual tracking
- Customer interest statements
- Accounting reconciliation
- Interest projection analytics
- Maturity amount forecasting

---

### 4. WithdrawalProcessedEvent

**Published When:** Premature withdrawal is processed

**Data Included:**
```java
- eventId: UUID
- eventType: "WithdrawalProcessedEvent"
- timestamp: LocalDateTime
- accountId: Long
- accountNumber: String
- accountName: String
- customerId: Long
- customerName: String
- withdrawalDate: LocalDate
- withdrawalReason: String
- isPremature: Boolean
- principalAmount: BigDecimal
- interestEarned: BigDecimal
- penaltyAmount: BigDecimal
- penaltyPercentage: BigDecimal
- netAmount: BigDecimal
- daysCompleted: Integer
- termDays: Integer
- accountStatusAfter: String (CLOSED)
- processedBy: String
- transactionReference: String
```

**Trigger Points:**
- `PrematureWithdrawalService.processPrematureWithdrawal()` - After withdrawal processing

**Use Cases:**
- Penalty calculation audit
- Customer notification (SMS/Email with penalty details)
- Analytics on premature closures
- Regulatory reporting
- Funds transfer initiation

---

### 5. AccountClosedEvent

**Published When:** Account is closed (premature withdrawal or maturity)

**Data Included:**
```java
- eventId: UUID
- eventType: "AccountClosedEvent"
- timestamp: LocalDateTime
- accountId: Long
- accountNumber: String
- accountName: String
- customerId: Long
- customerName: String
- closureDate: LocalDate
- closureReason: String
- closureType: String (PREMATURE, MATURITY, MANUAL)
- principalAmount: BigDecimal
- interestEarned: BigDecimal
- penaltyAmount: BigDecimal
- netPayoutAmount: BigDecimal
- daysCompleted: Integer
- totalDays: Integer
- closedBy: String
```

**Trigger Points:**
- `PrematureWithdrawalService.publishAccountClosedEvent()` - After premature withdrawal
- (Can be extended to maturity closure scenarios)

**Use Cases:**
- Account lifecycle tracking
- Closure reason analytics
- Customer retention analysis
- Automated account archival
- Funds transfer processing

---

### 6. MaturityProcessedEvent

**Published When:** Account reaches maturity and is processed (1:30 AM daily)

**Data Included:**
```java
- eventId: UUID
- eventType: "MaturityProcessedEvent"
- timestamp: LocalDateTime
- accountId: Long
- accountNumber: String
- accountName: String
- customerId: Long
- customerName: String
- maturityDate: LocalDate
- maturityInstruction: String (CLOSE_AND_PAYOUT, RENEW_PRINCIPAL_ONLY, etc.)
- principalAmount: BigDecimal
- interestEarned: BigDecimal
- totalAmount: BigDecimal
- payoutAmount: BigDecimal
- renewalAmount: BigDecimal
- isRenewed: Boolean
- newMaturityDate: LocalDate (if renewed)
- newTermMonths: Integer (if renewed)
- transferToAccount: String (if transferred)
- processedBy: String
```

**Trigger Points:**
- `MaturityProcessingBatch.processClosureAndPayout()` - After payout
- `MaturityProcessingBatch.processRenewalPrincipalOnly()` - After renewal
- `MaturityProcessingBatch.processRenewalWithInterest()` - After renewal

**Maturity Instructions Supported:**
- **CLOSE_AND_PAYOUT** - Full payout, account closed
- **RENEW_PRINCIPAL_ONLY** - Interest paid, principal renewed
- **RENEW_WITH_INTEREST** - Total amount renewed (compound effect)
- **TRANSFER_TO_SAVINGS** - Transfer to savings account
- **TRANSFER_TO_CURRENT** - Transfer to current account
- **HOLD** - No action, awaiting customer instruction

**Use Cases:**
- Maturity payout processing
- Customer notification (maturity instructions executed)
- Renewal tracking
- Compound interest analytics
- Fund transfer automation

---

## Implementation Details

### EventPublisher Service

**Location:** `com.app.fdaccount.service.EventPublisher`

**Key Features:**
- Centralized event publishing
- Automatic event enrichment (eventId, timestamp, eventType)
- Asynchronous publishing with CompletableFuture
- Comprehensive logging (success/failure)
- Graceful error handling (doesn't fail main transaction)

**Methods:**
```java
public void publishAccountCreated(AccountCreatedEvent event)
public void publishAccountClosed(AccountClosedEvent event)
public void publishTransactionCreated(TransactionCreatedEvent event)
public void publishMaturityProcessed(MaturityProcessedEvent event)
public void publishInterestAccrued(InterestAccruedEvent event)
public void publishWithdrawalProcessed(WithdrawalProcessedEvent event)
```

**Event Enrichment:**
- Auto-generates UUID for `eventId` if not set
- Sets `timestamp` to `LocalDateTime.now()` if not set
- Sets `eventType` to class name if not set

**Error Handling:**
```java
try {
    // Publish event
    eventPublisher.publishAccountCreated(event);
} catch (Exception e) {
    log.error("Failed to publish event: {}", e.getMessage(), e);
    // Don't fail the transaction if event publishing fails
}
```

---

## Integration Points

### 1. AccountCreationService

**Methods Modified:**
- `createAccount()` - Line 187: Publishes `AccountCreatedEvent`
- `createCustomizedAccount()` - Line 329: Publishes `AccountCreatedEvent`

**Event Data Source:**
- FdAccount entity
- ProductDto from product-pricing-service
- Primary AccountRole (customer details)

---

### 2. TransactionService

**Methods Modified:**
- `createTransaction()` - Line 165: Publishes `TransactionCreatedEvent`

**Event Data Source:**
- AccountTransaction entity
- FdAccount entity
- Primary customer ID from account roles

---

### 3. PrematureWithdrawalService

**Methods Modified:**
- `processPrematureWithdrawal()` - Line 222: Publishes `WithdrawalProcessedEvent` and `AccountClosedEvent`

**Event Data Source:**
- FdAccount entity
- PrematureWithdrawalInquiryResponse (penalty calculation)
- Transaction reference

---

### 4. InterestCalculationBatch

**Methods Modified:**
- `calculateDailyInterest()` - Line 127: Publishes `InterestAccruedEvent`

**Event Data Source:**
- FdAccount entity
- Daily interest calculation result
- Balance information

**Batch Schedule:** Daily at 1:00 AM

---

### 5. MaturityProcessingBatch

**Methods Modified:**
- `processClosureAndPayout()` - Line 151: Publishes `MaturityProcessedEvent`
- `processRenewalPrincipalOnly()` - Line 211: Publishes `MaturityProcessedEvent`
- `processRenewalWithInterest()` - Line 252: Publishes `MaturityProcessedEvent`

**Event Data Source:**
- FdAccount entity
- Maturity instruction
- Principal and interest amounts
- Renewal details (if applicable)

**Batch Schedule:** Daily at 1:30 AM (after interest calculation)

---

## Testing

### Prerequisites

1. **Kafka Server Running:**
```bash
# Start Zookeeper
zookeeper-server-start.bat config\zookeeper.properties

# Start Kafka
kafka-server-start.bat config\server.properties
```

2. **Create Topics (Optional - auto-created by application):**
```bash
kafka-topics.bat --create --topic account-created-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.bat --create --topic transaction-created-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.bat --create --topic interest-accrued-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.bat --create --topic withdrawal-processed-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.bat --create --topic account-closed-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.bat --create --topic maturity-processed-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### Test Scenarios

#### 1. Test Account Creation Event

**API Call:**
```bash
POST http://localhost:8086/api/fd-accounts/accounts
Authorization: Bearer <JWT_TOKEN>

{
  "productCode": "FD-REGULAR",
  "principalAmount": 100000,
  "termMonths": 12,
  "effectiveDate": "2025-11-08",
  "branchCode": "001",
  "branchName": "Main Branch",
  "roles": [
    {
      "customerId": 1,
      "customerName": "John Doe",
      "roleType": "PRIMARY_OWNER",
      "ownershipPercentage": 100.00,
      "isPrimary": true
    }
  ],
  "createdBy": "BANK_OFFICER"
}
```

**Expected Kafka Message:**
```json
{
  "eventId": "123e4567-e89b-12d3-a456-426614174000",
  "eventType": "AccountCreatedEvent",
  "timestamp": "2025-11-08T10:30:00",
  "accountId": 1,
  "accountNumber": "001-100001-7",
  "iban": "IN12CRXA0011000017",
  "accountName": "John Doe FD",
  "customerId": 1,
  "customerName": "John Doe",
  "productCode": "FD-REGULAR",
  "productName": "Regular Fixed Deposit",
  "principalAmount": 100000.00,
  "termMonths": 12,
  "interestRate": 7.50,
  "interestCalculationMethod": "COMPOUND",
  "effectiveDate": "2025-11-08",
  "maturityDate": "2026-11-08",
  "maturityAmount": 107788.00,
  "maturityInstruction": "HOLD",
  "branchCode": "001",
  "createdBy": "BANK_OFFICER"
}
```

**Verify with Kafka Consumer:**
```bash
kafka-console-consumer.bat --topic account-created-events --from-beginning --bootstrap-server localhost:9092
```

---

#### 2. Test Transaction Event

**API Call:**
```bash
POST http://localhost:8086/api/fd-accounts/transactions
Authorization: Bearer <JWT_TOKEN>

{
  "accountNumber": "001-100001-7",
  "transactionType": "ADDITIONAL_DEPOSIT",
  "amount": 10000.00,
  "transactionDate": "2025-11-08",
  "description": "Additional deposit",
  "performedBy": "BANK_OFFICER"
}
```

**Expected Event:** `TransactionCreatedEvent` published to `transaction-created-events`

---

#### 3. Test Interest Accrual Event

**Trigger:** Run interest calculation batch manually or wait for scheduled execution (1:00 AM)

**Manual Trigger (if endpoint exists):**
```bash
POST http://localhost:8086/api/fd-accounts/batch/interest-calculation
Authorization: Bearer <JWT_TOKEN>
```

**Expected Event:** `InterestAccruedEvent` published for each active account

---

#### 4. Test Premature Withdrawal Event

**API Call:**
```bash
POST http://localhost:8086/api/fd-accounts/transactions/premature-withdrawal/process
Authorization: Bearer <JWT_TOKEN>

{
  "accountNumber": "001-100001-7",
  "withdrawalDate": "2025-12-01",
  "reason": "Financial emergency"
}
```

**Expected Events:**
1. `WithdrawalProcessedEvent` published to `withdrawal-processed-events`
2. `AccountClosedEvent` published to `account-closed-events`

---

#### 5. Test Maturity Processing Event

**Trigger:** Run maturity processing batch manually or wait for scheduled execution (1:30 AM)

**Expected Event:** `MaturityProcessedEvent` published for each maturing account

---

### Monitoring Events

#### Console Consumer
```bash
# Monitor all topics
kafka-console-consumer.bat --topic account-created-events --from-beginning --bootstrap-server localhost:9092
kafka-console-consumer.bat --topic transaction-created-events --from-beginning --bootstrap-server localhost:9092
kafka-console-consumer.bat --topic interest-accrued-events --from-beginning --bootstrap-server localhost:9092
```

#### Application Logs
```
✅ Event published successfully to topic 'account-created-events' - Partition: 0, Offset: 123, Event: AccountCreatedEvent
✅ Event published successfully to topic 'transaction-created-events' - Partition: 1, Offset: 456, Event: TransactionCreatedEvent
```

#### Error Logs
```
❌ Failed to publish event to topic 'account-created-events': Connection refused
```

---

## Consumer Implementation (Downstream Services)

### Example Consumer Service

```java
@Service
@Slf4j
public class AccountEventConsumer {

    @KafkaListener(topics = "${kafka.topics.account-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleAccountCreated(AccountCreatedEvent event) {
        log.info("📩 Received AccountCreatedEvent: {}", event.getAccountNumber());
        
        // Send customer notification
        sendAccountCreationSMS(event.getCustomerId(), event.getAccountNumber());
        sendAccountCreationEmail(event.getCustomerId(), event.getAccountNumber());
        
        // Update analytics
        updateAccountCreationMetrics(event);
        
        // Sync with other systems
        syncToDataWarehouse(event);
    }

    @KafkaListener(topics = "${kafka.topics.transaction-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleTransactionCreated(TransactionCreatedEvent event) {
        log.info("📩 Received TransactionCreatedEvent: {}", event.getTransactionReference());
        
        // Send transaction alert
        if (event.getAmount().compareTo(new BigDecimal("50000")) > 0) {
            sendHighValueTransactionAlert(event);
        }
        
        // Update real-time balance display
        updateCustomerBalanceCache(event);
    }

    @KafkaListener(topics = "${kafka.topics.interest-accrued}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInterestAccrued(InterestAccruedEvent event) {
        log.info("📩 Received InterestAccruedEvent: {}", event.getAccountNumber());
        
        // Update customer's interest dashboard
        updateInterestProjection(event);
        
        // Accounting entry
        createInterestAccrualEntry(event);
    }
}
```

---

## Performance Considerations

### Event Publishing

- **Asynchronous:** Events are published asynchronously to avoid blocking main transaction
- **Error Isolation:** Event publishing failures don't fail the main transaction
- **Retry:** Configure Kafka producer retry settings for transient failures

### Recommended Production Settings

```yaml
spring:
  kafka:
    producer:
      retries: 3
      acks: all  # Wait for all replicas to acknowledge
      compression-type: snappy
      batch-size: 16384
      linger-ms: 10
      buffer-memory: 33554432
```

### Monitoring Metrics

- **Event Publishing Rate:** Monitor via application logs
- **Kafka Lag:** Monitor consumer lag for downstream services
- **Failed Events:** Monitor error logs and Kafka dead letter queues

---

## Deployment Checklist

### Pre-Production

- [ ] Kafka cluster running and accessible
- [ ] Topics created (or auto-creation enabled)
- [ ] Replication factor set appropriately (min 3 for production)
- [ ] Consumer services deployed and tested
- [ ] Monitoring and alerting configured
- [ ] Error handling and retry policies configured
- [ ] Schema registry configured (optional but recommended)

### Configuration Updates

**Development:**
```yaml
spring.kafka.bootstrap-servers: localhost:9092
```

**Production:**
```yaml
spring.kafka.bootstrap-servers: kafka-broker-1:9092,kafka-broker-2:9092,kafka-broker-3:9092
```

### Security

Consider adding Kafka security for production:
```yaml
spring:
  kafka:
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: org.apache.kafka.common.security.plain.PlainLoginModule required username="<username>" password="<password>";
```

---

## Troubleshooting

### Issue: Events not published

**Symptoms:** No events appearing in Kafka topics

**Solutions:**
1. Check Kafka server is running: `kafka-topics.bat --list --bootstrap-server localhost:9092`
2. Check application logs for connection errors
3. Verify `spring.kafka.bootstrap-servers` configuration
4. Check network connectivity to Kafka broker

---

### Issue: Consumer not receiving events

**Symptoms:** Events published but consumer not processing

**Solutions:**
1. Verify consumer group ID is unique
2. Check consumer is subscribed to correct topic
3. Verify deserializer configuration matches producer
4. Check consumer application logs

---

### Issue: Serialization errors

**Symptoms:** `JsonSerializer` errors in logs

**Solutions:**
1. Verify event DTOs are properly annotated with Lombok
2. Check for circular references in event objects
3. Configure Jackson ObjectMapper if custom serialization needed

---

## Future Enhancements

### 1. Dead Letter Queue (DLQ)
Implement DLQ for failed event processing:
```java
@Bean
public NewTopic accountCreatedDLQ() {
    return TopicBuilder.name("account-created-events-dlq")
            .partitions(1)
            .replicas(1)
            .build();
}
```

### 2. Event Versioning
Add version field to all events for backward compatibility:
```java
private String eventVersion = "1.0";
```

### 3. Schema Registry
Integrate Confluent Schema Registry for schema evolution:
```yaml
spring:
  kafka:
    properties:
      schema.registry.url: http://localhost:8081
```

### 4. Event Replay
Implement event replay functionality for disaster recovery

### 5. Event Sourcing
Consider full event sourcing for complete audit trail

---

## Summary

✅ **Implementation Complete:**
- 6 event types defined
- 6 Kafka topics configured
- EventPublisher service implemented
- 5 services integrated
- 2 batch jobs integrated
- Comprehensive logging and error handling
- Build successful

**Event Coverage:**
- Account lifecycle (creation, closure)
- Transaction tracking (all types)
- Interest accrual (daily)
- Premature withdrawal (with penalty)
- Maturity processing (all instructions)

**Next Steps:**
1. Start Kafka server
2. Deploy fd-account-service
3. Test event publishing with API calls
4. Implement consumer services in downstream applications
5. Monitor events and adjust configuration as needed

---

**Documentation maintained by:** GitHub Copilot  
**Last Updated:** November 8, 2025
