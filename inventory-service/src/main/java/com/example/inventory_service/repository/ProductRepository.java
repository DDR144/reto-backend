package com.example.inventory_service.repository;

import com.example.inventory_service.entity.Product;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, UUID> {

    Mono<Product> findById(UUID productId);

    /**
     * Decrementa el stock de forma atómica y condicional.
     * Solo actualiza si el stock resultante no queda negativo.
     * Devuelve el número de filas afectadas (0 o 1).
     */
    @Modifying
    @Query("UPDATE products SET stock = stock - :quantity, updated_at = NOW() " +
            "WHERE product_id = :productId AND stock >= :quantity")
    Mono<Integer> deductStock(@Param("productId") UUID productId, @Param("quantity") int quantity);
}
