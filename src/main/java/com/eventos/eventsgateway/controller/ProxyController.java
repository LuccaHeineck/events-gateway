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
            value = {"/usuarios/**",
                    "/auth/**",
                    "/eventos/**",
                    "/send-email"},
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}
    )
    public Mono<String> proxy(ServerHttpRequest request,
                              @RequestBody(required = false) Mono<String> body) {
        // Extrai caminho sem o primeiro / -> "usuarios/editar"
        String path = request.getURI().getPath().substring(1);

        // Envia method, path, headers, and body para ProxyService
        return proxyService.forward(path, request.getMethod(), request.getHeaders(), body);
    }
}
