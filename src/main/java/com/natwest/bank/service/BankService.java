package com.natwest.bank.service;

import com.natwest.bank.api.dto.AccountResponse;
import com.natwest.bank.api.dto.TransactionResponse;
import com.natwest.bank.api.mapper.AccountMapper;
import com.natwest.bank.domain.Account;
import com.natwest.bank.domain.Money;
import com.natwest.bank.domain.Transaction;
import com.natwest.bank.event.MoneyDepositedEvent;
import com.natwest.bank.event.MoneyWithdrawnEvent;
import com.natwest.bank.event.TransferCompletedEvent;
import com.natwest.bank.exception.AccountNotFoundException;
import com.natwest.bank.outbox.OutboxPublisher;
import com.natwest.bank.repository.AccountRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BankService {

    private static final Logger log = LoggerFactory.getLogger(BankService.class);

    private final AccountRepository accountRepository;
    private final OutboxPublisher outboxPublisher;

    public BankService(AccountRepository accountRepository, OutboxPublisher outboxPublisher) {
        this.accountRepository = accountRepository;
        this.outboxPublisher = outboxPublisher;
    }

    @Transactional
    public Account openAccount(String accountId, Money initialBalance) {
        if (accountRepository.existsById(accountId)) {
            throw new IllegalArgumentException("Account already exists: " + accountId);
        }
        Account account = new Account(accountId, initialBalance);
        Account saved = accountRepository.save(account);
        log.info("Opened account {} with balance {}", accountId, initialBalance);
        return saved;
    }

    @Transactional(readOnly = true)
    public Money getBalance(String accountId) {
        return getAccount(accountId).getBalance();
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String accountId) {
        return getAccount(accountId).getLedger();
    }

    @Transactional
    @CircuitBreaker(name = "bankOperations")
    @RateLimiter(name = "bankOperations")
    public void deposit(String accountId, Money amount) {
        Account account = getAccount(accountId);
        account.deposit(amount, "Deposit");
        accountRepository.save(account);
        outboxPublisher.enqueue("Account", accountId, new MoneyDepositedEvent(accountId, amount));
        log.info("Deposited {} into account {}", amount, accountId);
    }

    @Transactional
    @CircuitBreaker(name = "bankOperations")
    @RateLimiter(name = "bankOperations")
    public void withdraw(String accountId, Money amount) {
        Account account = getAccount(accountId);
        account.withdraw(amount, "Withdrawal");
        accountRepository.save(account);
        outboxPublisher.enqueue("Account", accountId, new MoneyWithdrawnEvent(accountId, amount));
        log.info("Withdrew {} from account {}", amount, accountId);
    }

    /**
     * Transfer with optimistic locking.
     * On version conflict we retry a limited number of times.
     */
    @Transactional
    @CircuitBreaker(name = "bankOperations")
    @RateLimiter(name = "bankOperations")
    public void transfer(String fromAccountId, String toAccountId, Money amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                doTransfer(fromAccountId, toAccountId, amount);
                return;
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.warn("Optimistic lock conflict on transfer attempt {}/{}", attempt, maxRetries);
                if (attempt == maxRetries) {
                    throw ex;
                }
            }
        }
    }

    private void doTransfer(String fromAccountId, String toAccountId, Money amount) {
        Account from = getAccount(fromAccountId);
        Account to = getAccount(toAccountId);

        from.transferOut(amount, toAccountId, "Transfer to " + toAccountId);
        to.transferIn(amount, fromAccountId, "Transfer from " + fromAccountId);

        accountRepository.save(from);
        accountRepository.save(to);

        outboxPublisher.enqueue("Account", fromAccountId,
                new TransferCompletedEvent(fromAccountId, toAccountId, amount));

        log.info("Transferred {} from {} to {}", amount, fromAccountId, toAccountId);
    }

    private Account getAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }


    @Transactional(readOnly = true)
    public AccountResponse getAccountDetails(String accountId) {
        AccountMapper mapper = new AccountMapper();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        List<TransactionResponse> history = account.getLedger()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return new AccountResponse(
                accountId,
                account.getBalance().toString(),
                history
        );
    }


}
