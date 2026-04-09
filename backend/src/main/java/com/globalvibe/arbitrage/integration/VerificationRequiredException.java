package com.globalvibe.arbitrage.integration;

public class VerificationRequiredException extends IllegalStateException {

    public VerificationRequiredException(String message) {
        super(message);
    }

    public VerificationRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
