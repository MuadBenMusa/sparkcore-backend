package com.sparkcore.backend.service;

import com.sparkcore.backend.model.AppUser;
import com.sparkcore.backend.model.RefreshToken;
import com.sparkcore.backend.repository.RefreshTokenRepository;
import com.sparkcore.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    // Refresh Tokens sind 7 Tage gültig
    private static final long REFRESH_TOKEN_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 7;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Erstellt ein neues Refresh Token für den angegebenen User.
     * Wenn der User bereits ein altes Refresh Token hat, wird dieses überschrieben
     * (Token Rotation / Single Device Enforced für dieses Beispiel).
     */
    public RefreshToken createRefreshToken(String username) {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User nicht gefunden"));

        // Altes Token löschen (Token Rotation erzwingen)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID().toString(),
                user,
                Instant.now().plusMillis(REFRESH_TOKEN_EXPIRATION_MS));

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Prüft das Token, ob es in der Datenbank existiert und noch gültig ist.
     */
    public Optional<RefreshToken> verifyExpiration(String token) {
        return refreshTokenRepository.findByToken(token)
                .map(refreshToken -> {
                    if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
                        refreshTokenRepository.delete(refreshToken);
                        throw new RuntimeException("Refresh Token ist abgelaufen. Bitte neu anmelden.");
                    }
                    return refreshToken;
                });
    }

    /**
     * Löscht das Token manuell ab (z.B. bei Logout).
     */
    public void deleteByUser(AppUser user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
