package com.example.order_service.dto;

import java.util.UUID;

/**
 * Respuesta del Inventory Service al validar/descontar stock.
 */
public record InventoryValidationResponse(
        UUID productId,
        String status,
        String traceId
) {
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_STOCK_INSUFFICIENT = "STOCK_INSUFFICIENT";
    public static final String STATUS_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";

    public boolean isConfirmed() {
        return STATUS_CONFIRMED.equals(status);
    }
}
