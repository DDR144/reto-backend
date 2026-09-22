package com.example.order_service.exception;

import com.example.order_service.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;

import java.time.OffsetDateTime;

/**
 * Manejo global de errores con el formato estándar
 * { timestamp, status, code, message, traceId }.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, ServerWebExchange exchange) {
        log.warn("[traceId={}] Error de negocio {}: {}", traceId(exchange), ex.getCode(), ex.getMessage());
        return build(exchange, ex.getHttpStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        return build(exchange, HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("[traceId={}] Error inesperado", traceId(exchange), ex);
        return build(exchange, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Unexpected error");
    }

    private ResponseEntity<ErrorResponse> build(ServerWebExchange exchange, int status, String code, String message) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                status,
                code,
                message,
                traceId(exchange));
        return ResponseEntity.status(status).body(body);
    }

    private String traceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        return traceId == null ? "" : traceId;
    }
}
