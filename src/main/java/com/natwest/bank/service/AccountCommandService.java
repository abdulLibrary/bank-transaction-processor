package com.natwest.bank.service;

import com.natwest.bank.domain.Account;
import com.natwest.bank.domain.Money;
import com.natwest.bank.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account lifecycle commands (open, and later freeze/close).
 */
@Service
public class AccountCommandService {

    private static final Logger log = LoggerFactory.getLogger(AccountCommandService.class);

    private final AccountRepository accountRepository;

    public AccountCommandService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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
}
