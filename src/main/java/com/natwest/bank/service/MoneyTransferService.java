package com.natwest.bank.service;

import com.natwest.bank.domain.Account;
import com.natwest.bank.domain.Money;
import com.natwest.bank.event.MoneyDepositedEvent;
import com.natwest.bank.event.MoneyWithdrawnEvent;
import com.natwest.bank.exception.AccountNotFoundException;
import com.natwest.bank.exception.SameAccountTransferException;
import com.natwest.bank.port.DomainEventPort;
import com.natwest.bank.repository.AccountRepository;
import com.natwest.bank.support.OptimisticLockRetry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Money movement: deposit, withdraw, transfer.
 * Transfer retries use a separate bean so each attempt gets its own transaction.
 */
@Service
public class MoneyTransferService {

    private static final Logger log = LoggerFactory.getLogger(MoneyTransferService.class);
    private static final int TRANSFER_MAX_RETRIES = 3;

    private final AccountRepository accountRepository;
    private final DomainEventPort domainEvents;
    private final OptimisticLockRetry optimisticLockRetry;
    private final TransferWorker transferWorker;

    public MoneyTransferService(AccountRepository accountRepository,
                                DomainEventPort domainEvents,
                                OptimisticLockRetry optimisticLockRetry,
                                TransferWorker transferWorker) {
        this.accountRepository = accountRepository;
        this.domainEvents = domainEvents;
        this.optimisticLockRetry = optimisticLockRetry;
        this.transferWorker = transferWorker;
    }

    @Transactional
    @CircuitBreaker(name = "bankOperations")
    @RateLimiter(name = "bankOperations")
    public void deposit(String accountId, Money amount) {
        Account account = requireAccount(accountId);
        account.deposit(amount, "Deposit");
        accountRepository.save(account);
        domainEvents.record("Account", accountId, new MoneyDepositedEvent(accountId, amount));
        log.info("Deposited {} into account {}", amount, accountId);
    }

    @Transactional
    @CircuitBreaker(name = "bankOperations")
    @RateLimiter(name = "bankOperations")
    public void withdraw(String accountId, Money amount) {
        Account account = requireAccount(accountId);
        account.withdraw(amount, "Withdrawal");
        accountRepository.save(account);
        domainEvents.record("Account", accountId, new MoneyWithdrawnEvent(accountId, amount));
        log.info("Withdrew {} from account {}", amount, accountId);
    }

    /**
     * Not @Transactional: each retry must open a new transaction via TransferWorker.
     */
    @CircuitBreaker(name = "bankOperations")
    @RateLimiter(name = "bankOperations")
    public void transfer(String fromAccountId, String toAccountId, Money amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new SameAccountTransferException(fromAccountId);
        }

        optimisticLockRetry.execute(TRANSFER_MAX_RETRIES,
                () -> transferWorker.execute(fromAccountId, toAccountId, amount));
    }

    private Account requireAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}