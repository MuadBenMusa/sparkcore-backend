package com.sparkcore.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Hat der Request einen "Authorization" Header, der mit "Bearer " anfängt?
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Kein Token? Weitergeben (wird dann später blockiert)
            return;
        }

        // 2. Token extrahieren (ab dem 7. Zeichen, da "Bearer " 7 Zeichen lang ist)
        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);

        // 3. Wenn ein Name im Token steht und der User noch NICHT im System authentifiziert ist...
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Lade den User aus der Datenbank
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 4. Prüfe, ob das Token wirklich gültig ist
            if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {

                // 5. Dem Türsteher (Spring Security) sagen: "Der Typ ist cool, lass ihn rein!"
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // User im "Tresor" (Security Context) für diesen Request speichern
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. Den Request an den eigentlichen Controller weiterleiten
        filterChain.doFilter(request, response);
    }
}