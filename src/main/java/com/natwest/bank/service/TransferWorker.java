package com.natwest.bank.service;

import com.natwest.bank.domain.Account;
import com.natwest.bank.domain.Money;
import com.natwest.bank.event.TransferCompletedEvent;
import com.natwest.bank.exception.AccountNotFoundException;
import com.natwest.bank.port.DomainEventPort;
import com.natwest.bank.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One transfer attempt = one transaction (load → domain → save → outbox).
 */
@Service
public class TransferWorker {

    private static final Logger log = LoggerFactory.getLogger(TransferWorker.class);

    private final AccountRepository accountRepository;
    private final DomainEventPort domainEvents;

    public TransferWorker(AccountRepository accountRepository, DomainEventPort domainEvents) {
        this.accountRepository = accountRepository;
        this.domainEvents = domainEvents;
    }

    @Transactional
    public void execute(String fromAccountId, String toAccountId, Money amount) {
        Account from = requireAccount(fromAccountId);
        Account to = requireAccount(toAccountId);

        from.transferOut(amount, toAccountId, "Transfer to " + toAccountId);
        to.transferIn(amount, fromAccountId, "Transfer from " + fromAccountId);

        accountRepository.save(from);
        accountRepository.save(to);

        domainEvents.record("Account", fromAccountId,
                new TransferCompletedEvent(fromAccountId, toAccountId, amount));
        log.info("Transferred {} from {} to {}", amount, fromAccountId, toAccountId);
    }

    private Account requireAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
