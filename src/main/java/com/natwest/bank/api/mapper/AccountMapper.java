package com.natwest.bank.api.mapper;

import com.natwest.bank.api.dto.AccountResponse;
import com.natwest.bank.api.dto.TransactionResponse;
import com.natwest.bank.domain.Account;
import com.natwest.bank.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AccountMapper {

    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getType().name(),
                t.getAmount().toString(),
                t.getTimestamp(),
                t.getDescription(),
                t.getRelatedAccountId()
        );
    }

    public AccountResponse toResponse(Account account) {
        List<TransactionResponse> txs = account.getLedger().stream()
                .map(this::toResponse)
                .toList();
        return new AccountResponse(
                account.getId(),
                account.getBalance().toString(),
                txs
        );
    }
}
