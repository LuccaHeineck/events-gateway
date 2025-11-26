package com.eventos.eventsgateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(1)
public class LoggingFilter implements WebFilter {


    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if ("/health".equals(path)) {
            return chain.filter(exchange);
        }

        logger.info("Request: {} {}", exchange.getRequest().getMethod(), path);
        exchange.getRequest().getHeaders()
                .forEach((key, value) -> logger.info("{}: {}", key, value));

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> logger.info("Response Status Code: {}", exchange.getResponse().getStatusCode()));
    }
}
