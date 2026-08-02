package com.example.productservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

final class RetryingRestClient {

    private static final Logger log = LoggerFactory.getLogger(RetryingRestClient.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    private RetryingRestClient() {
    }

    static <T> T execute(String operation, Supplier<T> request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return request.get();
            } catch (RestClientResponseException exception) {
                if (!isTransient(exception) || attempt == MAX_ATTEMPTS) {
                    throw exception;
                }
                waitBeforeRetry(operation, attempt, exception);
            } catch (RestClientException exception) {
                if (attempt == MAX_ATTEMPTS) {
                    throw exception;
                }
                waitBeforeRetry(operation, attempt, exception);
            }
        }

        throw new IllegalStateException("Retry loop ended unexpectedly");
    }

    private static boolean isTransient(RestClientResponseException exception) {
        return exception.getStatusCode().is5xxServerError()
                || exception.getStatusCode().value() == 429;
    }

    private static void waitBeforeRetry(String operation, int attempt, RestClientException exception) {
        long delay = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
        log.warn("{} failed on attempt {}/{}; retrying in {}ms: {}",
                operation, attempt, MAX_ATTEMPTS, delay, exception.getMessage());
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying " + operation, interruptedException);
        }
    }
}
