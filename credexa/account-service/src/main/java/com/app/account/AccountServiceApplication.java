package com.app.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

/**
 * Main Application Class for Account Service
 * Simplified FD Account Management with JWT Authentication
 */
@SpringBootApplication(scanBasePackages = {"com.app.account", "com.app.common"})
@EnableScheduling
@Slf4j
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
        
        log.info("==============================================");
        log.info("✅ Account Service started successfully!");
        log.info("📍 Swagger UI: http://localhost:8087/api/accounts/swagger-ui.html");
        log.info("🏦 Managing Fixed Deposit Accounts");
        log.info("🔄 Batch Processing available (disabled by default)");
        log.info("==============================================");
    }
}
