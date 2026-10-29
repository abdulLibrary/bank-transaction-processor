package com.natwest.bank.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.natwest.bank.api.dto.CreateAccountRequest;
import com.natwest.bank.api.dto.MoneyRequest;
import com.natwest.bank.api.dto.TransferRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BankControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;

    @Test
    void fullHappyPath() throws Exception {
        // Create account requires ADMIN
        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateAccountRequest("ACC1", "100.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").value("ACC1"))
                .andExpect(jsonPath("$.balance").value("100.00"));

        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateAccountRequest("ACC2", "0"))))
                .andExpect(status().isCreated());

        // Deposit / transfer as USER
        mockMvc.perform(post("/api/v1/accounts/ACC1/deposits")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "dep-1")
                        .content(mapper.writeValueAsString(new MoneyRequest("50.00"))))
                .andExpect(status().isNoContent());

        // Replay with same key → still 204
        mockMvc.perform(post("/api/v1/accounts/ACC1/deposits")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "dep-1")
                        .content(mapper.writeValueAsString(new MoneyRequest("50.00"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/accounts/transfers")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "xfer-1")
                        .content(mapper.writeValueAsString(new TransferRequest("ACC1", "ACC2", "30.00"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/accounts/ACC1/balance")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("120.00"));

        mockMvc.perform(get("/api/v1/accounts/ACC2/balance")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value("30.00"));
    }

    @Test
    void overdraftReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateAccountRequest("ACC3", "10.00"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/accounts/ACC3/withdrawals")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new MoneyRequest("50.00"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/DOES-NOT-EXIST/balance")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/ACC1/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotCreateAccount() throws Exception {
        mockMvc.perform(post("/api/v1/accounts")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new CreateAccountRequest("ACC-X", "0"))))
                .andExpect(status().isForbidden());
    }
}
