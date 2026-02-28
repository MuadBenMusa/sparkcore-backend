package com.sparkcore.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestUtils {

    private RequestUtils() {
        // Utils-Klasse soll nicht instanziiert werden
    }

    // Hilfsmethode: IP-Adresse aus dem aktuellen Request holen (für Service-Layer)
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest().getRemoteAddr() : "UNKNOWN_IP";
    }

    // Überladung: IP-Adresse direkt aus dem HttpServletRequest lesen (für Filter)
    // Berücksichtigt X-Forwarded-For für Deployments hinter einem Reverse Proxy
    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Erster Eintrag ist die Original-IP (bei mehreren Proxies)
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
