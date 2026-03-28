package com.globalvibe.arbitrage.integration;

public class GatewayFallbackException extends IllegalStateException {

    public GatewayFallbackException(String message) {
        super(message);
    }

    public GatewayFallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
