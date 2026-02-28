package com.sparkcore.backend.service;

import com.sparkcore.backend.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schlichter In-Memory Rate Limiter für den Login-Endpunkt.
 *
 * Strategie: Sliding Window – maximal MAX_ATTEMPTS Anfragen pro IP
 * innerhalb eines WINDOW_MILLIS-Zeitfensters.
 *
 * Hinweis: Für Multi-Instance-Deployments sollte dies durch einen
 * Redis-backed Rate Limiter (z.B. Bucket4j + Redis) ersetzt werden.
 */
@Service
public class RateLimiterService {

    // Maximale Login-Versuche im Zeitfenster
    private static final int MAX_ATTEMPTS = 5;

    // Zeitfenster: 1 Minute
    private static final long WINDOW_MILLIS = 60_000L;

    // IP-Adresse → Zeitstempel der letzten Anfragen im Fenster
    private final ConcurrentHashMap<String, Deque<Long>> attemptHistory = new ConcurrentHashMap<>();

    /**
     * Gibt true zurück, wenn die Anfrage erlaubt ist (Limit nicht erreicht).
     * Gibt false zurück, wenn das Limit überschritten wurde → 429 senden.
     */
    public boolean isAllowed(HttpServletRequest request) {
        String clientIp = RequestUtils.getClientIp(request);
        long now = System.currentTimeMillis();

        // Atomic update: alte Einträge raus, neuen Zeitstempel rein
        attemptHistory.compute(clientIp, (ip, history) -> {
            Deque<Long> window = (history != null) ? history : new ArrayDeque<>();
            // Versuche außerhalb des Zeitfensters entfernen
            window.removeIf(timestamp -> now - timestamp > WINDOW_MILLIS);
            window.addLast(now);
            return window;
        });

        return attemptHistory.get(clientIp).size() <= MAX_ATTEMPTS;
    }

    /**
     * Setzt den Zähler für eine IP zurück (z.B. nach erfolgreichem Login).
     */
    public void resetAttempts(HttpServletRequest request) {
        String clientIp = RequestUtils.getClientIp(request);
        attemptHistory.remove(clientIp);
    }
}
