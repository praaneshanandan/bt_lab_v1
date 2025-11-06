package com.app.fdaccount.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.app.fdaccount.service.StatementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Batch job for generating periodic account statements
 * Daily statements run at 3:00 AM
 * Monthly statements run at 2:00 AM on the 1st of each month
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatementGenerationBatch {
    
    private final StatementService statementService;
    
    /**
     * Generate daily statements for all active accounts
     * Scheduled to run at 3:00 AM daily
     */
    @Scheduled(cron = "${batch.statement-daily.cron:0 0 3 * * ?}")
    public void generateDailyStatements() {
        log.info("===============================================");
        log.info("🕐 Starting daily statement generation batch...");
        log.info("===============================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            int generatedCount = statementService.generateDailyStatements();
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("===============================================");
            log.info("✅ Daily statement generation completed successfully");
            log.info("Generated {} statements in {}ms", generatedCount, duration);
            log.info("===============================================");
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("===============================================");
            log.error("❌ Daily statement generation failed after {}ms", duration);
            log.error("Error: {}", e.getMessage(), e);
            log.error("===============================================");
        }
    }
    
    /**
     * Generate monthly statements for all active accounts
     * Scheduled to run at 2:00 AM on the 1st of each month
     */
    @Scheduled(cron = "${batch.statement-monthly.cron:0 0 2 1 * ?}")
    public void generateMonthlyStatements() {
        log.info("================================================");
        log.info("🕐 Starting monthly statement generation batch...");
        log.info("================================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            int generatedCount = statementService.generateMonthlyStatements();
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("================================================");
            log.info("✅ Monthly statement generation completed successfully");
            log.info("Generated {} statements in {}ms", generatedCount, duration);
            log.info("================================================");
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("================================================");
            log.error("❌ Monthly statement generation failed after {}ms", duration);
            log.error("Error: {}", e.getMessage(), e);
            log.error("================================================");
        }
    }
}
