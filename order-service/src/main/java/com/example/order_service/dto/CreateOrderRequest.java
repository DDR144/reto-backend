package com.example.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Petición de creación de pedido.
 */
public record CreateOrderRequest(
        @NotBlank(message = "userId is required") String userId,
        @NotNull(message = "productId is required") UUID productId,
        @NotNull(message = "quantity is required") @Min(value = 1, message = "quantity must be at least 1") Integer quantity
) {
}
