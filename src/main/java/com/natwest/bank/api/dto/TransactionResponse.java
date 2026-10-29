package com.natwest.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Ledger entry")
public record TransactionResponse(
        String id,
        String type,
        String amount,
        Instant timestamp,
        String description,
        String relatedAccountId
) {}
