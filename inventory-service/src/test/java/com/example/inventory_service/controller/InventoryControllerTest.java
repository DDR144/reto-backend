package com.example.inventory_service.controller;

import com.example.inventory_service.dto.StockValidationRequest;
import com.example.inventory_service.entity.Product;
import com.example.inventory_service.exception.ProductNotFoundException;
import com.example.inventory_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del controlador de inventario (validación de stock).
 */
@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    @Mock
    private ProductRepository productRepository;

    private InventoryController controller;

    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new InventoryController(productRepository);
    }

    @Test
    void validateStock_confirmsWhenDeductAffectsRow() {
        Product product = new Product();
        product.setProductId(productId);
        product.setStock(10);

        when(productRepository.findById(productId)).thenReturn(Mono.just(product));
        when(productRepository.deductStock(productId, 2)).thenReturn(Mono.just(1));

        StepVerifier.create(controller.validateStock(new StockValidationRequest(productId, 2), "trace"))
                .assertNext(response -> {
                    assertThat(response.status()).isEqualTo("CONFIRMED");
                    assertThat(response.traceId()).isEqualTo("trace");
                    assertThat(response.productId()).isEqualTo(productId);
                })
                .verifyComplete();

        verify(productRepository).deductStock(productId, 2);
    }

    @Test
    void validateStock_insufficientWhenDeductAffectsZeroRows() {
        Product product = new Product();
        product.setProductId(productId);
        product.setStock(0);

        when(productRepository.findById(productId)).thenReturn(Mono.just(product));
        when(productRepository.deductStock(productId, 999)).thenReturn(Mono.just(0));

        StepVerifier.create(controller.validateStock(new StockValidationRequest(productId, 999), "trace"))
                .assertNext(response -> assertThat(response.status()).isEqualTo("STOCK_INSUFFICIENT"))
                .verifyComplete();
    }

    @Test
    void validateStock_throwsWhenProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.validateStock(new StockValidationRequest(productId, 1), "trace"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void getProduct_returnsProductWhenFound() {
        Product product = new Product();
        product.setProductId(productId);
        product.setName("Laptop");
        product.setStock(5);

        when(productRepository.findById(productId)).thenReturn(Mono.just(product));

        StepVerifier.create(controller.getProduct(productId))
                .assertNext(found -> {
                    assertThat(found.getName()).isEqualTo("Laptop");
                    assertThat(found.getStock()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    void getProduct_throwsWhenNotFound() {
        when(productRepository.findById(productId)).thenReturn(Mono.empty());

        StepVerifier.create(controller.getProduct(productId))
                .expectError(ProductNotFoundException.class)
                .verify();
    }
}
