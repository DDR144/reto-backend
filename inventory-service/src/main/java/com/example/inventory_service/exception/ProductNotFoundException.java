package com.example.inventory_service.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Producto no encontrado en el inventario.
 */
public class ProductNotFoundException extends RuntimeException {

    private final int httpStatus = HttpStatus.NOT_FOUND.value();
    private final String code = "PRODUCT_NOT_FOUND";

    public ProductNotFoundException(UUID productId) {
        super("Product not found: " + productId);
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }
}
