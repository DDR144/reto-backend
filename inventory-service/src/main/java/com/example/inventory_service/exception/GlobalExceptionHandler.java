package com.example.inventory_service.exception;

import com.example.inventory_service.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ProductNotFoundException ex, ServerWebExchange exchange) {
        log.warn("[traceId={}] {}", traceId(exchange), ex.getMessage());
        return build(exchange, ex.getHttpStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, ServerWebExchange exchange) {
        log.error("[traceId={}] Error inesperado", traceId(exchange), ex);
        return build(exchange, HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Unexpected error");
    }

    private ResponseEntity<ErrorResponse> build(ServerWebExchange exchange, int status, String code, String message) {
        ErrorResponse body = new ErrorResponse(OffsetDateTime.now(), status, code, message, traceId(exchange));
        return ResponseEntity.status(status).body(body);
    }

    private String traceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        return traceId == null ? "" : traceId;
    }
}
