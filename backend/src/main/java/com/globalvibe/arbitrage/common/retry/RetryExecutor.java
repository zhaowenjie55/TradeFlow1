package com.globalvibe.arbitrage.common.retry;

import com.globalvibe.arbitrage.integration.VerificationRequiredException;
import org.slf4j.Logger;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Simple retry executor with exponential backoff and jitter.
 * <p>
 * {@link VerificationRequiredException} is never retried — it always propagates immediately
 * because it represents a user-actionable state (1688 CAPTCHA/login).
 */
public final class RetryExecutor {

    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final double backoffMultiplier;
    private final Logger logger;

    public RetryExecutor(int maxAttempts, long initialBackoffMillis, double backoffMultiplier, Logger logger) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialBackoffMillis = Math.max(0, initialBackoffMillis);
        this.backoffMultiplier = Math.max(1.0, backoffMultiplier);
        this.logger = logger;
    }

    public RetryExecutor(int maxAttempts, long initialBackoffMillis, Logger logger) {
        this(maxAttempts, initialBackoffMillis, 2.0, logger);
    }

    public <T> T execute(Supplier<T> action, String operationName) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (VerificationRequiredException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                lastError = ex;
                if (attempt == maxAttempts) {
                    break;
                }
                long backoff = computeBackoff(attempt);
                logger.warn("{} failed (attempt {}/{}), retrying in {}ms: {}",
                        operationName, attempt, maxAttempts, backoff, ex.getMessage());
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw lastError;
    }

    private long computeBackoff(int attempt) {
        long base = (long) (initialBackoffMillis * Math.pow(backoffMultiplier, attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, base / 4));
        return base + jitter;
    }
}
