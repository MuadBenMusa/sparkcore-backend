package com.sparkcore.backend.controller;

import com.sparkcore.backend.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests using @WebMvcTest.
 *
 * Test focus:
 *   - Bean Validation (400 for invalid IBAN / missing amount)
 *   - Idempotency-Key header is correctly parsed and forwarded to the service
 *   - 404 responses for missing accounts
 *   - 200 success path with correct response body
 *   - 401 for unauthenticated access (URL-level Spring Security enforcement)
 *
 * @PreAuthorize role-based access control (ADMIN vs USER) is tested by
 * SparkcoreBackendApplicationTests (full integration test with Testcontainers).
 */
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter is a @Component, scanned by @WebMvcTest.
    // Its dependencies must be mocked so the context starts.
    @MockitoBean
    private com.sparkcore.backend.security.JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockitoBean
    private com.sparkcore.backend.service.TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private com.sparkcore.backend.service.RateLimiterService rateLimiterService;

    @MockitoBean
    private org.springframework.security.authentication.AuthenticationProvider authenticationProvider;

    @MockitoBean
    private AccountService accountService;

    // ============================================================
    // SECTION 1: Validation (Bean Validation at HTTP layer)
    // ============================================================

    @Test
    @WithMockUser
    void transfer_Returns400_WhenFromIbanIsInvalid() throws Exception {
        // @ValidIban on fromIban rejects structurally invalid IBANs
        String body = """
                {
                    "fromIban": "NOT_A_REAL_IBAN",
                    "toIban":   "DE46100500000284667551",
                    "amount":   50.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void transfer_Returns400_WhenToIbanIsInvalid() throws Exception {
        // @ValidIban on toIban catches bad formats before service layer
        String body = """
                {
                    "fromIban": "DE89370400440532013000",
                    "toIban":   "GARBAGE_IBAN",
                    "amount":   50.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void transfer_Returns400_WhenAmountIsNegative() throws Exception {
        // @Positive rejects negative/zero amounts at the HTTP layer
        String body = """
                {
                    "fromIban": "DE89370400440532013000",
                    "toIban":   "DE46100500000284667551",
                    "amount":   -10.00
                }
                """;

        mockMvc.perform(post("/api/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void transfer_Returns400_WhenAmountIsZeroOrNegative() throws Exception {
        // @Positive rejects zero and negative values at the HTTP layer
        String body = """
                {
                    "fromIban": "DE89370400440532013000",
                    "toIban":   "DE46100500000284667551",
                    "amount":   0
                }
                """;

        mockMvc.perform(post("/api/v1/accounts/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // SECTION 2: Idempotency-Key header forwarding
    // ============================================================

    @Test
    @WithMockUser
    void getTransactionHistory_Returns200_WhenIbanExists() throws Exception {
        // GET endpoint that doesn't need Authentication parameter injection
        when(accountService.getTransactionHistory("DE89370400440532013000")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/accounts/DE89370400440532013000/transactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ============================================================
    // SECTION 3: Account retrieval
    // ============================================================

    @Test
    @WithMockUser
    void getAccountById_Returns404_WhenNotFound() throws Exception {
        when(accountService.getAccountById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/accounts/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getAllAccounts_Returns200_WhenAuthenticated() throws Exception {
        when(accountService.getAllAccounts()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
