package com.example.order_service.entity;

/**
 * Estados de un pedido (decisión: enum simple persistido como STRING).
 * Transiciones válidas:
 *   PENDING   -> CONFIRMED  (stock disponible)
 *   PENDING   -> CANCELLED
 *   CONFIRMED -> CANCELLED
 *   CANCELLED -> (terminal, sin transiciones)
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED;

    /**
     * Indica si la transición {@code to} es válida desde este estado.
     */
    public boolean canTransitionTo(OrderStatus to) {
        if (this == CANCELLED) {
            return false;
        }
        return switch (this) {
            case PENDING -> to == CONFIRMED || to == CANCELLED;
            case CONFIRMED -> to == CANCELLED;
            case CANCELLED -> false;
        };
    }
}
