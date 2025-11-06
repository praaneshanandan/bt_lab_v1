package com.app.fdaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.fdaccount.dto.FDPortfolioDto;
import com.app.fdaccount.dto.FDReportDto;
import com.app.fdaccount.dto.InterestTransactionReportDto;
import com.app.fdaccount.dto.MaturitySummaryDto;
import com.app.fdaccount.entity.AccountBalance;
import com.app.fdaccount.entity.AccountRole;
import com.app.fdaccount.entity.AccountTransaction;
import com.app.fdaccount.entity.FdAccount;
import com.app.fdaccount.enums.AccountStatus;
import com.app.fdaccount.enums.TransactionType;
import com.app.fdaccount.repository.FdAccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating FD reports
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final FdAccountRepository accountRepository;

    /**
     * Get FD Summary Report (Admin/Bank Officer)
     * Aggregated data grouped by product code
     */
    @Transactional(readOnly = true)
    public List<FDReportDto> getFDSummary() {
        log.info("Generating FD summary report");

        List<FdAccount> allAccounts = accountRepository.findAll();

        // Group by product code and aggregate
        return allAccounts.stream()
                .collect(Collectors.groupingBy(FdAccount::getProductCode))
                .entrySet()
                .stream()
                .map(entry -> {
                    String productCode = entry.getKey();
                    List<FdAccount> accounts = entry.getValue();

                    // Get product name from first account
                    String productName = accounts.isEmpty() ? "" : accounts.get(0).getProductName();

                    // Calculate aggregates
                    long totalAccounts = accounts.size();
                    
                    BigDecimal totalPrincipal = accounts.stream()
                            .map(FdAccount::getPrincipalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalInterestAccrued = accounts.stream()
                            .map(this::getLatestInterestBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal totalMaturityAmount = totalPrincipal.add(totalInterestAccrued);

                    long activeAccounts = accounts.stream()
                            .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                            .count();

                    long maturedAccounts = accounts.stream()
                            .filter(a -> a.getStatus() == AccountStatus.MATURED)
                            .count();

                    long closedAccounts = accounts.stream()
                            .filter(a -> a.getStatus() == AccountStatus.CLOSED)
                            .count();

                    return FDReportDto.builder()
                            .productCode(productCode)
                            .productName(productName)
                            .totalAccounts(totalAccounts)
                            .totalPrincipal(totalPrincipal)
                            .totalInterestAccrued(totalInterestAccrued)
                            .totalMaturityAmount(totalMaturityAmount)
                            .activeAccounts(activeAccounts)
                            .maturedAccounts(maturedAccounts)
                            .closedAccounts(closedAccounts)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get Customer Portfolio Report
     * All FD accounts for a specific customer
     */
    @Transactional(readOnly = true)
    public List<FDPortfolioDto> getPortfolioForCustomer(Long customerId) {
        log.info("Generating portfolio report for customer: {}", customerId);

        List<FdAccount> accounts = accountRepository.findByCustomerId(customerId);

        return accounts.stream()
                .map(this::mapToPortfolioDto)
                .collect(Collectors.toList());
    }

    /**
     * Get Interest Transaction History
     * All interest credit transactions for a customer
     */
    @Transactional(readOnly = true)
    public List<InterestTransactionReportDto> getInterestTransactionHistory(Long customerId, 
                                                                              LocalDate fromDate, 
                                                                              LocalDate toDate) {
        log.info("Generating interest transaction report for customer: {} from {} to {}", 
                 customerId, fromDate, toDate);

        List<FdAccount> accounts = accountRepository.findByCustomerId(customerId);

        return accounts.stream()
                .flatMap(account -> account.getTransactions().stream()
                        .filter(txn -> txn.getTransactionType() == TransactionType.INTEREST_CREDIT)
                        .filter(txn -> !txn.getTransactionDate().isBefore(fromDate))
                        .filter(txn -> !txn.getTransactionDate().isAfter(toDate))
                        .filter(txn -> !txn.getIsReversed())
                        .map(txn -> mapToInterestTransactionDto(txn, account)))
                .collect(Collectors.toList());
    }

    /**
     * Get Maturity Summary Report
     * All accounts maturing within date range
     */
    @Transactional(readOnly = true)
    public List<MaturitySummaryDto> getMaturitySummary(LocalDate fromDate, LocalDate toDate) {
        log.info("Generating maturity summary report from {} to {}", fromDate, toDate);

        List<FdAccount> accounts = accountRepository.findAccountsMaturingBetween(fromDate, toDate);

        return accounts.stream()
                .map(this::mapToMaturitySummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Get Maturity Summary for specific customer
     */
    @Transactional(readOnly = true)
    public List<MaturitySummaryDto> getMaturitySummaryForCustomer(Long customerId, 
                                                                    LocalDate fromDate, 
                                                                    LocalDate toDate) {
        log.info("Generating maturity summary report for customer: {} from {} to {}", 
                 customerId, fromDate, toDate);

        List<FdAccount> accounts = accountRepository.findByCustomerId(customerId);

        return accounts.stream()
                .filter(account -> account.getMaturityDate() != null)
                .filter(account -> !account.getMaturityDate().isBefore(fromDate))
                .filter(account -> !account.getMaturityDate().isAfter(toDate))
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .map(this::mapToMaturitySummaryDto)
                .collect(Collectors.toList());
    }

    // Helper methods

    /**
     * Get latest interest balance for an account
     */
    private BigDecimal getLatestInterestBalance(FdAccount account) {
        return account.getBalances().stream()
                .filter(b -> "INTEREST_ACCRUED".equals(b.getBalanceType()))
                .max((b1, b2) -> b1.getAsOfDate().compareTo(b2.getAsOfDate()))
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get latest available balance for an account
     */
    private BigDecimal getLatestAvailableBalance(FdAccount account) {
        return account.getBalances().stream()
                .filter(b -> "AVAILABLE".equals(b.getBalanceType()))
                .max((b1, b2) -> b1.getAsOfDate().compareTo(b2.getAsOfDate()))
                .map(AccountBalance::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get primary customer name from account roles
     */
    private String getPrimaryCustomerName(FdAccount account) {
        return account.getRoles().stream()
                .filter(role -> Boolean.TRUE.equals(role.getIsPrimary()))
                .findFirst()
                .map(AccountRole::getCustomerName)
                .orElse("N/A");
    }

    /**
     * Get primary customer ID from account roles
     */
    private Long getPrimaryCustomerId(FdAccount account) {
        return account.getRoles().stream()
                .filter(role -> Boolean.TRUE.equals(role.getIsPrimary()))
                .findFirst()
                .map(AccountRole::getCustomerId)
                .orElse(null);
    }

    /**
     * Calculate days to maturity
     */
    private Integer calculateDaysToMaturity(LocalDate maturityDate) {
        if (maturityDate == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), maturityDate);
        return (int) days;
    }

    /**
     * Map account to portfolio DTO
     */
    private FDPortfolioDto mapToPortfolioDto(FdAccount account) {
        BigDecimal interestRate = account.getCustomInterestRate() != null ? 
                account.getCustomInterestRate() : account.getInterestRate();

        return FDPortfolioDto.builder()
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .productCode(account.getProductCode())
                .productName(account.getProductName())
                .status(account.getStatus().name())
                .principalAmount(account.getPrincipalAmount())
                .interestRate(interestRate)
                .termMonths(account.getTermMonths())
                .maturityAmount(account.getMaturityAmount())
                .interestAccrued(getLatestInterestBalance(account))
                .availableBalance(getLatestAvailableBalance(account))
                .effectiveDate(account.getEffectiveDate())
                .maturityDate(account.getMaturityDate())
                .daysToMaturity(calculateDaysToMaturity(account.getMaturityDate()))
                .branchCode(account.getBranchCode())
                .branchName(account.getBranchName())
                .build();
    }

    /**
     * Map transaction to interest transaction DTO
     */
    private InterestTransactionReportDto mapToInterestTransactionDto(AccountTransaction txn, 
                                                                       FdAccount account) {
        return InterestTransactionReportDto.builder()
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .transactionReference(txn.getTransactionReference())
                .transactionType(txn.getTransactionType().name())
                .amount(txn.getAmount())
                .transactionDate(txn.getTransactionDate())
                .valueDate(txn.getValueDate())
                .principalBalanceAfter(txn.getPrincipalBalanceAfter())
                .interestBalanceAfter(txn.getInterestBalanceAfter())
                .totalBalanceAfter(txn.getTotalBalanceAfter())
                .description(txn.getDescription())
                .build();
    }

    /**
     * Map account to maturity summary DTO
     */
    private MaturitySummaryDto mapToMaturitySummaryDto(FdAccount account) {
        BigDecimal interestRate = account.getCustomInterestRate() != null ? 
                account.getCustomInterestRate() : account.getInterestRate();

        return MaturitySummaryDto.builder()
                .accountNumber(account.getAccountNumber())
                .accountName(account.getAccountName())
                .customerId(getPrimaryCustomerId(account))
                .customerName(getPrimaryCustomerName(account))
                .productCode(account.getProductCode())
                .productName(account.getProductName())
                .principalAmount(account.getPrincipalAmount())
                .interestAccrued(getLatestInterestBalance(account))
                .maturityAmount(account.getMaturityAmount())
                .interestRate(interestRate)
                .termMonths(account.getTermMonths())
                .effectiveDate(account.getEffectiveDate())
                .maturityDate(account.getMaturityDate())
                .daysToMaturity(calculateDaysToMaturity(account.getMaturityDate()))
                .maturityInstruction(account.getMaturityInstruction() != null ? 
                        account.getMaturityInstruction().name() : null)
                .branchCode(account.getBranchCode())
                .status(account.getStatus().name())
                .build();
    }
}
