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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de la lógica de negocio de pedidos.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderHistoryRepository historyRepository;

    @Mock
    private InventoryClient inventoryClient;

    private OrderService orderService;

    private final UUID productId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, historyRepository, inventoryClient);
        // Comportamiento por defecto (lenient): save devuelve la misma entidad (INSERT o UPDATE).
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        lenient().when(historyRepository.save(any(OrderHistory.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    @Test
    void createOrder_confirmsWhenStockAvailable() {
        when(inventoryClient.validateStock(eq(productId), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(new InventoryValidationResponse(productId, "CONFIRMED", "trace")));

        StepVerifier.create(orderService.createOrder("user", productId, 2, "trace", "Bearer token"))
                .assertNext(order -> {
                    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                    assertThat(order.getOrderId()).isNotNull();
                    assertThat(order.getUserId()).isEqualTo("user");
                    assertThat(order.getQuantity()).isEqualTo(2);
                    assertThat(order.getTraceId()).isEqualTo("trace");
                })
                .verifyComplete();

        // Primer save = INSERT en PENDING; segundo save = UPDATE a CONFIRMED.
        verify(orderRepository, times(2)).save(any(Order.class));
        // Dos cambios de estado: (null -> PENDING) y (PENDING -> CONFIRMED).
        verify(historyRepository, times(2)).save(any(OrderHistory.class));
        verify(inventoryClient).validateStock(eq(productId), eq(2), eq("trace"), eq("Bearer token"));
    }

    @Test
    void createOrder_failsWhenStockInsufficient() {
        when(inventoryClient.validateStock(eq(productId), anyInt(), anyString(), anyString()))
                .thenReturn(Mono.just(new InventoryValidationResponse(productId, "STOCK_INSUFFICIENT", "trace")));

        StepVerifier.create(orderService.createOrder("user", productId, 999, "trace", "Bearer"))
                .expectError(StockInsufficientException.class)
                .verify();
    }

    @Test
    void getOrder_returnsOrderWhenFound() {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.getOrder(orderId))
                .assertNext(found -> assertThat(found.getOrderId()).isEqualTo(orderId))
                .verifyComplete();
    }

    @Test
    void getOrder_throwsWhenNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrder(orderId))
                .expectError(OrderNotFoundException.class)
                .verify();
    }

    @Test
    void cancelOrder_cancelsConfirmedOrder() {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.cancelOrder(orderId, "trace"))
                .assertNext(cancelled -> assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED))
                .verifyComplete();

        verify(historyRepository).save(any(OrderHistory.class));
    }

    @Test
    void cancelOrder_throwsWhenAlreadyCancelled() {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(orderId)).thenReturn(Mono.just(order));

        StepVerifier.create(orderService.cancelOrder(orderId, "trace"))
                .expectError(InvalidTransitionException.class)
                .verify();
    }

    @Test
    void cancelOrder_throwsWhenNotFound() {
        when(orderRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.cancelOrder(orderId, "trace"))
                .expectError(OrderNotFoundException.class)
                .verify();
    }

    @Test
    void getHistory_returnsEmptyWhenNoRecords() {
        when(historyRepository.findByOrderIdOrderByChangedAtAsc(orderId))
                .thenReturn(reactor.core.publisher.Flux.empty());

        StepVerifier.create(orderService.getHistory(orderId))
                .verifyComplete();
    }
}
