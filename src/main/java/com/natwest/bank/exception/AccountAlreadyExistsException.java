package com.natwest.bank.exception;

public class AccountAlreadyExistsException extends RuntimeException {

    private final String accountId;

    public AccountAlreadyExistsException(String accountId) {
        super("Account already exists: " + accountId);
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
