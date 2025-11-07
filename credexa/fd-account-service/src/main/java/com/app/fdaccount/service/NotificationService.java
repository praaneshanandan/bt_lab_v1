package com.app.fdaccount.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.app.fdaccount.entity.AccountRole;
import com.app.fdaccount.entity.FdAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    /**
     * Send a notification to the customer after the FD account has matured and been processed.
     *
     * @param account The matured FD account.
     */
    public void sendMaturityPayoutNotification(FdAccount account) {
        log.info("Attempting to send maturity payout notification for account: {}", account.getAccountNumber());

        AccountRole primaryOwner = getPrimaryOwner(account);
        if (primaryOwner == null) {
            log.warn("No active primary owner found for account: {}. Skipping notification.", account.getAccountNumber());
            return;
        }

        String message = buildMaturityPayoutMessage(account, primaryOwner);

        // Mock sending Email
        sendEmail(primaryOwner.getCustomerId(), primaryOwner.getCustomerName(), "Your Fixed Deposit Has Matured", message);

        // Mock sending WhatsApp
        sendWhatsApp(primaryOwner.getCustomerId(), primaryOwner.getCustomerName(), message);

        log.info("✅ Successfully sent maturity payout notification for account: {}", account.getAccountNumber());
    }

    private String buildMaturityPayoutMessage(FdAccount account, AccountRole owner) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
        BigDecimal maturityAmount = account.getTransactions().stream()
                .filter(t -> t.getTransactionType() == com.app.fdaccount.enums.TransactionType.MATURITY_PAYOUT)
                .map(com.app.fdaccount.entity.AccountTransaction::getAmount)
                .findFirst()
                .orElse(account.getMaturityAmount());

        return String.format(
            "Dear %s,\n\n" +
            "Your Fixed Deposit %s has matured on %s.\n\n" +
            "Maturity Amount: ₹%,.2f has been processed as per your instructions.\n\n" +
            "Thank you for banking with us.",
            owner.getCustomerName(),
            account.getAccountNumber(),
            account.getMaturityDate().format(formatter),
            maturityAmount
        );
    }

    private AccountRole getPrimaryOwner(FdAccount account) {
        return account.getRoles().stream()
                .filter(role -> Boolean.TRUE.equals(role.getIsPrimary()) && Boolean.TRUE.equals(role.getIsActive()))
                .findFirst()
                .orElse(account.getRoles().stream()
                        .filter(AccountRole::getIsActive)
                        .findFirst()
                        .orElse(null));
    }

    private void sendEmail(Long customerId, String customerName, String subject, String message) {
        // In a real application, this would integrate with an email service (e.g., SendGrid, AWS SES)
        log.info("📧 [MOCK] Email sent to customer {} ({}). Subject: {}", customerId, customerName, subject);
        log.debug("Email content:\n{}", message);
    }

    private void sendWhatsApp(Long customerId, String customerName, String message) {
        // In a real application, this would integrate with a WhatsApp Business API provider (e.g., Twilio)
        log.info("📱 [MOCK] WhatsApp message sent to customer {} ({}).", customerId, customerName);
        log.debug("WhatsApp content:\n{}", message);
    }
}
