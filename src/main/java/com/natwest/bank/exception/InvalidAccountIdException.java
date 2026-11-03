package com.natwest.bank.exception;

public class InvalidAccountIdException extends RuntimeException {

    public InvalidAccountIdException(String message) {
        super(message);
    }
}
