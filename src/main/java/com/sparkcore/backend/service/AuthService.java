package com.sparkcore.backend.service;

import com.sparkcore.backend.dto.AuthRequest;
import com.sparkcore.backend.dto.AuthResponse;
import com.sparkcore.backend.dto.RegisterRequest;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Registrierung und Login – gibt bei Erfolg immer ein JWT zurück
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogRepository auditLogRepository; // NEU

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuthenticationManager authenticationManager, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.auditLogRepository = auditLogRepository;
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest().getRemoteAddr() : "UNKNOWN_IP";
    }

    public AuthResponse register(RegisterRequest request) {
        // Username schon vergeben?
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Benutzername ist bereits vergeben!");
        }

        AppUser newUser = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),
                Role.USER);
        userRepository.save(newUser);

        String jwtToken = jwtService.generateToken(newUser.getUsername());
        return new AuthResponse(jwtToken);
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
                    getClientIp(), "FAILURE"));
            throw e; // Fehler weiterwerfen, damit der Controller abbricht
        }

        // ERFOLGSFALL: Wenn wir hier ankommen, war das Passwort richtig!
        auditLogRepository.save(new AuditLog(
                request.username(), "LOGIN_SUCCESS",
                "Erfolgreich angemeldet",
                getClientIp(), "SUCCESS"));

        AppUser user = userRepository.findByUsername(request.username()).orElseThrow();
        String jwtToken = jwtService.generateToken(user.getUsername());

        return new AuthResponse(jwtToken);
    }
}