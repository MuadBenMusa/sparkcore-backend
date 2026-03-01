package com.sparkcore.backend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

import io.jsonwebtoken.Claims;
import java.util.function.Function;

@Service
public class JwtService {

    // Ein geheimer Schlüssel (mindestens 256-bit), der NUR dem Server bekannt ist!
    // In einer echten Bank läge dieser Key in einem sicheren "Vault" und niemals im
    // Code.
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    // Bereitet den Schlüssel für die Verschlüsselung vor
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username) // Wer ist der Nutzer?
                .issuedAt(new Date(System.currentTimeMillis())) // Ausgestellt jetzt
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // Gültig für 15 MINUTEN
                .signWith(getSigningKey()) // Digitale Unterschrift der Bank
                .compact();
    }

    // 1. Liest den Benutzernamen (Subject) aus dem Token aus
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 2. Prüft, ob der Token zum User passt und noch nicht abgelaufen ist
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // 3. Hilfsmethoden für das Lesen der Token-Daten (Claims)
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Prüft die digitale Unterschrift mit unserem geheimen Schlüssel!
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}