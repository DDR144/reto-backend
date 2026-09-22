package com.example.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/**
 * Configuración de seguridad del API Gateway.
 * Responsabilidades: validación de JWT (OAuth 2.0/OIDC), rechazo de peticiones
 * no autenticadas con formato de error estándar, sin lógica de negocio.
 * El decodificador JWT reactivo se auto-configura desde las propiedades
 * spring.security.oauth2.resourceserver.jwt.*.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${trace.header-name:X-Trace-Id}")
    private String traceHeaderName;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authenticationEntryPoint()));
        return http.build();
    }

    /**
     * Punto de entrada para peticiones sin token o con token inválido/expirado.
     * Devuelve 401 con el formato estándar { timestamp, status, code, message, traceId }.
     */
    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            String traceId = exchange.getRequest().getHeaders().getFirst(traceHeaderName);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String body = String.format(
                    "{\"timestamp\":\"%s\",\"status\":401,\"code\":\"UNAUTHORIZED\",\"message\":\"Missing or invalid token\",\"traceId\":\"%s\"}",
                    OffsetDateTime.now(), traceId == null ? "" : traceId);
            DataBuffer buffer = exchange.getResponse().bufferFactory()
                    .wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        };
    }
}
