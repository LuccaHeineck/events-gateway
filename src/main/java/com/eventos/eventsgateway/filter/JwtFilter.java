package com.eventos.eventsgateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtFilter implements WebFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "/auth/login",
            "/auth/register"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        // Rotas públicas
        if (PUBLIC_ROUTES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized();
        }

        String token = authHeader.substring(7);

        try {
            JwtUtils.validateToken(token, jwtSecret);
            return chain.filter(exchange);
        } catch (Exception e) {
            return unauthorized();
        }
    }

    private Mono<Void> unauthorized() {
        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido ou ausente"));
    }
}
