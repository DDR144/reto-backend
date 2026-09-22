package com.example.inventory_service.controller;

import com.example.inventory_service.dto.StockValidationRequest;
import com.example.inventory_service.dto.StockValidationResponse;
import com.example.inventory_service.entity.Product;
import com.example.inventory_service.exception.ProductNotFoundException;
import com.example.inventory_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    @Value("${trace.header-name:X-Trace-Id}")
    private String traceHeaderName;

    private final ProductRepository productRepository;

    public InventoryController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Consulta un producto y su stock actual.
     */
    @GetMapping("/{productId}")
    public Mono<Product> getProduct(@PathVariable UUID productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(productId)));
    }

    /**
     * Valida y descuenta stock de forma atómica (decisión: UPDATE condicional).
     * No confirma si el stock es insuficiente; el descuento solo ocurre cuando
     * el UPDATE afecta una fila (stock suficiente).
     */
    @PostMapping("/validate")
    public Mono<StockValidationResponse> validateStock(@RequestBody StockValidationRequest request,
                                                       @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String effectiveTraceId = traceId != null && !traceId.isBlank()
                ? traceId
                : UUID.randomUUID().toString();

        return productRepository.findById(request.productId())
                .switchIfEmpty(Mono.error(new ProductNotFoundException(request.productId())))
                .flatMap(product -> productRepository.deductStock(request.productId(), request.quantity())
                        .map(rows -> {
                            if (rows != null && rows > 0) {
                                log.info("[traceId={}] Stock descontado para producto {} (cantidad {})",
                                        effectiveTraceId, request.productId(), request.quantity());
                                return new StockValidationResponse(
                                        request.productId(),
                                        StockValidationResponse.STATUS_CONFIRMED,
                                        effectiveTraceId);
                            }
                            log.warn("[traceId={}] Stock insuficiente para producto {} (cantidad {}, disponible {})",
                                    effectiveTraceId, request.productId(), request.quantity(), product.getStock());
                            return new StockValidationResponse(
                                    request.productId(),
                                    StockValidationResponse.STATUS_STOCK_INSUFFICIENT,
                                    effectiveTraceId);
                        }));
    }
}
