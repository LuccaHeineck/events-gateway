package com.eventos.eventsgateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

@Component
@Order(0)
public class AuthVerificationFilter implements WebFilter {

    @Value("${user.service.url}")
    private String userServiceUrl;

    private final WebClient webClient;

    public AuthVerificationFilter(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        if (path.startsWith("/auth") || path.equals("/health")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        return webClient.post()
                .uri(userServiceUrl + "/auth/verify")
                .header("Authorization", authHeader)
                .retrieve()
                .toBodilessEntity()
                .then(chain.filter(exchange))
                .onErrorResume(e -> unauthorized(exchange));
    }


    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
