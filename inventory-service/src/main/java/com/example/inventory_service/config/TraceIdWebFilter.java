package com.example.inventory_service.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdWebFilter implements WebFilter {

    @Value("${trace.header-name:X-Trace-Id}")
    private String traceHeaderName;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(traceHeaderName);
        if (traceId != null && !traceId.isBlank()) {
            MDC.put("traceId", traceId);
        }
        return chain.filter(exchange).doFinally(signalType -> MDC.remove("traceId"));
    }
}
