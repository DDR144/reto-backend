package com.example.order_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Historial de cambios de estado de un pedido.
 * Decisión: un único registro por cambio de estado.
 */
@Table("order_history")
public class OrderHistory implements Persistable<UUID> {

    @Id
    @Column("history_id")
    private UUID historyId;

    @Column("order_id")
    private UUID orderId;

    @Column("from_status")
    private OrderStatus fromStatus;

    @Column("to_status")
    private OrderStatus toStatus;

    @Column("changed_at")
    private LocalDateTime changedAt;

    @Column("trace_id")
    private String traceId;

    @Transient
    private boolean isNew = false;

    public OrderHistory() {
    }

    @JsonIgnore
    @Override
    public UUID getId() {
        return historyId;
    }

    @JsonIgnore
    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public UUID getHistoryId() {
        return historyId;
    }

    public void setHistoryId(UUID historyId) {
        this.historyId = historyId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(OrderStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public OrderStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(OrderStatus toStatus) {
        this.toStatus = toStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
