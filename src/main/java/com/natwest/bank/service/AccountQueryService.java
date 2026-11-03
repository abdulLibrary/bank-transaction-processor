package com.natwest.bank.service;

import com.natwest.bank.domain.Account;
import com.natwest.bank.domain.Money;
import com.natwest.bank.domain.Transaction;
import com.natwest.bank.exception.AccountNotFoundException;
import com.natwest.bank.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only account queries. Separated from commands (CQRS-lite).
 */
@Service
public class AccountQueryService {

    private final AccountRepository accountRepository;

    public AccountQueryService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public Money getBalance(String accountId) {
        return requireAccount(accountId).getBalance();
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String accountId) {
        return requireAccount(accountId).getLedger();
    }

    @Transactional(readOnly = true)
    public Account requireAccount(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}
