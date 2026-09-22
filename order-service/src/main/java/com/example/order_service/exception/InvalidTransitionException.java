package com.example.order_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Transición de estado inválida (p. ej. cancelar un pedido ya cancelado).
 */
public class InvalidTransitionException extends BusinessException {
    public InvalidTransitionException(String message) {
        super(HttpStatus.CONFLICT.value(), "INVALID_TRANSITION", message);
    }
}
