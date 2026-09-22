package com.example.order_service.controller;

import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderHistory;
import com.example.order_service.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Value("${trace.header-name:X-Trace-Id}")
    private String traceHeaderName;

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                   @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        String effectiveTraceId = traceId != null && !traceId.isBlank()
                ? traceId
                : UUID.randomUUID().toString();
        return orderService.createOrder(request.userId(), request.productId(), request.quantity(), effectiveTraceId, authorization);
    }

    @GetMapping("/{orderId}")
    public Mono<Order> getOrder(@PathVariable UUID orderId) {
        return orderService.getOrder(orderId);
    }

    @GetMapping("/{orderId}/history")
    public Flux<OrderHistory> getHistory(@PathVariable UUID orderId) {
        return orderService.getHistory(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public Mono<Order> cancelOrder(@PathVariable UUID orderId,
                                   @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String effectiveTraceId = traceId != null && !traceId.isBlank()
                ? traceId
                : UUID.randomUUID().toString();
        return orderService.cancelOrder(orderId, effectiveTraceId);
    }
}
