package com.sparkcore.backend.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestUtils {

    private RequestUtils() {
        // Utils-Klasse soll nicht instanziiert werden
    }

    // Hilfsmethode: IP-Adresse aus dem aktuellen Request holen
    public static String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attributes != null) ? attributes.getRequest().getRemoteAddr() : "UNKNOWN_IP";
    }
}
