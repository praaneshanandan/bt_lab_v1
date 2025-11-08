package com.app.account.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.account.client.CalculatorServiceClient;
import com.app.account.client.CustomerServiceClient;
import com.app.account.client.ProductServiceClient;
import com.app.account.dto.AccountInquiryRequest;
import com.app.account.dto.AccountResponse;
import com.app.account.dto.BalanceResponse;
import com.app.account.dto.CreateAccountRequest;
import com.app.account.dto.external.CalculationRequest;
import com.app.account.dto.external.CalculationResponse;
import com.app.account.dto.external.CustomerDto;
import com.app.account.dto.external.ProductDto;
import com.app.account.entity.FdAccount;
import com.app.account.repository.FdAccountRepository;
import com.app.account.util.AccountNumberGenerator;

/**
 * Service layer for FD Account operations
 */
@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    @Autowired
    private FdAccountRepository accountRepository;

    @Autowired
    private CustomerServiceClient customerServiceClient;

    @Autowired
    private ProductServiceClient productServiceClient;

    @Autowired
    private CalculatorServiceClient calculatorServiceClient;

    @Autowired
    private AccountNumberGenerator accountNumberGenerator;

    /**
     * Create FD Account - VERSION 1: All values defaulted from product
     */
    @Transactional
    public AccountResponse createAccountWithDefaults(CreateAccountRequest request, String currentUser) {
        logger.info("📝 Creating FD account with default values for customer: {}", request.getCustomerId());

        // 1. Validate and fetch customer
        CustomerDto customer = customerServiceClient.getCustomerById(request.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("Customer not found with ID: " + request.getCustomerId());
        }

        // 2. Validate and fetch product
        ProductDto product = productServiceClient.getProductByCode(request.getProductCode());
        if (product == null || !product.getActive()) {
            throw new RuntimeException("Product not found or inactive: " + request.getProductCode());
        }

        // 3. Validate product constraints (use defaults)
        validateProductConstraints(request, product);

        // 4. Generate account numbers
        String accountNumber = accountNumberGenerator.generateStandardAccountNumber();
        String ibanNumber = accountNumberGenerator.generateIBANAccountNumber();

        // 5. Calculate maturity using calculator-service
        CalculationRequest calculationRequest = CalculationRequest.builder()
                .principalAmount(request.getPrincipalAmount())
                .interestRate(product.getBaseInterestRate()) // Use product's base rate
                .tenureMonths(request.getTermMonths())
                .calculationType(product.getInterestCalculationMethod() != null ? 
                        product.getInterestCalculationMethod() : "SIMPLE")
                .compoundingFrequency("QUARTERLY") // Default
                .startDate(request.getEffectiveDate())
                .tdsApplicable(product.getTdsApplicable() != null ? product.getTdsApplicable() : false)
                .tdsRate(product.getTdsRate() != null ? product.getTdsRate() : BigDecimal.ZERO)
                .customerId(customer.getId())
                .build();

        CalculationResponse calculation = calculatorServiceClient.calculateMaturity(calculationRequest);

        // 6. Create FD Account entity
        FdAccount account = FdAccount.builder()
                .accountNumber(accountNumber)
                .ibanNumber(ibanNumber)
                .accountName(request.getAccountName())
                // Customer details (denormalized)
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerMobile(customer.getMobile())
                // Product details (denormalized)
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .productType(product.getProductType())
                // Financial details
                .principalAmount(request.getPrincipalAmount())
                .interestRate(product.getBaseInterestRate())
                .termMonths(request.getTermMonths())
                .maturityAmount(calculation.getMaturityAmount())
                .interestEarned(calculation.getInterestEarned())
                // Dates
                .effectiveDate(request.getEffectiveDate())
                .maturityDate(calculation.getMaturityDate())
                // Calculation details
                .calculationType(calculation.getCalculationType())
                .compoundingFrequency(calculation.getCompoundingFrequency())
                // TDS
                .tdsRate(calculation.getTdsRate())
                .tdsAmount(calculation.getTdsAmount())
                .tdsApplicable(product.getTdsApplicable())
                // Status
                .status(FdAccount.AccountStatus.ACTIVE)
                // Branch
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                // Metadata
                .remarks(request.getRemarks())
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        // 7. Save account
        FdAccount savedAccount = accountRepository.save(account);
        logger.info("✅ Account created successfully: {} (IBAN: {})", savedAccount.getAccountNumber(), savedAccount.getIbanNumber());

        return mapToAccountResponse(savedAccount);
    }

    /**
     * Create FD Account - VERSION 2: Customized interest rate within product purview
     */
    @Transactional
    public AccountResponse createAccountWithCustomization(CreateAccountRequest request, 
                                                          BigDecimal customInterestRate,
                                                          String customCalculationType,
                                                          String customCompoundingFrequency,
                                                          String currentUser) {
        logger.info("📝 Creating FD account with customized values for customer: {}", request.getCustomerId());

        // 1. Validate and fetch customer
        CustomerDto customer = customerServiceClient.getCustomerById(request.getCustomerId());
        if (customer == null) {
            throw new RuntimeException("Customer not found with ID: " + request.getCustomerId());
        }

        // 2. Validate and fetch product
        ProductDto product = productServiceClient.getProductByCode(request.getProductCode());
        if (product == null || !product.getActive()) {
            throw new RuntimeException("Product not found or inactive: " + request.getProductCode());
        }

        // 3. Validate product constraints
        validateProductConstraints(request, product);

        // 4. Validate custom interest rate is within product purview
        BigDecimal finalInterestRate = validateAndGetInterestRate(customInterestRate, product);

        // 5. Generate account numbers
        String accountNumber = accountNumberGenerator.generateStandardAccountNumber();
        String ibanNumber = accountNumberGenerator.generateIBANAccountNumber();

        // 6. Calculate maturity with custom values
        CalculationRequest calculationRequest = CalculationRequest.builder()
                .principalAmount(request.getPrincipalAmount())
                .interestRate(finalInterestRate)
                .tenureMonths(request.getTermMonths())
                .calculationType(customCalculationType != null ? customCalculationType : 
                        (product.getInterestCalculationMethod() != null ? product.getInterestCalculationMethod() : "SIMPLE"))
                .compoundingFrequency(customCompoundingFrequency != null ? customCompoundingFrequency : "QUARTERLY")
                .startDate(request.getEffectiveDate())
                .tdsApplicable(product.getTdsApplicable() != null ? product.getTdsApplicable() : false)
                .tdsRate(product.getTdsRate() != null ? product.getTdsRate() : BigDecimal.ZERO)
                .customerId(customer.getId())
                .build();

        CalculationResponse calculation = calculatorServiceClient.calculateMaturity(calculationRequest);

        // 7. Create FD Account entity
        FdAccount account = FdAccount.builder()
                .accountNumber(accountNumber)
                .ibanNumber(ibanNumber)
                .accountName(request.getAccountName())
                // Customer details
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerMobile(customer.getMobile())
                // Product details
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .productType(product.getProductType())
                // Financial details (with custom rate)
                .principalAmount(request.getPrincipalAmount())
                .interestRate(finalInterestRate)
                .termMonths(request.getTermMonths())
                .maturityAmount(calculation.getMaturityAmount())
                .interestEarned(calculation.getInterestEarned())
                // Dates
                .effectiveDate(request.getEffectiveDate())
                .maturityDate(calculation.getMaturityDate())
                // Calculation details (with custom settings)
                .calculationType(calculation.getCalculationType())
                .compoundingFrequency(calculation.getCompoundingFrequency())
                // TDS
                .tdsRate(calculation.getTdsRate())
                .tdsAmount(calculation.getTdsAmount())
                .tdsApplicable(product.getTdsApplicable())
                // Status
                .status(FdAccount.AccountStatus.ACTIVE)
                // Branch
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                // Metadata
                .remarks(request.getRemarks() != null ? request.getRemarks() + " [Custom Rate Applied]" : "[Custom Rate Applied]")
                .createdBy(currentUser)
                .updatedBy(currentUser)
                .build();

        // 8. Save account
        FdAccount savedAccount = accountRepository.save(account);
        logger.info("✅ Account created with customization: {} (Rate: {}%)", savedAccount.getAccountNumber(), finalInterestRate);

        return mapToAccountResponse(savedAccount);
    }

    /**
     * Account Inquiry - Find by flexible ID type
     */
    public AccountResponse getAccountByInquiry(AccountInquiryRequest inquiryRequest) {
        logger.info("🔍 Account inquiry: Type={}, Value={}", 
                inquiryRequest.getIdTypeOrDefault(), inquiryRequest.getIdValue());

        FdAccount account = null;

        switch (inquiryRequest.getIdTypeOrDefault()) {
            case IBAN:
                account = accountRepository.findByIbanNumber(inquiryRequest.getIdValue())
                        .orElseThrow(() -> new RuntimeException("Account not found with IBAN: " + inquiryRequest.getIdValue()));
                break;

            case INTERNAL_ID:
                try {
                    Long internalId = Long.parseLong(inquiryRequest.getIdValue());
                    account = accountRepository.findById(internalId)
                            .orElseThrow(() -> new RuntimeException("Account not found with ID: " + internalId));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid internal ID format: " + inquiryRequest.getIdValue());
                }
                break;

            case ACCOUNT_NUMBER:
            default:
                account = accountRepository.findByAccountNumber(inquiryRequest.getIdValue())
                        .orElseThrow(() -> new RuntimeException("Account not found with account number: " + inquiryRequest.getIdValue()));
                break;
        }

        logger.info("✅ Account found: {}", account.getAccountNumber());
        return mapToAccountResponse(account);
    }

    /**
     * Get account by standard account number
     */
    public AccountResponse getAccountByNumber(String accountNumber) {
        logger.info("🔍 Fetching account: {}", accountNumber);

        FdAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        return mapToAccountResponse(account);
    }

    /**
     * List accounts with pagination
     */
    public Page<AccountResponse> listAccounts(Pageable pageable) {
        logger.info("📋 Listing accounts: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<FdAccount> accounts = accountRepository.findAll(pageable);
        return accounts.map(this::mapToAccountResponse);
    }

    /**
     * List accounts by customer
     */
    public Page<AccountResponse> listAccountsByCustomer(Long customerId, Pageable pageable) {
        logger.info("📋 Listing accounts for customer: {}", customerId);

        Page<FdAccount> accounts = accountRepository.findByCustomerId(customerId, pageable);
        return accounts.map(this::mapToAccountResponse);
    }

    /**
     * Get account balance
     */
    public BalanceResponse getAccountBalance(String accountNumber) {
        logger.info("💰 Fetching balance for account: {}", accountNumber);

        FdAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        long daysToMaturity = account.getMaturityDate() != null ? 
                ChronoUnit.DAYS.between(LocalDate.now(), account.getMaturityDate()) : 0;

        BigDecimal netAmount = account.getMaturityAmount();
        if (account.getTdsAmount() != null) {
            netAmount = netAmount.subtract(account.getTdsAmount());
        }

        return BalanceResponse.builder()
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .principalAmount(account.getPrincipalAmount())
                .interestEarned(account.getInterestEarned())
                .maturityAmount(account.getMaturityAmount())
                .tdsAmount(account.getTdsAmount())
                .netAmount(netAmount)
                .status(account.getStatus())
                .effectiveDate(account.getEffectiveDate())
                .maturityDate(account.getMaturityDate())
                .daysToMaturity(daysToMaturity)
                .build();
    }

    /**
     * Validate product constraints
     */
    private void validateProductConstraints(CreateAccountRequest request, ProductDto product) {
        // Validate amount range
        if (product.getMinAmount() != null && request.getPrincipalAmount().compareTo(product.getMinAmount()) < 0) {
            throw new RuntimeException(String.format("Principal amount %.2f is below minimum %.2f for product %s",
                    request.getPrincipalAmount(), product.getMinAmount(), product.getProductCode()));
        }

        if (product.getMaxAmount() != null && request.getPrincipalAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new RuntimeException(String.format("Principal amount %.2f exceeds maximum %.2f for product %s",
                    request.getPrincipalAmount(), product.getMaxAmount(), product.getProductCode()));
        }

        // Validate term range
        if (product.getMinTermMonths() != null && request.getTermMonths() < product.getMinTermMonths()) {
            throw new RuntimeException(String.format("Term %d months is below minimum %d months for product %s",
                    request.getTermMonths(), product.getMinTermMonths(), product.getProductCode()));
        }

        if (product.getMaxTermMonths() != null && request.getTermMonths() > product.getMaxTermMonths()) {
            throw new RuntimeException(String.format("Term %d months exceeds maximum %d months for product %s",
                    request.getTermMonths(), product.getMaxTermMonths(), product.getProductCode()));
        }
    }

    /**
     * Validate and get interest rate (custom must be within ±2% of base rate)
     */
    private BigDecimal validateAndGetInterestRate(BigDecimal customRate, ProductDto product) {
        BigDecimal baseRate = product.getBaseInterestRate();

        if (customRate == null) {
            return baseRate; // Use base rate if no custom rate provided
        }

        // Allow ±2% variation from base rate
        BigDecimal minAllowed = baseRate.subtract(new BigDecimal("2.0"));
        BigDecimal maxAllowed = baseRate.add(new BigDecimal("2.0"));

        if (customRate.compareTo(minAllowed) < 0 || customRate.compareTo(maxAllowed) > 0) {
            throw new RuntimeException(String.format(
                    "Custom interest rate %.2f%% is outside allowed range (%.2f%% - %.2f%%) for product %s (base: %.2f%%)",
                    customRate, minAllowed, maxAllowed, product.getProductCode(), baseRate));
        }

        logger.info("✅ Custom interest rate validated: {}% (base: {}%)", customRate, baseRate);
        return customRate;
    }

    /**
     * Map entity to response DTO
     */
    private AccountResponse mapToAccountResponse(FdAccount account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .ibanNumber(account.getIbanNumber())
                .accountName(account.getAccountName())
                .customerId(account.getCustomerId())
                .customerName(account.getCustomerName())
                .customerEmail(account.getCustomerEmail())
                .customerMobile(account.getCustomerMobile())
                .productCode(account.getProductCode())
                .productName(account.getProductName())
                .productType(account.getProductType())
                .principalAmount(account.getPrincipalAmount())
                .interestRate(account.getInterestRate())
                .termMonths(account.getTermMonths())
                .maturityAmount(account.getMaturityAmount())
                .interestEarned(account.getInterestEarned())
                .effectiveDate(account.getEffectiveDate())
                .maturityDate(account.getMaturityDate())
                .closureDate(account.getClosureDate())
                .calculationType(account.getCalculationType())
                .compoundingFrequency(account.getCompoundingFrequency())
                .tdsRate(account.getTdsRate())
                .tdsAmount(account.getTdsAmount())
                .tdsApplicable(account.getTdsApplicable())
                .status(account.getStatus())
                .branchCode(account.getBranchCode())
                .branchName(account.getBranchName())
                .remarks(account.getRemarks())
                .createdBy(account.getCreatedBy())
                .updatedBy(account.getUpdatedBy())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
