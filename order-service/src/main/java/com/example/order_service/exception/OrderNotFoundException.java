package com.example.order_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Pedido no encontrado.
 */
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND.value(), "ORDER_NOT_FOUND", message);
    }
}
