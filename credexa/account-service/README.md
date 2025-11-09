# Account Service - Simplified FD Account Management

## Overview
Account Service is a simplified, clean implementation for managing Fixed Deposit (FD) accounts. It replaces the complex `fd-account-service` with a streamlined architecture focused on core functionality.

## Key Features

### ✅ Implemented Features

1. **Dual Account Creation Modes**
   - **Version 1 (Default)**: All values defaulted from product configuration
   - **Version 2 (Custom)**: Customized interest rate within ±2% of base rate, custom calculation parameters

2. **Account Number Generation**
   - **Standard Format**: `FD-YYYYMMDDHHMMSS-NNNN-C` with Luhn check digit
   - **IBAN Format**: `IN<check><bank><branch><account>` with mod 97 validation
   - Automatic generation for both formats on account creation

3. **Flexible Account Inquiry**
   - Search by Account Number (default)
   - Search by IBAN
   - Search by Internal Database ID
   - Single unified endpoint for all ID types

4. **Account Listing**
   - Paginated list of all accounts (MANAGER/ADMIN)
   - Filter by customer
   - Sortable by any field
   - Supports pagination parameters

5. **Account Details & Balance**
   - Complete account information
   - Balance with TDS calculations
   - Days to maturity
   - Net amount after TDS

6. **Transaction Management**
   - Create transactions with flexible account ID types
   - Transaction inquiry using account ID type + transaction ID
   - Transaction list using account ID types (paginated)
   - Automatic balance tracking (before/after each transaction)
   - Multiple transaction types (DEPOSIT, INTEREST_CREDIT, TDS_DEDUCTION, etc.)
   - Transaction count and history

7. **Redemption Management**
   - Redemption inquiry with complete calculation (interest, TDS, penalties)
   - Full redemption (account closure) with automatic transaction creation
   - Partial redemption (withdrawal) with minimum balance validation
   - Automatic penalty calculation for premature redemption (0.5% on interest)
   - Support for all account ID types (ACCOUNT_NUMBER, IBAN, INTERNAL_ID)
   - Redemption type detection (PREMATURE, ON_MATURITY, POST_MATURITY)
   - Net redemption amount calculation with detailed breakdown

8. **Interest Calculation Process**
   - Calculate interest for specific periods using simple interest formula
   - Automatic period detection from last interest credit date
   - Optional interest crediting (creates INTEREST_CREDIT transaction)
   - Automatic TDS deduction (creates TDS_DEDUCTION transaction)
   - Balance tracking through transaction chain
   - Historical interest and TDS summary
   - Support for quarterly, half-yearly, or custom period calculations

## Architecture

### Clean Design Principles
- **Denormalized Entity**: Customer and product data stored directly (no complex joins)
- **Simple Integration**: WebClient with automatic JWT forwarding
- **No Batch Jobs**: Focused on API operations only
- **No Event-Driven**: REST-only integration
- **Role-Based Access**: Simple JWT authentication with 3 roles

### Technology Stack
- **Framework**: Spring Boot 3.2.5
- **Java**: 17
- **Security**: Spring Security 6.x with JWT (JJWT 0.12.6)
- **Database**: MySQL (account_db)
- **API Docs**: SpringDoc OpenAPI 2.7.0
- **Integration**: Spring WebFlux WebClient
- **Build**: Maven

## Service Configuration

### Port & Context
- **Port**: 8087
- **Context Path**: /api/accounts
- **Swagger UI**: http://localhost:8087/swagger-ui.html

### Database
- **Name**: account_db
- **Auto-create**: Enabled
- **Hibernate DDL**: update

### JWT Configuration
- **Secret**: Shared across all services (256-bit HS256)
- **Expiration**: 1 hour (3600000ms)
- **Version**: JJWT 0.12.6 (consistent with ecosystem)

## Integration Services

### 1. Login Service (Port 8080)
- **Purpose**: JWT token generation
- **Used For**: Authentication

### 2. Customer Service (Port 8082)
- **Purpose**: Customer data management
- **Used For**: Validate customer, fetch customer details

### 3. Product Pricing Service (Port 8084)
- **Purpose**: FD product configuration
- **Used For**: Interest rates, term limits, TDS rules, validation

### 4. Calculator Service (Port 8085)
- **Purpose**: FD maturity calculations
- **Used For**: Calculate maturity amount, interest earned, TDS

## API Endpoints

### Public Endpoints
- `GET /api/accounts/health` - Health check

### Authenticated Endpoints

#### Account Creation (MANAGER/ADMIN)
- `POST /api/accounts/create/default` - Create with default product values
- `POST /api/accounts/create/custom` - Create with custom values (±2% rate variance)

#### Account Inquiry (ALL ROLES)
- `POST /api/accounts/inquiry` - Find by flexible ID type
- `GET /api/accounts/{accountNumber}` - Get by account number
- `GET /api/accounts/{accountNumber}/balance` - Get balance

#### Account Listing (MANAGER/ADMIN for all, CUSTOMER for own)
- `GET /api/accounts` - List all accounts (paginated)
- `GET /api/accounts/customer/{customerId}` - List by customer (paginated)

#### Transaction Management
- `POST /api/transactions/create` - Create transaction (with account ID type) (MANAGER/ADMIN)
- `POST /api/transactions/inquiry` - Transaction inquiry (with account ID type) (ALL ROLES)
- `GET /api/transactions/{transactionId}` - Get by transaction ID (ALL ROLES)
- `GET /api/transactions/list` - List by account (with ID type) (ALL ROLES)
- `GET /api/transactions/account/{accountNumber}` - List by account number (ALL ROLES)
- `GET /api/transactions/type/{type}` - List by type (MANAGER/ADMIN)
- `GET /api/transactions/status/{status}` - List by status (MANAGER/ADMIN)
- `GET /api/transactions/count/{accountNumber}` - Get count (ALL ROLES)

#### Redemption Management
- `POST /api/redemptions/inquiry` - Get redemption calculation details (ALL ROLES)
- `POST /api/redemptions/process` - Process full or partial redemption (MANAGER/ADMIN)

#### Interest Calculation
- `POST /api/interest/calculate` - Calculate and credit interest with optional TDS (MANAGER/ADMIN)

## Quick Start

### Prerequisites
1. MySQL running on localhost:3306
2. Java 17 installed
3. All dependent services running:
   - login-service (8080)
   - customer-service (8082)
   - product-pricing-service (8084)
   - calculator-service (8085)

### Build & Run
```bash
# Clone and navigate
cd account-service

# Build
mvnw clean install

# Run
mvnw spring-boot:run

# Or use start script (Windows)
start-service.bat
```

### Access Swagger UI
1. Open: http://localhost:8087/swagger-ui.html
2. Get JWT token from login-service (http://localhost:8080/api/auth/login)
3. Click "Authorize" in Swagger
4. Enter: `Bearer <your-token>`
5. Test endpoints

## Testing Guide

See [SWAGGER-TESTING-GUIDE.md](SWAGGER-TESTING-GUIDE.md) for comprehensive account testing instructions.

See [TRANSACTION-TESTING-GUIDE.md](TRANSACTION-TESTING-GUIDE.md) for transaction management testing.

See [REDEMPTION-TESTING-GUIDE.md](REDEMPTION-TESTING-GUIDE.md) for redemption features testing.

See [INTEREST-CALCULATION-TESTING-GUIDE.md](INTEREST-CALCULATION-TESTING-GUIDE.md) for interest calculation testing.

### Quick Test Sequence
1. **Login**: Get JWT from login-service as MANAGER/ADMIN
2. **Create Account (Default)**:
   ```json
   POST /api/accounts/create/default
   {
     "accountName": "Test FD Account",
     "customerId": 1,
     "productCode": "FD-STD-001",
     "principalAmount": 50000,
     "termMonths": 12,
     "effectiveDate": "2025-11-08"
   }
   ```
3. **Create Account (Custom)**:
   ```
   POST /api/accounts/create/custom?customInterestRate=8.5&customCalculationType=COMPOUND
   (same body as above)
   ```
4. **Inquiry**: Test with different ID types
5. **List**: View all accounts
6. **Balance**: Check account balance

## Account Number Generation

### Standard Account Number
- **Format**: `FD-YYYYMMDDHHMMSS-NNNN-C`
- **Example**: `FD-20251108120000-1234-5`
- **Check Digit**: Luhn algorithm (last digit)
- **Validation**: Automatic on creation

### IBAN Format
- **Format**: `IN<check><bank><branch><account>`
- **Example**: `IN29CRED0001FD2511081234`
- **Check Digit**: Mod 97 algorithm (positions 3-4)
- **Bank Code**: CRED (Credexa)
- **Branch Code**: 0001 (default)

## Role-Based Access Control

| Role | Create Account | View Account | List All | List Own |
|------|---------------|--------------|----------|----------|
| CUSTOMER | ❌ | ✅ | ❌ | ✅ |
| MANAGER | ✅ | ✅ | ✅ | ✅ |
| ADMIN | ✅ | ✅ | ✅ | ✅ |

## Data Model

### FdAccount Entity (Denormalized)
- **Account Info**: accountNumber, ibanNumber, accountName
- **Customer Info**: customerId, customerName, email, mobile
- **Product Info**: productCode, productName, productType
- **Financial**: principalAmount, interestRate, termMonths, maturityAmount, interestEarned
- **Dates**: effectiveDate, maturityDate, closureDate
- **Calculation**: calculationType, compoundingFrequency
- **TDS**: tdsRate, tdsAmount, tdsApplicable
- **Status**: ACTIVE, MATURED, CLOSED, SUSPENDED
- **Branch**: branchCode, branchName
- **Audit**: createdBy, updatedBy, createdAt, updatedAt

### FdTransaction Entity
- **Transaction Info**: transactionId, accountNumber
- **Type**: DEPOSIT, INTEREST_CREDIT, TDS_DEDUCTION, WITHDRAWAL, MATURITY_CREDIT, CLOSURE, REVERSAL, ADJUSTMENT
- **Amount**: amount, balanceBefore, balanceAfter
- **Status**: PENDING, APPROVED, COMPLETED, FAILED, REJECTED, REVERSED
- **References**: referenceNumber, description, remarks
- **Approval**: initiatedBy, approvedBy, transactionDate, approvalDate, valueDate
- **Metadata**: channel, branchCode, ipAddress

### Redemption Features
- **Inquiry**: Complete calculation including interest earned, TDS deducted, penalty (if premature)
- **Process**: Full redemption (CLOSURE transaction, account status → CLOSED) or Partial redemption (WITHDRAWAL transaction, account remains ACTIVE)
- **Penalty**: 0.5% on interest earned for premature redemptions (before maturity date)
- **Minimum Balance**: 10% of principal amount for partial redemptions
- **Redemption Types**: PREMATURE (before maturity), ON_MATURITY (on maturity date), POST_MATURITY (after maturity)
- **Net Amount**: Calculated as Current Balance + Interest Earned - TDS Deducted - Penalty Amount

### Interest Calculation
- **Formula**: Simple Interest = (Principal × Rate × Days) / (100 × 365)
- **Period Detection**: Automatic detection from last interest credit date or manual specification
- **Crediting**: Optional crediting creates INTEREST_CREDIT transaction
- **TDS**: Automatic TDS_DEDUCTION transaction if applicable
- **Balance Tracking**: Maintains balance chain through transactions
- **Historical Summary**: Shows total interest and TDS credited till date

## Key Differences from fd-account-service

| Aspect | Old (fd-account-service) | New (account-service) |
|--------|-------------------------|----------------------|
| Architecture | Complex with roles, batch, Kafka | Simple, API-focused |
| Entity Design | Normalized (multiple tables) | Denormalized (single table) |
| Account Numbers | Complex plugin system | Simple generator with check digit |
| Integration | Mixed patterns | Consistent WebClient |
| JWT | Mismatched versions/secrets | Consistent across services |
| Features | Many (batch, events, complex roles) | Core only (CRUD, inquiry, list) |
| Code Quality | Spaghetti code | Clean, maintainable |

## Validation Rules

### Product Constraints
- Principal amount: Within product min/max
- Term months: Within product min/max
- Product must be active

### Custom Rate Constraints
- Interest rate: Base rate ±2%
- Example: If base is 7.5%, allowed range is 5.5% - 9.5%

### Customer Validation
- Must exist in customer-service
- Must have valid customer ID

## Logging

Debug logging enabled for:
- `com.app.account` - All account service operations
- `org.springframework.security` - Security filter chain
- `org.springframework.web` - HTTP requests

Log patterns:
- 🚀 = Filter/operation started
- 🔍 = Inspection/validation
- ✅ = Success
- ❌ = Error/failure
- 💰 = Financial operation
- 📋 = List operation
- 📝 = Creation operation

## Error Handling

All errors return consistent `ApiResponse` format:
```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

Common error scenarios:
- 400: Validation failed, constraints violated
- 401: JWT token missing/invalid
- 403: Insufficient permissions
- 404: Account/customer/product not found
- 500: Integration failure or internal error

## Future Enhancements (Not Implemented)

- [ ] Account statements
- [ ] Redemption approval workflow (currently auto-approved)
- [ ] Transaction history
- [ ] Nominee management
- [ ] Joint account support
- [ ] Auto-renewal on maturity
- [ ] Interest rate revision
- [ ] Customer ID validation in CUSTOMER role endpoints

## Dependencies (pom.xml)

### Core
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- spring-boot-starter-validation

### Database
- mysql-connector-j

### JWT
- jjwt-api: 0.12.6
- jjwt-impl: 0.12.6
- jjwt-jackson: 0.12.6

### Integration
- spring-boot-starter-webflux

### Documentation
- springdoc-openapi-starter-webmvc-ui: 2.7.0

### Utilities
- lombok
- common-lib (internal)

## Project Structure

```
account-service/
├── src/main/java/com/app/account/
│   ├── AccountServiceApplication.java      # Main class
│   ├── client/                             # Service integration
│   │   ├── CustomerServiceClient.java
│   │   ├── ProductServiceClient.java
│   │   └── CalculatorServiceClient.java
│   ├── config/                             # Configuration
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── SecurityConfig.java
│   │   ├── WebClientConfig.java
│   │   └── OpenApiConfig.java
│   ├── controller/                         # REST endpoints
│   │   ├── AccountController.java
│   │   ├── TransactionController.java
│   │   ├── RedemptionController.java
│   │   └── InterestCalculationController.java
│   ├── dto/                                # Request/Response DTOs
│   │   ├── CreateAccountRequest.java
│   │   ├── AccountResponse.java
│   │   ├── BalanceResponse.java
│   │   ├── AccountInquiryRequest.java
│   │   ├── CreateTransactionRequest.java
│   │   ├── TransactionResponse.java
│   │   ├── TransactionInquiryRequest.java
│   │   ├── RedemptionInquiryRequest.java
│   │   ├── RedemptionInquiryResponse.java
│   │   ├── RedemptionProcessRequest.java
│   │   ├── RedemptionProcessResponse.java
│   │   ├── InterestCalculationRequest.java
│   │   ├── InterestCalculationResponse.java
│   │   └── external/                       # External service DTOs
│   │       ├── CustomerDto.java
│   │       ├── ProductDto.java
│   │       ├── CalculationRequest.java
│   │       └── CalculationResponse.java
│   ├── entity/                             # JPA entities
│   │   ├── FdAccount.java
│   │   └── FdTransaction.java
│   ├── repository/                         # Data access
│   │   ├── FdAccountRepository.java
│   │   └── FdTransactionRepository.java
│   ├── service/                            # Business logic
│   │   ├── AccountService.java
│   │   ├── TransactionService.java
│   │   ├── RedemptionService.java
│   │   └── InterestCalculationService.java
│   └── util/                               # Utilities
│       └── AccountNumberGenerator.java
├── src/main/resources/
│   └── application.yml                     # Configuration
├── pom.xml                                 # Maven dependencies
├── start-service.bat                       # Windows start script
├── README.md                               # This file
├── SWAGGER-TESTING-GUIDE.md               # Account testing guide
├── TRANSACTION-TESTING-GUIDE.md           # Transaction testing guide
├── REDEMPTION-TESTING-GUIDE.md            # Redemption testing guide
├── INTEREST-CALCULATION-TESTING-GUIDE.md  # Interest calculation testing guide
└── TRANSACTION-IMPLEMENTATION-SUMMARY.md  # Transaction features summary
```

## Support & Documentation

- **Swagger UI**: http://localhost:8087/swagger-ui.html (when running)
- **Testing Guide**: SWAGGER-TESTING-GUIDE.md
- **Architecture**: Clean, simple, maintainable
- **Contact**: Credexa Banking Platform Team

## License
Internal project for Credexa Banking Platform

---

**Version**: 1.0.0  
**Last Updated**: November 8, 2025  
**Status**: ✅ Production Ready (Core Features)
