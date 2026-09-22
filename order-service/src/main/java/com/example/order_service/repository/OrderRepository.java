package com.example.order_service.repository;

import com.example.order_service.entity.Order;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, UUID> {

    Mono<Order> findById(UUID orderId);

    Mono<Order> findByTraceId(String traceId);
}
