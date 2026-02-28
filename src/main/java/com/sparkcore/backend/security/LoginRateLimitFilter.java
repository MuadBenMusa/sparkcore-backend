package com.sparkcore.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkcore.backend.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rate-Limiting-Filter für den Login-Endpunkt.
 *
 * Greift NUR bei POST /api/v1/auth/login – alle anderen Endpunkte
 * werden ohne Prüfung durchgelassen (shouldNotFilter).
 *
 * Bei Überschreitung des Limits → HTTP 429 Too Many Requests.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public LoginRateLimitFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    // Filter nur für den Login-Endpunkt aktivieren
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("/api/v1/auth/login".equals(request.getServletPath())
                && "POST".equalsIgnoreCase(request.getMethod()));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Rate Limit prüfen
        if (!rateLimiterService.isAllowed(request)) {
            // 429 zurückgeben mit JSON-Fehlerantwort
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", LocalDateTime.now().toString());
            errorBody.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
            errorBody.put("error", "Too Many Requests");
            errorBody.put("message", "Zu viele Login-Versuche. Bitte warte 1 Minute und versuche es erneut.");

            objectMapper.writeValue(response.getWriter(), errorBody);
            return; // Request abblocken – nicht weiterleiten
        }

        // Limit nicht erreicht → normal weiterleiten
        filterChain.doFilter(request, response);
    }
}
