package com.natwest.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Account details including full ledger")
public record AccountResponse(
        String accountId,
        String balance,
        List<TransactionResponse> transactions
) {}
