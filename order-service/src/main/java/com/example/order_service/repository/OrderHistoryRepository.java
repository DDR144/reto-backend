package com.example.order_service.repository;

import com.example.order_service.entity.OrderHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface OrderHistoryRepository extends R2dbcRepository<OrderHistory, UUID> {

    Flux<OrderHistory> findByOrderIdOrderByChangedAtAsc(UUID orderId);
}
