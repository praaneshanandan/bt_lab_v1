package com.app.account.batch;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service to handle batch processing time (supports time travel for testing)
 */
@Service
public class BatchTimeService {

    private LocalDate overrideDate = null;

    /**
     * Get the current batch processing date
     * If time travel is enabled, returns the override date
     */
    public LocalDate getBatchDate() {
        return overrideDate != null ? overrideDate : LocalDate.now();
    }

    /**
     * Get the current batch processing datetime
     */
    public LocalDateTime getBatchDateTime() {
        LocalDate date = getBatchDate();
        return date.atStartOfDay();
    }

    /**
     * Enable time travel mode with specific date
     */
    public void setOverrideDate(LocalDate date) {
        this.overrideDate = date;
    }

    /**
     * Disable time travel mode (return to current date)
     */
    public void clearOverrideDate() {
        this.overrideDate = null;
    }

    /**
     * Check if time travel mode is active
     */
    public boolean isTimeTravelActive() {
        return overrideDate != null;
    }

    /**
     * Get the override date (null if not in time travel mode)
     */
    public LocalDate getOverrideDate() {
        return overrideDate;
    }
}
