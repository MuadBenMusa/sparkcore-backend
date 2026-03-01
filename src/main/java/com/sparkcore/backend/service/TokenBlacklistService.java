package com.sparkcore.backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Service für das Blacklisting von JWTs.
 * Wenn ein User sich ausloggt (Logout), wird das aktuelle Access Token
 * hier gespeichert. Das Token bleibt in Redis, bis es sowieso abgelaufen
 * wäre (TTL), danach löscht Redis es automatisch.
 */
@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    // Präfix, um Kollisionen mit anderen Cache-Keys (z.B. Rate-Limiting) zu
    // vermeiden
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Setzt ein Token auf die Blacklist.
     * 
     * @param token                   Das rohe JWT (ohne "Bearer ")
     * @param durationUntilExpiration Die Restlebenszeit des Tokens. Nach dieser
     *                                Zeit löscht Redis den Eintrag.
     */
    public void blacklistToken(String token, Duration durationUntilExpiration) {
        redisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + token,
                "revoked",
                durationUntilExpiration);
    }

    /**
     * Prüft, ob ein Token auf der Blacklist steht.
     * 
     * @param token Das rohe JWT
     * @return true, wenn das Token widerrufen (ausgeloggt) wurde.
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
    }
}
