package com.example.ares.common;

import java.time.Instant;

/**
 * Stable error response returned by ARES operations APIs.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
