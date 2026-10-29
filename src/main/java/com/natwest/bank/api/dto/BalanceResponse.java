package com.natwest.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current account balance")
public record BalanceResponse(
        @Schema(example = "ACC-001") String accountId,
        @Schema(example = "150.00") String balance
) {}
