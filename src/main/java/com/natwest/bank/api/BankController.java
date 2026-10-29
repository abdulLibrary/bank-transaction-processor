package com.natwest.bank.api;

import com.natwest.bank.api.dto.*;
import com.natwest.bank.api.mapper.AccountMapper;
import com.natwest.bank.domain.Money;
import com.natwest.bank.idempotency.IdempotencyRecord;
import com.natwest.bank.idempotency.IdempotencyService;
import com.natwest.bank.service.BankService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Banking transaction operations")
public class BankController {

    private final BankService bankService;
    private final AccountMapper mapper;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public BankController(BankService bankService,
                          AccountMapper mapper,
                          IdempotencyService idempotencyService,
                          ObjectMapper objectMapper) {
        this.bankService = bankService;
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Open a new account")
    @ApiResponse(responseCode = "201", description = "Account created")
    public ResponseEntity<AccountResponse> openAccount(@Valid @RequestBody CreateAccountRequest request) {
        Money initial = (request.initialBalance() == null || request.initialBalance().isBlank())
                ? Money.ZERO
                : Money.of(request.initialBalance());

        var account = bankService.openAccount(request.accountId(), initial);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(account));
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get current balance")
    public BalanceResponse getBalance(@PathVariable String accountId) {
        Money balance = bankService.getBalance(accountId);
        return new BalanceResponse(accountId, balance.toString());
    }


    @GetMapping("/{accountId}")
    @Operation(summary = "Get account with full transaction history")
    public AccountResponse getAccount(@PathVariable String accountId) {
        return bankService.getAccountDetails(accountId);
    }



    @PostMapping("/{accountId}/deposits")
    @Operation(summary = "Deposit money",
            parameters = @Parameter(name = "Idempotency-Key", description = "Client-generated unique key to make the operation idempotent"))
    @ApiResponse(responseCode = "204", description = "Deposit successful")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deposit(
            @PathVariable String accountId,
            @Valid @RequestBody MoneyRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return executeIdempotent("DEPOSIT", idempotencyKey, request, () -> {
            bankService.deposit(accountId, Money.of(request.amount()));
            return ResponseEntity.noContent().build();
        });
    }

    @PostMapping("/{accountId}/withdrawals")
    @Operation(summary = "Withdraw money")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> withdraw(
            @PathVariable String accountId,
            @Valid @RequestBody MoneyRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return executeIdempotent("WITHDRAWAL", idempotencyKey, request, () -> {
            bankService.withdraw(accountId, Money.of(request.amount()));
            return ResponseEntity.noContent().build();
        });
    }

    @PostMapping("/transfers")
    @Operation(summary = "Transfer money between accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return executeIdempotent("TRANSFER", idempotencyKey, request, () -> {
            bankService.transfer(
                    request.fromAccountId(),
                    request.toAccountId(),
                    Money.of(request.amount())
            );
            return ResponseEntity.noContent().build();
        });
    }

    private ResponseEntity<Void> executeIdempotent(String operation,
                                                   String key,
                                                   Object requestBody,
                                                   java.util.function.Supplier<ResponseEntity<Void>> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }

        Optional<IdempotencyRecord> existing = idempotencyService.find(key, operation);
        if (existing.isPresent()) {
            idempotencyService.assertSameRequest(existing.get(), requestBody);
            return ResponseEntity.status(existing.get().getResponseStatus()).build();
        }

        ResponseEntity<Void> response = action.get();
        idempotencyService.save(key, operation, requestBody, response.getStatusCode().value(), null);
        return response;
    }
}
