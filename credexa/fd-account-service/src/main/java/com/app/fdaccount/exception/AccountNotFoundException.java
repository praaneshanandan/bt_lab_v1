package com.app.fdaccount.exception;

/**
 * Exception thrown when an FD account is not found
 */
public class AccountNotFoundException extends RuntimeException {
    
    public AccountNotFoundException(String message) {
        super(message);
    }
    
    public AccountNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
