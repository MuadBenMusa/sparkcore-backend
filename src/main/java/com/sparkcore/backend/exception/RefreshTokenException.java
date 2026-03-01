package com.sparkcore.backend.exception;

/**
 * Wird geworfen, wenn ein Refresh Token ungültig, abgelaufen oder nicht
 * gefunden wurde. Erlaubt dem GlobalExceptionHandler, eine strukturierte
 * 400 Bad Request JSON-Antwort zurückzugeben.
 */
public class RefreshTokenException extends RuntimeException {

    public RefreshTokenException(String message) {
        super(message);
    }
}
