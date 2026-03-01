package com.sparkcore.backend.service;

import com.sparkcore.backend.util.RequestUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Distributed Rate Limiter für den Login-Endpunkt (Redis + Bucket4j).
 *
 * Strategie: Token Bucket – maximal 5 Anfragen pro IP pro Minute.
 * Da dieser Limitierer Redis nutzt, greift das Limit global über alle
 * Server-Instanzen hinweg (Horizontale Skalierung gesichert).
 */
@Service
public class RateLimiterService {

    // 5 Login-Versuche pro Minute pro IP
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final ProxyManager<byte[]> proxyManager;

    public RateLimiterService(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    /**
     * Gibt true zurück, wenn die Anfrage erlaubt ist (Token war verfügbar).
     * Gibt false zurück, wenn keine Tokens mehr im Bucket der IP sind → 429.
     */
    public boolean isAllowed(HttpServletRequest request) {
        String clientIp = RequestUtils.getClientIp(request);
        byte[] key = clientIp.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Sucht den Bucket in Redis oder erstellt einen neuen, falls er noch nicht
        // existiert
        io.github.bucket4j.Bucket bucket = proxyManager.builder().build(key, this::createBucketConfig);

        // Versuche, genau 1 Token aus dem Bucket zu "konsumieren"
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        return probe.isConsumed();
    }

    /**
     * Löscht den Bucket aus Redis (z.B. nach erfolgreichem Login),
     * damit der Nutzer sofort wieder das volle Limit für neue Sessions hat.
     */
    public void resetAttempts(HttpServletRequest request) {
        String clientIp = RequestUtils.getClientIp(request);
        byte[] key = clientIp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        proxyManager.removeProxy(key);
    }

    /**
     * Konfiguration des Buckets: Wir vergeben 5 Tokens, die sich im Laufe
     * einer Minute kontinuierlich wieder auffüllen ("refillGreedy").
     */
    private BucketConfiguration createBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(MAX_ATTEMPTS)
                        .refillGreedy(MAX_ATTEMPTS, WINDOW)
                        .build())
                .build();
    }
}
