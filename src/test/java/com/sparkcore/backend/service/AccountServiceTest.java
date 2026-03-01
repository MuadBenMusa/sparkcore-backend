package com.sparkcore.backend.service;

import com.sparkcore.backend.model.Account;
import com.sparkcore.backend.repository.AccountRepository;
import com.sparkcore.backend.repository.TransactionRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @InjectMocks
    private AccountService accountService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Sets an authenticated user in the SecurityContext for tests. */
    private void authenticateAs(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void transferMoney_ThrowsException_WhenInsufficientFunds() {
        String senderIban = "DE_SENDER";
        String receiverIban = "DE_RECEIVER";
        BigDecimal transferAmount = new BigDecimal("500.00");

        Account senderAccount = new Account();
        senderAccount.setIban(senderIban);
        senderAccount.setOwnerName("alice");
        senderAccount.setBalance(new BigDecimal("100.00"));

        Account receiverAccount = new Account();
        receiverAccount.setIban(receiverIban);
        receiverAccount.setBalance(new BigDecimal("0.00"));

        when(accountRepository.findByIban(senderIban)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban(receiverIban)).thenReturn(Optional.of(receiverAccount));

        authenticateAs("alice");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.transferMoney(senderIban, receiverIban, transferAmount));

        assertEquals("Nicht ausreichendes Guthaben auf dem Sender-Konto.", exception.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferMoney_Success() {
        String senderIban = "DE_SENDER";
        String receiverIban = "DE_RECEIVER";
        BigDecimal transferAmount = new BigDecimal("100.00");

        Account senderAccount = new Account();
        senderAccount.setIban(senderIban);
        senderAccount.setOwnerName("alice");
        senderAccount.setBalance(new BigDecimal("500.00"));

        Account receiverAccount = new Account();
        receiverAccount.setIban(receiverIban);
        receiverAccount.setBalance(new BigDecimal("200.00"));

        when(accountRepository.findByIban(senderIban)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban(receiverIban)).thenReturn(Optional.of(receiverAccount));

        authenticateAs("alice");

        accountService.transferMoney(senderIban, receiverIban, transferAmount);

        assertEquals(new BigDecimal("400.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("300.00"), receiverAccount.getBalance());
        verify(accountRepository, times(1)).save(senderAccount);
        verify(accountRepository, times(1)).save(receiverAccount);
        verify(kafkaTemplate, times(1)).send(eq("transaction-events"),
                any(com.sparkcore.backend.dto.TransactionEvent.class));
    }

    @Test
    void transferMoney_ThrowsException_WhenNotAccountOwner() {
        String senderIban = "DE_SENDER";
        String receiverIban = "DE_RECEIVER";

        Account senderAccount = new Account();
        senderAccount.setIban(senderIban);
        senderAccount.setOwnerName("alice");
        senderAccount.setBalance(new BigDecimal("500.00"));

        Account receiverAccount = new Account();
        receiverAccount.setIban(receiverIban);
        receiverAccount.setBalance(BigDecimal.ZERO);

        when(accountRepository.findByIban(senderIban)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban(receiverIban)).thenReturn(Optional.of(receiverAccount));

        // Logged-in user is NOT the account owner
        authenticateAs("bob");

        assertThrows(
                AccessDeniedException.class,
                () -> accountService.transferMoney(senderIban, receiverIban, new BigDecimal("100.00")));

        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any());
    }
}
