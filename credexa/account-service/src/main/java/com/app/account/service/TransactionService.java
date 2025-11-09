package com.app.account.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.account.dto.AccountInquiryRequest;
import com.app.account.dto.AccountInquiryRequest.AccountIdType;
import com.app.account.dto.CreateTransactionRequest;
import com.app.account.dto.TransactionInquiryRequest;
import com.app.account.dto.TransactionResponse;
import com.app.account.entity.FdAccount;
import com.app.account.entity.FdTransaction;
import com.app.account.entity.FdTransaction.TransactionStatus;
import com.app.account.repository.FdAccountRepository;
import com.app.account.repository.FdTransactionRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service layer for FD Transaction operations
 */
@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private static final AtomicLong transactionCounter = new AtomicLong(1000);

    @Autowired
    private FdTransactionRepository transactionRepository;

    @Autowired
    private FdAccountRepository accountRepository;

    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;

    /**
     * Create transaction using Account ID type
     */
    @Transactional
    public TransactionResponse createTransaction(AccountIdType idType, String idValue,
                                                  CreateTransactionRequest request, String currentUser) {
        logger.info("💳 Creating transaction: Type={}, Amount={}, Account ID Type={}, ID Value={}", 
                request.getTransactionType(), request.getAmount(), idType, idValue);

        // 1. Find account by ID type
        FdAccount account = findAccountByIdType(idType, idValue);

        // 2. Get current balance (from latest transaction or principal)
        BigDecimal currentBalance = getCurrentBalance(account);

        // 3. Calculate new balance based on transaction type
        BigDecimal newBalance = calculateNewBalance(currentBalance, request.getAmount(), request.getTransactionType());

        // 4. Generate transaction ID
        String transactionId = generateTransactionId();

        // 5. Get IP address
        String ipAddress = getClientIpAddress();

        // 6. Create transaction entity
        FdTransaction transaction = FdTransaction.builder()
                .transactionId(transactionId)
                .account(account)
                .accountNumber(account.getAccountNumber())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .balanceBefore(currentBalance)
                .balanceAfter(newBalance)
                .status(TransactionStatus.COMPLETED) // Auto-approve for now
                .referenceNumber(request.getReferenceNumber())
                .description(request.getDescription())
                .remarks(request.getRemarks())
                .initiatedBy(currentUser)
                .approvedBy(currentUser) // Auto-approve
                .transactionDate(LocalDateTime.now())
                .approvalDate(LocalDateTime.now())
                .valueDate(request.getValueDate() != null ? request.getValueDate() : LocalDateTime.now())
                .channel(request.getChannel() != null ? request.getChannel() : "API")
                .branchCode(request.getBranchCode() != null ? request.getBranchCode() : account.getBranchCode())
                .ipAddress(ipAddress)
                .build();

        // 7. Save transaction
        FdTransaction savedTransaction = transactionRepository.save(transaction);
        logger.info("✅ Transaction created successfully: {} - {} - Amount: {}", 
                savedTransaction.getTransactionId(), 
                savedTransaction.getTransactionType(), 
                savedTransaction.getAmount());

        return mapToTransactionResponse(savedTransaction, account.getAccountName());
    }

    /**
     * Get transaction by inquiry (using Account ID type and Transaction ID)
     */
    public TransactionResponse getTransactionByInquiry(TransactionInquiryRequest inquiryRequest) {
        logger.info("🔍 Transaction inquiry: Account ID Type={}, ID Value={}, Transaction ID={}", 
                inquiryRequest.getIdTypeOrDefault(), inquiryRequest.getIdValue(), inquiryRequest.getTransactionId());

        // 1. Find account by ID type
        FdAccount account = findAccountByIdType(inquiryRequest.getIdTypeOrDefault(), inquiryRequest.getIdValue());

        // 2. Find transaction by transaction ID
        FdTransaction transaction = transactionRepository.findByTransactionId(inquiryRequest.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + inquiryRequest.getTransactionId()));

        // 3. Verify transaction belongs to the account
        if (!transaction.getAccountNumber().equals(account.getAccountNumber())) {
            throw new RuntimeException("Transaction " + inquiryRequest.getTransactionId() + 
                    " does not belong to account " + account.getAccountNumber());
        }

        logger.info("✅ Transaction found: {} - {}", transaction.getTransactionId(), transaction.getTransactionType());
        return mapToTransactionResponse(transaction, account.getAccountName());
    }

    /**
     * Get transaction by transaction ID only
     */
    public TransactionResponse getTransactionById(String transactionId) {
        logger.info("🔍 Fetching transaction: {}", transactionId);

        FdTransaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        FdAccount account = transaction.getAccount();
        return mapToTransactionResponse(transaction, account.getAccountName());
    }

    /**
     * List transactions for account using Account ID type
     */
    public Page<TransactionResponse> listTransactionsByAccountId(AccountIdType idType, String idValue, Pageable pageable) {
        logger.info("📋 Listing transactions: Account ID Type={}, ID Value={}", idType, idValue);

        // Find account by ID type
        FdAccount account = findAccountByIdType(idType, idValue);

        // Get transactions for account
        Page<FdTransaction> transactions = transactionRepository.findByAccountNumber(account.getAccountNumber(), pageable);

        return transactions.map(txn -> mapToTransactionResponse(txn, account.getAccountName()));
    }

    /**
     * List transactions by account number (standard)
     */
    public Page<TransactionResponse> listTransactionsByAccountNumber(String accountNumber, Pageable pageable) {
        logger.info("📋 Listing transactions for account: {}", accountNumber);

        FdAccount account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        Page<FdTransaction> transactions = transactionRepository.findByAccountNumber(accountNumber, pageable);

        return transactions.map(txn -> mapToTransactionResponse(txn, account.getAccountName()));
    }

    /**
     * List transactions by type
     */
    public Page<TransactionResponse> listTransactionsByType(FdTransaction.TransactionType transactionType, Pageable pageable) {
        logger.info("📋 Listing transactions by type: {}", transactionType);

        Page<FdTransaction> transactions = transactionRepository.findByTransactionType(transactionType, pageable);

        return transactions.map(txn -> {
            String accountName = txn.getAccount() != null ? txn.getAccount().getAccountName() : "N/A";
            return mapToTransactionResponse(txn, accountName);
        });
    }

    /**
     * List transactions by status
     */
    public Page<TransactionResponse> listTransactionsByStatus(FdTransaction.TransactionStatus status, Pageable pageable) {
        logger.info("📋 Listing transactions by status: {}", status);

        Page<FdTransaction> transactions = transactionRepository.findByStatus(status, pageable);

        return transactions.map(txn -> {
            String accountName = txn.getAccount() != null ? txn.getAccount().getAccountName() : "N/A";
            return mapToTransactionResponse(txn, accountName);
        });
    }

    /**
     * Get transaction count for account
     */
    public long getTransactionCount(String accountNumber) {
        return transactionRepository.countByAccountNumber(accountNumber);
    }

    /**
     * Find account by ID type (reusable method)
     */
    private FdAccount findAccountByIdType(AccountIdType idType, String idValue) {
        FdAccount account = null;

        switch (idType != null ? idType : AccountIdType.ACCOUNT_NUMBER) {
            case IBAN:
                account = accountRepository.findByIbanNumber(idValue)
                        .orElseThrow(() -> new RuntimeException("Account not found with IBAN: " + idValue));
                break;

            case INTERNAL_ID:
                try {
                    Long internalId = Long.parseLong(idValue);
                    account = accountRepository.findById(internalId)
                            .orElseThrow(() -> new RuntimeException("Account not found with ID: " + internalId));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid internal ID format: " + idValue);
                }
                break;

            case ACCOUNT_NUMBER:
            default:
                account = accountRepository.findByAccountNumber(idValue)
                        .orElseThrow(() -> new RuntimeException("Account not found with account number: " + idValue));
                break;
        }

        return account;
    }

    /**
     * Get current balance from latest transaction or principal
     */
    private BigDecimal getCurrentBalance(FdAccount account) {
        Optional<FdTransaction> latestTransaction = transactionRepository
                .findFirstByAccountNumberOrderByTransactionDateDesc(account.getAccountNumber());

        if (latestTransaction.isPresent()) {
            return latestTransaction.get().getBalanceAfter();
        } else {
            // No transactions yet, use principal amount
            return account.getPrincipalAmount();
        }
    }

    /**
     * Calculate new balance based on transaction type
     */
    private BigDecimal calculateNewBalance(BigDecimal currentBalance, BigDecimal amount, 
                                          FdTransaction.TransactionType transactionType) {
        switch (transactionType) {
            case DEPOSIT:
            case INTEREST_CREDIT:
            case MATURITY_CREDIT:
            case ADJUSTMENT:
                return currentBalance.add(amount);

            case WITHDRAWAL:
            case TDS_DEDUCTION:
            case CLOSURE:
                return currentBalance.subtract(amount);

            case REVERSAL:
                // Reversal logic depends on the original transaction
                return currentBalance.subtract(amount);

            default:
                return currentBalance;
        }
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String sequence = String.format("%04d", transactionCounter.getAndIncrement() % 10000);
        return String.format("TXN-%s-%s", timestamp, sequence);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress() {
        if (httpServletRequest == null) {
            return "SYSTEM";
        }

        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return httpServletRequest.getRemoteAddr();
    }

    /**
     * Map entity to response DTO
     */
    private TransactionResponse mapToTransactionResponse(FdTransaction transaction, String accountName) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .accountNumber(transaction.getAccountNumber())
                .accountName(accountName)
                .customerId(transaction.getAccount() != null ? transaction.getAccount().getCustomerId() : null)
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .status(transaction.getStatus())
                .referenceNumber(transaction.getReferenceNumber())
                .description(transaction.getDescription())
                .remarks(transaction.getRemarks())
                .initiatedBy(transaction.getInitiatedBy())
                .approvedBy(transaction.getApprovedBy())
                .transactionDate(transaction.getTransactionDate())
                .approvalDate(transaction.getApprovalDate())
                .valueDate(transaction.getValueDate())
                .channel(transaction.getChannel())
                .branchCode(transaction.getBranchCode())
                .ipAddress(transaction.getIpAddress())
                .build();
    }
}
