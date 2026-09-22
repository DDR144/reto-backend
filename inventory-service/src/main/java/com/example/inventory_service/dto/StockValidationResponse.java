package com.example.inventory_service.dto;

import java.util.UUID;

public record StockValidationResponse(
        UUID productId,
        String status,
        String traceId
) {
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_STOCK_INSUFFICIENT = "STOCK_INSUFFICIENT";
    public static final String STATUS_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
}
