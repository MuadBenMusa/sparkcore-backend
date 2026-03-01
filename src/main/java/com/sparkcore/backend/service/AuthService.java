package com.sparkcore.backend.service;

import com.sparkcore.backend.dto.AuthRequest;
import com.sparkcore.backend.dto.AuthResponse;
import com.sparkcore.backend.dto.RegisterRequest;
import com.sparkcore.backend.exception.RefreshTokenException;
import com.sparkcore.backend.model.AppUser;
import com.sparkcore.backend.model.AuditLog;
import com.sparkcore.backend.model.Role;
import com.sparkcore.backend.repository.UserRepository;
import com.sparkcore.backend.repository.AuditLogRepository;
import com.sparkcore.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.sparkcore.backend.util.RequestUtils;
import java.time.Duration;

// Registrierung und Login – gibt bei Erfolg immer ein JWT zurück
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogRepository auditLogRepository;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuthenticationManager authenticationManager, AuditLogRepository auditLogRepository,
            RefreshTokenService refreshTokenService, TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditLogRepository = auditLogRepository;
        this.refreshTokenService = refreshTokenService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResponse register(RegisterRequest request) {
        // Username schon vergeben?
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Benutzername ist bereits vergeben!");
        }

        AppUser newUser = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.USER); // Role is always USER – never client-controlled
        userRepository.save(newUser);

        String jwtToken = jwtService.generateToken(newUser.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(newUser.getUsername()).getToken();

        return new AuthResponse(jwtToken, refreshToken);
    }

    public AuthResponse login(AuthRequest request) {
        try {
            // Versucht, den User anzumelden
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (Exception e) {
            // FEHLERFALL: Falsches Passwort oder Benutzer nicht gefunden!
            auditLogRepository.save(new AuditLog(
                    request.username(), "LOGIN_FAILED",
                    "Ungültige Anmeldedaten",
                    RequestUtils.getClientIp(), "FAILURE"));
            throw e; // Fehler weiterwerfen, damit der Controller abbricht
        }

        // ERFOLGSFALL: Wenn wir hier ankommen, war das Passwort richtig!
        auditLogRepository.save(new AuditLog(
                request.username(), "LOGIN_SUCCESS",
                "Erfolgreich angemeldet",
                RequestUtils.getClientIp(), "SUCCESS"));

        AppUser user = userRepository.findByUsername(request.username()).orElseThrow();
        String jwtToken = jwtService.generateToken(user.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername()).getToken();

        return new AuthResponse(jwtToken, refreshToken);
    }

    public AuthResponse refreshToken(String refreshTokenRequest) {
        return refreshTokenService.verifyExpiration(refreshTokenRequest)
                .map(refreshToken -> {
                    AppUser user = refreshToken.getUser();
                    // Neues JWT
                    String jwtToken = jwtService.generateToken(user.getUsername());
                    // Refresh Token rotieren für höhere Sicherheit
                    String newRefreshToken = refreshTokenService.createRefreshToken(user.getUsername()).getToken();

                    return new AuthResponse(jwtToken, newRefreshToken);
                })
                .orElseThrow(() -> new RefreshTokenException("Refresh Token nicht gefunden oder bereits verwendet."));
    }

    public void logout(String authHeader, String username) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Logout erfordert einen gültigen Bearer-Token im Authorization-Header.");
        }

        String jwt = authHeader.substring(7);

        // 1. JWT auf die Blacklist in Redis setzen (mit der exakten Restlaufzeit)
        long expirationTimeMs = jwtService.extractExpiration(jwt).getTime();
        long nowMs = System.currentTimeMillis();
        long diffMs = expirationTimeMs - nowMs;

        if (diffMs > 0) {
            tokenBlacklistService.blacklistToken(jwt, Duration.ofMillis(diffMs));
        }

        // 2. Bestehendes Refresh-Token aus der PostgreSQL Datenbank löschen
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User nicht gefunden"));
        refreshTokenService.deleteByUser(user);
    }
}