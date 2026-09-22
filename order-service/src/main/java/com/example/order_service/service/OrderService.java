package com.example.order_service.service;

import com.example.order_service.client.InventoryClient;
import com.example.order_service.dto.InventoryValidationResponse;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderHistory;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.exception.InvalidTransitionException;
import com.example.order_service.exception.OrderNotFoundException;
import com.example.order_service.exception.StockInsufficientException;
import com.example.order_service.repository.OrderHistoryRepository;
import com.example.order_service.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lógica de negocio de pedidos.
 * Flujo de creación: PENDING -> validar stock -> CONFIRMED | STOCK_INSUFFICIENT.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderHistoryRepository historyRepository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository,
                        OrderHistoryRepository historyRepository,
                        InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.inventoryClient = inventoryClient;
    }

    /**
     * Crea un pedido en PENDING, valida stock contra Inventory y confirma o rechaza.
     */
    public Mono<Order> createOrder(String userId, UUID productId, int quantity, String traceId, String bearerToken) {
        Order order = new Order();
        order.setOrderId(UUID.randomUUID());
        order.setUserId(userId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setStatus(OrderStatus.PENDING);
        order.setTraceId(traceId);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setNew(true);

        return orderRepository.save(order)
                .flatMap(saved -> {
                    saved.setNew(false);
                    log.info("[traceId={}] Pedido {} creado en PENDING", traceId, saved.getOrderId());
                    return recordHistory(saved.getOrderId(), null, OrderStatus.PENDING, traceId)
                            .thenReturn(saved);
                })
                .flatMap(saved -> inventoryClient.validateStock(productId, quantity, traceId, bearerToken)
                        .flatMap(result -> {
                            if (result.isConfirmed()) {
                                saved.setStatus(OrderStatus.CONFIRMED);
                                saved.setUpdatedAt(LocalDateTime.now());
                                return orderRepository.save(saved)
                                        .flatMap(confirmed -> recordHistory(
                                                confirmed.getOrderId(), OrderStatus.PENDING, OrderStatus.CONFIRMED, traceId)
                                                .thenReturn(confirmed))
                                        .doOnSuccess(c -> log.info("[traceId={}] Pedido {} confirmado", traceId, c.getOrderId()));
                            }
                            log.warn("[traceId={}] Stock insuficiente para producto {} en pedido {}", traceId, productId, saved.getOrderId());
                            return Mono.error(new StockInsufficientException(
                                    "Insufficient stock for product " + productId));
                        }));
    }

    /**
     * Consulta el estado actual de un pedido.
     */
    public Mono<Order> getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found: " + orderId)));
    }

    /**
     * Consulta el historial de cambios de estado de un pedido.
     */
    public Flux<OrderHistory> getHistory(UUID orderId) {
        return historyRepository.findByOrderIdOrderByChangedAtAsc(orderId);
    }

    /**
     * Cancela un pedido si su estado lo permite (no CANCELLED).
     */
    public Mono<Order> cancelOrder(UUID orderId, String traceId) {
        return orderRepository.findById(orderId)
                .switchIfEmpty(Mono.error(new OrderNotFoundException("Order not found: " + orderId)))
                .flatMap(order -> {
                    if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
                        return Mono.error(new InvalidTransitionException(
                                "Cannot cancel order in state " + order.getStatus()));
                    }
                    OrderStatus previous = order.getStatus();
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setUpdatedAt(LocalDateTime.now());
                    return orderRepository.save(order)
                            .flatMap(cancelled -> recordHistory(cancelled.getOrderId(), previous, OrderStatus.CANCELLED, traceId)
                                    .thenReturn(cancelled))
                            .doOnSuccess(c -> log.info("[traceId={}] Pedido {} cancelado desde {}", traceId, c.getOrderId(), previous));
                });
    }

    private Mono<OrderHistory> recordHistory(UUID orderId, OrderStatus from, OrderStatus to, String traceId) {
        OrderHistory history = new OrderHistory();
        history.setHistoryId(UUID.randomUUID());
        history.setOrderId(orderId);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setChangedAt(LocalDateTime.now());
        history.setTraceId(traceId);
        history.setNew(true);
        return historyRepository.save(history);
    }
}
