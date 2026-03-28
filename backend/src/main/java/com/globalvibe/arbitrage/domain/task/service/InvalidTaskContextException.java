package com.globalvibe.arbitrage.domain.task.service;

public class InvalidTaskContextException extends RuntimeException {

    public InvalidTaskContextException(String message) {
        super(message);
    }
}
