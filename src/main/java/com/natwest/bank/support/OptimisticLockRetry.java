package com.natwest.bank.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Retries a unit of work on optimistic lock conflicts.
 */
@Component
public class OptimisticLockRetry {

    private static final Logger log = LoggerFactory.getLogger(OptimisticLockRetry.class);

    public void execute(int maxAttempts, Runnable action) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.run();
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock conflict on attempt {}/{}", attempt, maxAttempts);
                if (attempt == maxAttempts) {
                    throw ex;
                }
            }
        }
    }
}
