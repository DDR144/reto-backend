package com.example.inventory_service.dto;

import java.util.UUID;

public record StockValidationRequest(
        UUID productId,
        int quantity
) {
}
