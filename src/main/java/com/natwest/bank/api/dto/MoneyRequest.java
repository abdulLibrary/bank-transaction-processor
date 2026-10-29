package com.natwest.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Monetary amount for deposit or withdrawal")
public record MoneyRequest(
        @NotBlank(message = "amount is required")
        @Pattern(
                regexp = "^(0|[1-9]\\d{0,16})(\\.\\d{1,2})?$",
                message = "amount must be a positive number with up to 17 digits and 2 decimals (max 99999999999999999.99)"
        )
        @Schema(example = "50.00")
        String amount
) {}