package com.natwest.bank.service;

import com.natwest.bank.domain.Account;
import com.natwest.bank.domain.Money;
import com.natwest.bank.domain.Transaction;
import org.springframework.stereotype.Service;

import java.util.List;

/* Prefer AccountQueryService, AccountCommandService, MoneyTransferService in new code.
 */
@Service
public class BankService {

    private final AccountQueryService queries;
    private final AccountCommandService commands;
    private final MoneyTransferService transfers;

    public BankService(AccountQueryService queries,
                       AccountCommandService commands,
                       MoneyTransferService transfers) {
        this.queries = queries;
        this.commands = commands;
        this.transfers = transfers;
    }

    public Account openAccount(String accountId, Money initialBalance) {
        return commands.openAccount(accountId, initialBalance);
    }

    public Money getBalance(String accountId) {
        return queries.getBalance(accountId);
    }

    public List<Transaction> getTransactionHistory(String accountId) {
        return queries.getTransactionHistory(accountId);
    }

    public void deposit(String accountId, Money amount) {
        transfers.deposit(accountId, amount);
    }

    public void withdraw(String accountId, Money amount) {
        transfers.withdraw(accountId, amount);
    }

    public void transfer(String fromAccountId, String toAccountId, Money amount) {
        transfers.transfer(fromAccountId, toAccountId, amount);
    }

    public Account getAccount(String accountId) {
        return queries.requireAccount(accountId);
    }
}
