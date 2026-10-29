package com.natwest.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to open a new bank account")
public record CreateAccountRequest(
        @NotBlank(message = "accountId is required")
        @Schema(example = "ACC-001")
        String accountId,

        @Pattern(
                regexp = "^(0|[1-9]\\d{0,16})(\\.\\d{1,2})?$",
                message = "initialBalance must be a non-negative number with up to 17 digits and 2 decimals"
        )
        @Schema(example = "100.00", description = "Optional initial balance (defaults to 0)")
        String initialBalance
) {}