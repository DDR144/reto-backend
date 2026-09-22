package com.example.order_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Stock insuficiente en el Inventory Service al intentar confirmar un pedido.
 */
public class StockInsufficientException extends BusinessException {
    public StockInsufficientException(String message) {
        super(HttpStatus.CONFLICT.value(), "STOCK_INSUFFICIENT", message);
    }
}
