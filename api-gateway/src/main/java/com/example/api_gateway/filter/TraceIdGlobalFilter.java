package com.example.api_gateway.filter;

import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Filtro global de trazabilidad end-to-end.
 * - Si la petición llega con X-Trace-Id, lo propaga tal cual (decisión: "ambos").
 * - Si no viene, genera un UUID v4.
 * - Lo inyecta en las cabeceras hacia los microservicios, en la respuesta y en el MDC
 *   para que Logback lo incluya en cada línea de log.
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);

    @Value("${trace.header-name:X-Trace-Id}")
    private String traceHeaderName;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(traceHeaderName);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalTraceId = traceId;

        // Propaga la cabecera hacia el microservicio destino.
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(traceHeaderName, finalTraceId)
                .build();

        // Añade la cabecera a la respuesta para que el cliente pueda correlacionar.
        exchange.getResponse().getHeaders().add(traceHeaderName, finalTraceId);

        MDC.put("traceId", finalTraceId);
        log.info("{} {} [X-Trace-Id={}]", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(), finalTraceId);
        try {
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .doFinally(signalType -> MDC.remove("traceId"));
        } catch (Throwable t) {
            MDC.remove("traceId");
            return Mono.error(t);
        }
    }

    @Override
    public int getOrder() {
        // Debe ejecutarse muy temprano para que el trace id esté disponible en logs y filtros de seguridad.
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
