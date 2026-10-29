package com.natwest.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to transfer money between two accounts")
public record TransferRequest(
        @NotBlank(message = "fromAccountId is required")
        @Schema(example = "ACC-001")
        String fromAccountId,

        @NotBlank(message = "toAccountId is required")
        @Schema(example = "ACC-002")
        String toAccountId,

        @NotBlank(message = "amount is required")
        @Pattern(
                regexp = "^(0|[1-9]\\d{0,16})(\\.\\d{1,2})?$",
                message = "amount must be a positive number with up to 17 digits and 2 decimals (max 99999999999999999.99)"
        )
        @Schema(example = "25.00")
        String amount
) {}