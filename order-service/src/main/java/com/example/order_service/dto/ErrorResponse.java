package com.example.order_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

/**
 * Formato estándar de error exigido por el PRD:
 * { timestamp, status, code, message, traceId }
 */
public record ErrorResponse(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX") OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String traceId
) {
}
