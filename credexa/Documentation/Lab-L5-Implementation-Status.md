# Lab L5: Product and Pricing Module - API Development and Documentation

**Date:** November 5, 2025  
**Status:** ✅ **100% COMPLETE**

---

## Summary

Lab L5 focuses on developing and documenting RESTful APIs for the **Product and Pricing Module**, which serves as the central component defining banking products (FDs, savings accounts, etc.). All financial accounts are derived from these product definitions, which encapsulate business rules, interest configurations, charges, and role definitions.

---

## ✅ What's Implemented

### 1. Product Structure

Each product includes comprehensive configuration:

#### Basic Information
- Product Code (unique identifier)
- Product Name
- Product Type (FD variants: FIXED_DEPOSIT, TAX_SAVER_FD, SENIOR_CITIZEN_FD, FLEXI_FD, CUMULATIVE_FD, NON_CUMULATIVE_FD)
- Currency (INR, USD, etc.)
- Effective Date & Expiry Date

#### Business Rules
- **Term Limits:** Minimum and maximum deposit terms (months)
- **Amount Limits:** Minimum and maximum deposit amounts
- **Rate Limits:** Interest rate ranges
- **Transaction Types:** Allowed operations (deposit, withdrawal, etc.)
- **Balance Types:** Account balance classifications

#### Charges & Fees
- Interest Credit Method
- Tax Deduction at Source (TDS)
- Withdrawal Fees
- Premature Closure Penalties
- Charge Calculation Methods

#### Roles & Relationships
- Owner, Co-owner
- Nominee, Guardian
- Power of Attorney
- Authorized Signatory

---

## 🚀 API Specifications (Lab L5 Requirements)

### 1. Create Product - POST /api/products

**Endpoint:** `POST /api/products`

**Request Body:**
```json
{
  "productCode": "FD001",
  "productName": "Fixed Deposit - Short Term",
  "productType": "FIXED_DEPOSIT",
  "currency": "INR",
  "effectiveDate": "2025-07-01",
  "expiryDate": "2026-12-31",
  "minTermMonths": 3,
  "maxTermMonths": 36,
  "minAmount": 10000.00,
  "maxAmount": 10000000.00,
  "minInterestRate": 5.0,
  "maxInterestRate": 7.5,
  "status": "ACTIVE",
  "description": "Short-term fixed deposit with flexible tenure options",
  "isCompoundingAllowed": true,
  "isPrematureWithdrawalAllowed": true,
  "prematureWithdrawalPenalty": 1.0,
  "productRoles": [
    {
      "roleType": "OWNER",
      "minCount": 1,
      "maxCount": 1,
      "isMandatory": true
    },
    {
      "roleType": "NOMINEE",
      "minCount": 0,
      "maxCount": 3,
      "isMandatory": false
    }
  ],
  "productCharges": [
    {
      "chargeType": "INTEREST_CREDIT_FEE",
      "chargeCalculation": "PERCENTAGE",
      "chargeAmount": 0.5,
      "frequency": "MONTHLY"
    },
    {
      "chargeType": "TDS",
      "chargeCalculation": "PERCENTAGE",
      "chargeAmount": 10.0,
      "frequency": "ON_MATURITY"
    }
  ]
}
```

**Response: HTTP 201 Created**
```json
{
  "success": true,
  "message": "Product created successfully",
  "timestamp": "2025-11-05T15:30:00",
  "data": {
    "id": 1,
    "productCode": "FD001",
    "productName": "Fixed Deposit - Short Term",
    "productType": "FIXED_DEPOSIT",
    "currency": "INR",
    "effectiveDate": "2025-07-01",
    "expiryDate": "2026-12-31",
    "minTermMonths": 3,
    "maxTermMonths": 36,
    "minAmount": 10000.00,
    "maxAmount": 10000000.00,
    "minInterestRate": 5.0,
    "maxInterestRate": 7.5,
    "status": "ACTIVE",
    "createdAt": "2025-11-05T15:30:00",
    "updatedAt": "2025-11-05T15:30:00"
  }
}
```

---

### 2. Search Products - POST /api/products/search

**Endpoint:** `POST /api/products/search`

**Request Body:**
```json
{
  "productType": "FIXED_DEPOSIT",
  "status": "ACTIVE",
  "startDate": "2025-01-01",
  "endDate": "2025-12-31",
  "minAmount": 10000,
  "maxAmount": 1000000,
  "page": 0,
  "size": 10,
  "sortBy": "createdAt",
  "sortDirection": "DESC"
}
```

**Query Parameters Alternative:**
```
GET /api/products/date-range?startDate=2025-01-01&endDate=2025-12-31
```

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": null,
  "timestamp": "2025-11-05T15:35:00",
  "data": {
    "products": [
      {
        "id": 1,
        "productCode": "FD001",
        "productName": "Fixed Deposit - Short Term",
        "productType": "FIXED_DEPOSIT",
        "status": "ACTIVE",
        "minAmount": 10000.00,
        "maxAmount": 10000000.00,
        "minInterestRate": 5.0,
        "maxInterestRate": 7.5,
        "effectiveDate": "2025-07-01",
        "expiryDate": "2026-12-31"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

### 3. Update Product - PUT /api/products/{id}

**Endpoint:** `PUT /api/products/{id}`

**Request Body:**
```json
{
  "productName": "Fixed Deposit - Short Term (Updated)",
  "minInterestRate": 5.5,
  "maxInterestRate": 8.0,
  "description": "Updated interest rate range for competitive returns",
  "status": "ACTIVE"
}
```

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": "Product updated successfully",
  "timestamp": "2025-11-05T15:40:00",
  "data": {
    "id": 1,
    "productCode": "FD001",
    "productName": "Fixed Deposit - Short Term (Updated)",
    "minInterestRate": 5.5,
    "maxInterestRate": 8.0,
    "updatedAt": "2025-11-05T15:40:00"
  }
}
```

---

### 4. Get Product by ID - GET /api/products/{id}

**Endpoint:** `GET /api/products/{id}`

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": null,
  "timestamp": "2025-11-05T15:45:00",
  "data": {
    "id": 1,
    "productCode": "FD001",
    "productName": "Fixed Deposit - Short Term",
    "productType": "FIXED_DEPOSIT",
    "currency": "INR",
    "effectiveDate": "2025-07-01",
    "expiryDate": "2026-12-31",
    "minTermMonths": 3,
    "maxTermMonths": 36,
    "minAmount": 10000.00,
    "maxAmount": 10000000.00,
    "minInterestRate": 5.0,
    "maxInterestRate": 7.5,
    "status": "ACTIVE",
    "productRoles": [...],
    "productCharges": [...],
    "interestRates": [...]
  }
}
```

---

### 5. Get Product by Code - GET /api/products/code/{code}

**Endpoint:** `GET /api/products/code/FD001`

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": null,
  "timestamp": "2025-11-05T15:50:00",
  "data": {
    "productCode": "FD001",
    "productName": "Fixed Deposit - Short Term",
    "productType": "FIXED_DEPOSIT",
    // ... complete product details
  }
}
```

---

### 6. Get Products by Type - GET /api/products/type/{type}

**Endpoint:** `GET /api/products/type/FIXED_DEPOSIT`

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": null,
  "timestamp": "2025-11-05T15:55:00",
  "data": [
    {
      "id": 1,
      "productCode": "FD001",
      "productName": "Fixed Deposit - Short Term",
      "productType": "FIXED_DEPOSIT",
      "status": "ACTIVE"
    },
    {
      "id": 2,
      "productCode": "FD002",
      "productName": "Fixed Deposit - Long Term",
      "productType": "FIXED_DEPOSIT",
      "status": "ACTIVE"
    }
  ]
}
```

---

### 7. Get Active Products - GET /api/products/active

**Endpoint:** `GET /api/products/active`

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": null,
  "timestamp": "2025-11-05T16:00:00",
  "data": [
    {
      "id": 1,
      "productCode": "FD001",
      "productName": "Fixed Deposit - Short Term",
      "status": "ACTIVE"
    }
  ]
}
```

---

### 8. Get Currently Active Products - GET /api/products/currently-active

**Endpoint:** `GET /api/products/currently-active`

**Description:** Returns products that are ACTIVE and within their effective date range (today is between effectiveDate and expiryDate)

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": null,
  "timestamp": "2025-11-05T16:05:00",
  "data": [
    {
      "id": 1,
      "productCode": "FD001",
      "productName": "Fixed Deposit - Short Term",
      "effectiveDate": "2025-07-01",
      "expiryDate": "2026-12-31",
      "status": "ACTIVE"
    }
  ]
}
```

---

### 9. Update Product Status - PUT /api/products/{id}/status

**Endpoint:** `PUT /api/products/{id}/status?status=INACTIVE`

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": "Product status updated successfully",
  "timestamp": "2025-11-05T16:10:00",
  "data": {
    "id": 1,
    "productCode": "FD001",
    "status": "INACTIVE",
    "updatedAt": "2025-11-05T16:10:00"
  }
}
```

---

### 10. Delete Product - DELETE /api/products/{id}

**Endpoint:** `DELETE /api/products/{id}`

**Description:** Soft delete (sets status to CLOSED)

**Response: HTTP 200 OK**
```json
{
  "success": true,
  "message": "Product deleted successfully",
  "timestamp": "2025-11-05T16:15:00",
  "data": null
}
```

---

## 📋 Complete API Endpoint List

| Method | Endpoint | Description | Lab L5 Requirement |
|--------|----------|-------------|--------------------|
| POST | `/api/products` | Create new product | ✅ Section 4.1 |
| POST | `/api/products/search` | Search products with criteria | ✅ Section 4.2 |
| PUT | `/api/products/{id}` | Update product | ✅ Section 4.3 |
| GET | `/api/products/{id}` | Get product by ID | ✅ |
| GET | `/api/products/code/{code}` | Get product by code | ✅ |
| GET | `/api/products` | Get all products (paginated) | ✅ |
| GET | `/api/products/type/{type}` | Get products by type | ✅ Section 4.2 |
| GET | `/api/products/status/{status}` | Get products by status | ✅ |
| GET | `/api/products/active` | Get active products | ✅ |
| GET | `/api/products/currently-active` | Get currently active products | ✅ |
| GET | `/api/products/date-range` | Get products by date range | ✅ Section 4.2 |
| PUT | `/api/products/{id}/status` | Update product status | ✅ |
| DELETE | `/api/products/{id}` | Soft delete product | ✅ |
| DELETE | `/api/products/{id}/hard` | Hard delete product | ✅ |

---

## 🗄️ Database Schema

### products table
```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(50) UNIQUE NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_type ENUM('FIXED_DEPOSIT', 'TAX_SAVER_FD', 'SENIOR_CITIZEN_FD', 
                      'FLEXI_FD', 'CUMULATIVE_FD', 'NON_CUMULATIVE_FD') NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    description TEXT,
    
    -- Date Configuration
    effective_date DATE NOT NULL,
    expiry_date DATE,
    
    -- Term Configuration
    min_term_months INT NOT NULL,
    max_term_months INT NOT NULL,
    
    -- Amount Configuration
    min_amount DECIMAL(19,2) NOT NULL,
    max_amount DECIMAL(19,2),
    
    -- Interest Rate Configuration
    min_interest_rate DECIMAL(5,2) NOT NULL,
    max_interest_rate DECIMAL(5,2) NOT NULL,
    
    -- Product Flags
    is_compounding_allowed BOOLEAN DEFAULT FALSE,
    is_premature_withdrawal_allowed BOOLEAN DEFAULT FALSE,
    premature_withdrawal_penalty DECIMAL(5,2),
    
    -- Status
    status ENUM('DRAFT', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'CLOSED') DEFAULT 'DRAFT',
    
    -- Audit Fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    
    -- Indexes
    INDEX idx_product_code (product_code),
    INDEX idx_product_type (product_type),
    INDEX idx_status (status),
    INDEX idx_effective_date (effective_date),
    INDEX idx_created_at (created_at)
);
```

### product_roles table
```sql
CREATE TABLE product_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    role_type ENUM('OWNER', 'CO_OWNER', 'NOMINEE', 'GUARDIAN', 
                   'POWER_OF_ATTORNEY', 'AUTHORIZED_SIGNATORY') NOT NULL,
    min_count INT NOT NULL DEFAULT 0,
    max_count INT NOT NULL DEFAULT 1,
    is_mandatory BOOLEAN DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id)
);
```

### product_charges table
```sql
CREATE TABLE product_charges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    charge_type ENUM('INTEREST_CREDIT_FEE', 'TDS', 'WITHDRAWAL_FEE', 
                     'PREMATURE_CLOSURE_FEE', 'ACCOUNT_MAINTENANCE_FEE') NOT NULL,
    charge_calculation ENUM('FLAT', 'PERCENTAGE') NOT NULL,
    charge_amount DECIMAL(19,2) NOT NULL,
    frequency ENUM('ONE_TIME', 'DAILY', 'MONTHLY', 'QUARTERLY', 
                   'HALF_YEARLY', 'YEARLY', 'ON_MATURITY') NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id)
);
```

### interest_rates table (Product Interest Rate Matrix)
```sql
CREATE TABLE interest_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    
    -- Term Range
    min_term_months INT NOT NULL,
    max_term_months INT NOT NULL,
    
    -- Amount Range
    min_amount DECIMAL(19,2),
    max_amount DECIMAL(19,2),
    
    -- Customer Classification
    customer_classification ENUM('REGULAR', 'PREMIUM', 'VIP', 
                                  'SENIOR_CITIZEN', 'SUPER_SENIOR'),
    
    -- Interest Rate
    interest_rate DECIMAL(5,2) NOT NULL,
    additional_rate DECIMAL(5,2) DEFAULT 0.00,
    
    -- Effective Period
    effective_date DATE NOT NULL,
    expiry_date DATE,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_term_range (min_term_months, max_term_months),
    INDEX idx_effective_date (effective_date)
);
```

---

## 🏗️ Product Module Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  Product-Pricing Service                  │
│                      (Port 8084)                          │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────────────────────────────────────────┐    │
│  │         ProductController                       │    │
│  │  ├─ POST /api/products                          │    │
│  │  ├─ POST /api/products/search                   │    │
│  │  ├─ PUT /api/products/{id}                      │    │
│  │  ├─ GET /api/products/{id}                      │    │
│  │  ├─ GET /api/products/code/{code}               │    │
│  │  └─ GET /api/products/type/{type}               │    │
│  └─────────────────────────────────────────────────┘    │
│                         ↓                                 │
│  ┌─────────────────────────────────────────────────┐    │
│  │         ProductService                          │    │
│  │  ├─ Business Logic Validation                   │    │
│  │  ├─ Product Creation & Update                   │    │
│  │  ├─ Search & Filter Operations                  │    │
│  │  ├─ Status Management                           │    │
│  │  └─ Cache Management (Caffeine)                 │    │
│  └─────────────────────────────────────────────────┘    │
│                         ↓                                 │
│  ┌─────────────────────────────────────────────────┐    │
│  │         ProductRepository (JPA)                 │    │
│  │  ├─ findByProductCode()                         │    │
│  │  ├─ findByProductType()                         │    │
│  │  ├─ findByStatus()                              │    │
│  │  └─ Custom Query Methods                        │    │
│  └─────────────────────────────────────────────────┘    │
│                         ↓                                 │
│  ┌─────────────────────────────────────────────────┐    │
│  │              MySQL Database                      │    │
│  │         product_db (Auto-created)                │    │
│  └─────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

---

## 📊 Product Types & Use Cases

### Fixed Deposit Product Types

| Product Type | Description | Key Features |
|--------------|-------------|--------------|
| **FIXED_DEPOSIT** | Standard FD | Fixed tenure, guaranteed returns |
| **TAX_SAVER_FD** | Tax saving FD (80C) | 5-year lock-in, tax deduction |
| **SENIOR_CITIZEN_FD** | For senior citizens (60+) | Higher interest rates, flexible tenure |
| **FLEXI_FD** | Flexible FD with sweep facility | Overdraft against FD |
| **CUMULATIVE_FD** | Interest paid at maturity | Compounded interest |
| **NON_CUMULATIVE_FD** | Periodic interest payout | Monthly/quarterly interest |

---

## 🧪 Testing

### Sample Test Data - Create Product

```json
{
  "productCode": "FD001",
  "productName": "Fixed Deposit - 6 to 12 Months",
  "productType": "FIXED_DEPOSIT",
  "currency": "INR",
  "effectiveDate": "2025-01-01",
  "expiryDate": "2025-12-31",
  "minTermMonths": 6,
  "maxTermMonths": 12,
  "minAmount": 10000.00,
  "maxAmount": 10000000.00,
  "minInterestRate": 6.0,
  "maxInterestRate": 7.0,
  "status": "ACTIVE",
  "description": "Short-term fixed deposit with competitive interest rates",
  "isCompoundingAllowed": true,
  "isPrematureWithdrawalAllowed": true,
  "prematureWithdrawalPenalty": 1.0,
  "productRoles": [
    {
      "roleType": "OWNER",
      "minCount": 1,
      "maxCount": 1,
      "isMandatory": true,
      "description": "Primary account holder"
    },
    {
      "roleType": "NOMINEE",
      "minCount": 0,
      "maxCount": 3,
      "isMandatory": false,
      "description": "Beneficiary in case of death"
    }
  ],
  "productCharges": [
    {
      "chargeType": "TDS",
      "chargeCalculation": "PERCENTAGE",
      "chargeAmount": 10.0,
      "frequency": "ON_MATURITY",
      "description": "Tax Deduction at Source"
    }
  ],
  "interestRates": [
    {
      "minTermMonths": 6,
      "maxTermMonths": 9,
      "minAmount": 10000,
      "maxAmount": 100000,
      "customerClassification": "REGULAR",
      "interestRate": 6.0,
      "additionalRate": 0.0,
      "effectiveDate": "2025-01-01"
    },
    {
      "minTermMonths": 6,
      "maxTermMonths": 9,
      "minAmount": 10000,
      "maxAmount": 100000,
      "customerClassification": "SENIOR_CITIZEN",
      "interestRate": 6.0,
      "additionalRate": 0.5,
      "effectiveDate": "2025-01-01"
    },
    {
      "minTermMonths": 9,
      "maxTermMonths": 12,
      "minAmount": 10000,
      "maxAmount": 100000,
      "customerClassification": "REGULAR",
      "interestRate": 6.5,
      "additionalRate": 0.0,
      "effectiveDate": "2025-01-01"
    },
    {
      "minTermMonths": 9,
      "maxTermMonths": 12,
      "minAmount": 10000,
      "maxAmount": 100000,
      "customerClassification": "SENIOR_CITIZEN",
      "interestRate": 6.5,
      "additionalRate": 0.5,
      "effectiveDate": "2025-01-01"
    }
  ]
}
```

### Test Case 1: Create Product

**Request:**
```bash
curl -X POST http://localhost:8084/api/products \
  -H "Content-Type: application/json" \
  -d @product-test-data.json
```

**Expected: HTTP 201 Created**

---

### Test Case 2: Search Products by Type and Date Range

**Request:**
```bash
curl -X POST http://localhost:8084/api/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "productType": "FIXED_DEPOSIT",
    "startDate": "2025-01-01",
    "endDate": "2025-12-31"
  }'
```

**Expected: HTTP 200 OK with matching products**

---

### Test Case 3: Get Product by Code

**Request:**
```bash
curl -X GET http://localhost:8084/api/products/code/FD001
```

**Expected: HTTP 200 OK with product details**

---

### Test Case 4: Update Product

**Request:**
```bash
curl -X PUT http://localhost:8084/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{
    "productName": "Fixed Deposit - 6 to 12 Months (Updated)",
    "maxInterestRate": 7.5
  }'
```

**Expected: HTTP 200 OK with updated product**

---

### Test Case 5: Get Active Products

**Request:**
```bash
curl -X GET http://localhost:8084/api/products/active
```

**Expected: HTTP 200 OK with list of active products**

---

## 📝 Lab L5 Requirements Verification

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| **API Development** |||
| POST /api/product (Create) | ✅ | POST /api/products |
| GET /api/product/search (Filter) | ✅ | POST /api/products/search + GET /date-range |
| PUT /api/product (Update) | ✅ | PUT /api/products/{id} |
| **Product Structure** |||
| Basic Info (name, code, type, currency, dates) | ✅ | Complete implementation |
| Business Rules (terms, amounts, rates) | ✅ | Min/max configurations |
| Charges (interest, tax, fees) | ✅ | ProductCharges entity |
| Roles (owner, nominee, etc.) | ✅ | ProductRoles entity |
| **Query Capabilities** |||
| Filter by product type | ✅ | GET /type/{type} |
| Filter by date range | ✅ | GET /date-range |
| Filter by status | ✅ | GET /status/{status} |
| Get active products | ✅ | GET /active |
| **Documentation** |||
| API signatures | ✅ | Complete OpenAPI/Swagger docs |
| Sample inputs/outputs | ✅ | Documented in this file |
| Use-case validations | ✅ | Business logic in service layer |
| **Expected Output** |||
| Product stored with metadata | ✅ | MySQL persistence |
| Retrievable via queries | ✅ | Multiple query methods |
| Integration with FD/Calculator | ✅ | API ready for integration |

---

## 🔧 Technologies Used

| Component | Technology | Version |
|-----------|------------|---------|
| Framework | Spring Boot | 3.5.6 |
| Database | MySQL | 8.0.41 |
| ORM | JPA/Hibernate | 6.6.29 |
| Caching | Caffeine | - |
| API Documentation | Swagger/OpenAPI | 3.0 |
| Build Tool | Maven | 3.x |
| Java | OpenJDK | 17 |

---

## 🎯 Integration Points

### With Customer Module
- Customer classification determines applicable interest rates
- Products define eligible customer segments

### With FD Calculator Module
- Product interest rate matrix used for calculations
- Term and amount limits enforced

### With FD Account Module
- FD accounts created based on product definitions
- Product rules applied to account operations

---

## 🚀 Running the Service

### Start Product-Pricing Service

**PowerShell:**
```powershell
cd "d:\College\3rd Year\Lab - Banking Technology\bt dhruva\bt_khatam\credexa"
.\mvnw.cmd -pl product-pricing-service spring-boot:run
```

**Service Details:**
- **Port:** 8084
- **Context Path:** /api/products
- **Database:** product_db (auto-created)
- **Swagger UI:** http://localhost:8084/api/products/swagger-ui.html

---

## 📖 API Documentation Access

### Swagger UI
```
http://localhost:8084/api/products/swagger-ui.html
```

### OpenAPI JSON
```
http://localhost:8084/api/products/api-docs
```

---

## ✅ Lab L5 Status: **100% COMPLETE**

### Summary

**Implemented:**
1. ✅ Complete Product CRUD operations
2. ✅ Advanced search and filter capabilities
3. ✅ Product roles and charges management
4. ✅ Interest rate matrix configuration
5. ✅ Status management (DRAFT, ACTIVE, INACTIVE, etc.)
6. ✅ Date range queries
7. ✅ Pagination and sorting
8. ✅ Caching for performance
9. ✅ Complete Swagger/OpenAPI documentation
10. ✅ Comprehensive error handling

**All Lab L5 Requirements Met:**
- ✅ POST /api/product (Create Product)
- ✅ GET /api/product/search (Search Products)
- ✅ PUT /api/product (Update Product)
- ✅ Complete product structure with business rules, charges, and roles
- ✅ API documentation with sample inputs/outputs
- ✅ Ready for integration with FD and Calculator modules

---

**Last Updated:** November 5, 2025  
**Verified By:** GitHub Copilot  
**Service Port:** 8084  
**Ready for Testing:** ✅ YES
