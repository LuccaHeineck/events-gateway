package com.eventos.eventsgateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Order(1)
public class LoggingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();
        var path = request.getURI().getPath();

        if ("/health".equals(path)) {
            return chain.filter(exchange);
        }

        logger.info("Request: {} {}", request.getMethod(), path);

        request.getHeaders()
                .forEach((k, v) -> logger.info("{}: {}", k, v));

        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String bodyString = new String(bytes);
                    logger.info("Request Body: {}", bodyString);

                    Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                        DataBuffer buffer = exchange.getResponse()
                                .bufferFactory()
                                .wrap(bytes);
                        return Mono.just(buffer);
                    });

                    ServerHttpRequestDecorator decorated = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return cachedFlux;
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorated).build())
                            .doOnSuccess(aVoid ->
                                    logger.info("Response Status Code: {}", exchange.getResponse().getStatusCode())
                            );
                });
    }
}


