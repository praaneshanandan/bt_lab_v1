package com.app.fdaccount.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Interest Service
 * Handles interest calculation formulas for FD accounts
 * Supports both Simple and Compound interest calculations
 */
@Slf4j
@Service
public class InterestService {

    /**
     * Calculate simple interest
     * Formula: Interest = (Principal * Rate * TermMonths) / (12 * 100)
     * 
     * @param principal The principal amount
     * @param rate Annual interest rate in percentage
     * @param termMonths Term in months
     * @return Calculated interest amount
     */
    public BigDecimal calculateSimpleInterest(BigDecimal principal, BigDecimal rate, int termMonths) {
        log.debug("Calculating simple interest - Principal: {}, Rate: {}%, Term: {} months", 
                principal, rate, termMonths);
        
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Term must be positive");
        }
        
        // Interest = (Principal * Rate * TermMonths) / (12 * 100)
        BigDecimal interest = principal
                .multiply(rate)
                .multiply(new BigDecimal(termMonths))
                .divide(new BigDecimal(12 * 100), 2, RoundingMode.HALF_UP);
        
        log.debug("Calculated simple interest: {}", interest);
        return interest;
    }

    /**
     * Calculate compound interest (monthly compounding)
     * Formula: MaturityAmount = Principal × (1 + (Rate / (12 × 100))) ^ TermMonths
     * Interest = MaturityAmount - Principal
     * 
     * @param principal The principal amount
     * @param rate Annual interest rate in percentage
     * @param termMonths Term in months
     * @return Calculated interest amount
     */
    public BigDecimal calculateCompoundInterest(BigDecimal principal, BigDecimal rate, int termMonths) {
        log.debug("Calculating compound interest - Principal: {}, Rate: {}%, Term: {} months", 
                principal, rate, termMonths);
        
        if (principal == null || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Principal must be positive");
        }
        
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        
        if (termMonths <= 0) {
            throw new IllegalArgumentException("Term must be positive");
        }
        
        // Monthly interest rate: Rate / (12 × 100)
        BigDecimal monthlyRate = rate.divide(new BigDecimal(1200), 10, RoundingMode.HALF_UP);
        
        // Calculate (1 + monthlyRate)
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        
        // Calculate (1 + monthlyRate)^termMonths
        BigDecimal power = BigDecimal.valueOf(Math.pow(onePlusRate.doubleValue(), termMonths));
        
        // Calculate maturity amount
        BigDecimal maturityAmount = principal.multiply(power);
        
        // Calculate interest
        BigDecimal interest = maturityAmount.subtract(principal).setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Calculated compound interest: {} (Maturity Amount: {})", interest, maturityAmount);
        return interest;
    }

    /**
     * Calculate interest based on calculation method
     * 
     * @param principal The principal amount
     * @param rate Annual interest rate in percentage
     * @param termMonths Term in months
     * @param calculationMethod Interest calculation method (SIMPLE or COMPOUND)
     * @return Calculated interest amount
     */
    public BigDecimal calculateInterest(BigDecimal principal, BigDecimal rate, int termMonths, String calculationMethod) {
        if ("COMPOUND".equalsIgnoreCase(calculationMethod)) {
            return calculateCompoundInterest(principal, rate, termMonths);
        } else {
            return calculateSimpleInterest(principal, rate, termMonths);
        }
    }

    /**
     * Calculate daily interest for accrual
     * Used by the daily batch job
     * 
     * @param principal Current principal amount
     * @param rate Annual interest rate in percentage
     * @param calculationMethod Interest calculation method
     * @return Daily interest amount
     */
    public BigDecimal calculateDailyInterest(BigDecimal principal, BigDecimal rate, String calculationMethod) {
        // For daily accrual, we calculate monthly interest and divide by 30
        BigDecimal monthlyInterest = calculateInterest(principal, rate, 1, calculationMethod);
        
        // Divide by 30 to get daily interest
        BigDecimal dailyInterest = monthlyInterest.divide(new BigDecimal(30), 2, RoundingMode.HALF_UP);
        
        log.debug("Daily interest: {} (Monthly: {})", dailyInterest, monthlyInterest);
        return dailyInterest;
    }

    /**
     * Calculate interest for a specific period (used for custom calculations)
     * 
     * @param principal Principal amount
     * @param rate Annual interest rate in percentage
     * @param days Number of days
     * @param calculationMethod Interest calculation method
     * @return Interest for the specified period
     */
    public BigDecimal calculateInterestForDays(BigDecimal principal, BigDecimal rate, int days, String calculationMethod) {
        BigDecimal dailyInterest = calculateDailyInterest(principal, rate, calculationMethod);
        return dailyInterest.multiply(new BigDecimal(days)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate maturity amount (Principal + Interest)
     * 
     * @param principal Principal amount
     * @param rate Annual interest rate in percentage
     * @param termMonths Term in months
     * @param calculationMethod Interest calculation method
     * @return Maturity amount
     */
    public BigDecimal calculateMaturityAmount(BigDecimal principal, BigDecimal rate, int termMonths, String calculationMethod) {
        BigDecimal interest = calculateInterest(principal, rate, termMonths, calculationMethod);
        return principal.add(interest);
    }
}
