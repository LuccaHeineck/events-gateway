package com.eventos.eventsgateway.controller;

import com.eventos.eventsgateway.service.ProxyService;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping(
            value = {"/usuarios/**", "/auth/**", "/eventos/**", "/send-email"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
            produces = "application/json"
    )
    public Mono<String> proxy(ServerHttpRequest request,
                              @RequestBody(required = false) Mono<String> body) {

        String path = request.getURI().getPath().substring(1);
        return proxyService.forward(path, request.getMethod(), request.getHeaders(), body);
    }
}
