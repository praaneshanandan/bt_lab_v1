package com.app.account.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Account Number Generator using Check Digit algorithm
 * Generates unique account numbers with Luhn check digit validation
 */
@Component
public class AccountNumberGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AccountNumberGenerator.class);
    private static final AtomicLong counter = new AtomicLong(1000);

    /**
     * Generate standard account number with check digit
     * Format: FD-YYYYMMDDHHMMSS-NNNN-C
     * Where C is the Luhn check digit
     */
    public String generateStandardAccountNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String sequence = String.format("%04d", counter.getAndIncrement() % 10000);
        
        String baseNumber = "FD" + timestamp + sequence;
        int checkDigit = calculateLuhnCheckDigit(baseNumber);
        
        String accountNumber = String.format("FD-%s-%s-%d", timestamp, sequence, checkDigit);
        logger.info("✅ Generated standard account number: {}", accountNumber);
        
        return accountNumber;
    }

    /**
     * Generate IBAN format account number
     * Format: IN<check-digit><bank-code><branch><account-number>
     * Example: IN29CRED0001FD202511080001
     */
    public String generateIBANAccountNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String sequence = String.format("%04d", counter.getAndIncrement() % 10000);
        
        String bankCode = "CRED"; // Credexa Bank
        String branchCode = "0001"; // Default branch
        String accountPart = "FD" + timestamp.substring(2) + sequence; // Remove century from year
        
        // Calculate IBAN check digits (simplified)
        String baseIBAN = bankCode + branchCode + accountPart;
        int ibanCheckDigit = calculateIBANCheckDigit(baseIBAN);
        
        String iban = String.format("IN%02d%s%s%s", ibanCheckDigit, bankCode, branchCode, accountPart);
        logger.info("✅ Generated IBAN account number: {}", iban);
        
        return iban;
    }

    /**
     * Calculate Luhn check digit
     * Used for validating account numbers
     */
    private int calculateLuhnCheckDigit(String number) {
        // Remove non-digits
        String digits = number.replaceAll("[^0-9]", "");
        
        int sum = 0;
        boolean alternate = false;
        
        // Process digits from right to left
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(digits.charAt(i));
            
            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            
            sum += digit;
            alternate = !alternate;
        }
        
        // Check digit makes the total sum divisible by 10
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Validate account number using Luhn algorithm
     */
    public boolean validateAccountNumber(String accountNumber) {
        try {
            // Extract check digit from account number
            String[] parts = accountNumber.split("-");
            if (parts.length < 4) {
                return false;
            }
            
            int providedCheckDigit = Integer.parseInt(parts[parts.length - 1]);
            String baseNumber = "FD" + parts[1] + parts[2];
            int calculatedCheckDigit = calculateLuhnCheckDigit(baseNumber);
            
            boolean isValid = providedCheckDigit == calculatedCheckDigit;
            if (isValid) {
                logger.debug("✅ Account number validation successful: {}", accountNumber);
            } else {
                logger.warn("❌ Account number validation failed: {}", accountNumber);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("❌ Error validating account number: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Calculate IBAN check digits (mod 97 algorithm)
     * Uses chunking to handle large numbers that exceed Long.MAX_VALUE
     */
    private int calculateIBANCheckDigit(String baseIBAN) {
        // Convert letters to numbers (A=10, B=11, ..., Z=35)
        StringBuilder numericIBAN = new StringBuilder();
        for (char c : baseIBAN.toCharArray()) {
            if (Character.isLetter(c)) {
                numericIBAN.append(Character.getNumericValue(c));
            } else {
                numericIBAN.append(c);
            }
        }
        
        // Add country code numeric value (IN = 1823) and 00 for check digits
        numericIBAN.append("1823").append("00");
        
        // Calculate mod 97 using chunking to avoid Long overflow
        String ibanString = numericIBAN.toString();
        long mod = 0;
        
        // Process in chunks to avoid overflow
        for (int i = 0; i < ibanString.length(); i++) {
            mod = (mod * 10 + Character.getNumericValue(ibanString.charAt(i))) % 97;
        }
        
        int checkDigit = (int) (98 - mod);
        
        return checkDigit;
    }

    /**
     * Validate IBAN format
     */
    public boolean validateIBAN(String iban) {
        try {
            if (!iban.startsWith("IN") || iban.length() < 20) {
                return false;
            }
            
            // Extract check digits
            int checkDigits = Integer.parseInt(iban.substring(2, 4));
            String baseIBAN = iban.substring(4);
            
            // Recalculate and compare
            int calculatedCheckDigit = calculateIBANCheckDigit(baseIBAN);
            
            boolean isValid = checkDigits == calculatedCheckDigit;
            if (isValid) {
                logger.debug("✅ IBAN validation successful: {}", iban);
            } else {
                logger.warn("❌ IBAN validation failed: {}", iban);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("❌ Error validating IBAN: {}", e.getMessage());
            return false;
        }
    }
}
