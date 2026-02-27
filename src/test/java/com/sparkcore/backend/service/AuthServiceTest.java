package com.sparkcore.backend.service;

import com.sparkcore.backend.dto.AuthRequest;
import com.sparkcore.backend.dto.AuthResponse;
import com.sparkcore.backend.dto.RegisterRequest;
import com.sparkcore.backend.model.AppUser;
import com.sparkcore.backend.model.AuditLog;
import com.sparkcore.backend.model.Role;
import com.sparkcore.backend.repository.AuditLogRepository;
import com.sparkcore.backend.repository.UserRepository;
import com.sparkcore.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_Success() {
        // --- 1. ARRANGE ---
        RegisterRequest request = new RegisterRequest("newuser", "password123");

        when(userRepository.findByUsername(request.username())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
        when(jwtService.generateToken(request.username())).thenReturn("mocked_jwt_token");

        // --- 2. ACT ---
        AuthResponse response = authService.register(request);

        // --- 3. ASSERT & VERIFY ---
        assertEquals("mocked_jwt_token", response.token());

        // Prüfen, ob der User als Role.USER in die Datenbank gespeichert wurde
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        AppUser savedUser = userCaptor.getValue();
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("hashed_password", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole()); // Immer Role.USER!
    }

    @Test
    void register_ThrowsException_WhenUsernameExists() {
        // --- 1. ARRANGE ---
        RegisterRequest request = new RegisterRequest("existinguser", "password123");
        AppUser existingUser = new AppUser("existinguser", "hash", Role.USER);

        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(existingUser));

        // --- 2. ACT & ASSERT ---
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Benutzername ist bereits vergeben!", exception.getMessage());

        // Sicherstellen, dass nichts überschrieben oder gespeichert wurde
        verify(userRepository, never()).save(any(AppUser.class));
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void login_Success() {
        // --- 1. ARRANGE ---
        AuthRequest request = new AuthRequest("john_doe", "secretpass");
        AppUser user = new AppUser("john_doe", "hashed", Role.USER);

        // Wir tun so, als ob die Authentifizierung klappt (wirft keine Exception)
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        when(userRepository.findByUsername(request.username())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user.getUsername())).thenReturn("success_token");

        // --- 2. ACT ---
        AuthResponse response = authService.login(request);

        // --- 3. ASSERT & VERIFY ---
        assertEquals("success_token", response.token());

        // Prüfen, ob ein erfolgreiches Audit-Log geschrieben wurde
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertEquals("john_doe", savedLog.getUsername());
        assertEquals("LOGIN_SUCCESS", savedLog.getAction());
        assertEquals("SUCCESS", savedLog.getStatus());
    }

    @Test
    void login_ThrowsException_WhenCredentialsInvalid() {
        // --- 1. ARRANGE ---
        AuthRequest request = new AuthRequest("hacker", "wrongpass");

        // Wir tun so, als ob die Authentifizierung fehlschlägt
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // --- 2. ACT & ASSERT ---
        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request));

        // --- 3. VERIFY ---
        // Prüfen, ob ein fehlerhaftes Audit-Log geschrieben wurde (verhindert
        // Brute-Force Monitoring Lücken)
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertEquals("hacker", savedLog.getUsername());
        assertEquals("LOGIN_FAILED", savedLog.getAction());
        assertEquals("FAILURE", savedLog.getStatus());

        // Sicherstellen, dass auf keinen Fall ein Token generiert wurde
        verify(jwtService, never()).generateToken(anyString());
    }
}
