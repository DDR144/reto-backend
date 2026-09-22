package com.example.order_service.client;

import com.example.order_service.dto.InventoryValidationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Cliente reactivo (WebClient) hacia el Inventory Service.
 * Usa comunicación síncrona no bloqueante conforme al PRD.
 */
@Component
public class InventoryClient {

    private final WebClient webClient;

    public InventoryClient(WebClient.Builder builder,
                           @Value("${inventory.service.url:http://localhost:8082}") String inventoryServiceUrl) {
        this.webClient = builder.baseUrl(inventoryServiceUrl).build();
    }

    /**
     * Solicita al Inventory Service validar y descontar stock de forma atómica.
     * Propaga el JWT (service-to-service) y el X-Trace-Id.
     */
    public Mono<InventoryValidationResponse> validateStock(UUID productId, int quantity, String traceId, String bearerToken) {
        return webClient.post()
                .uri("/api/v1/products/validate")
                .header("X-Trace-Id", traceId)
                .header("Authorization", bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("productId", productId.toString(), "quantity", quantity))
                .retrieve()
                .bodyToMono(InventoryValidationResponse.class);
    }
}
