package com.eventos.eventsgateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;

@Service
public class ProxyService {

    private final WebClient webClient;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    @Value("${event.service.url}")
    private String eventServiceUrl;

    // Chave secreta compartilhada entre o Gateway e o serviço de e-mail
    @Value("${gateway.secret}")
    private String gatewaySecret;

    public ProxyService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> forward(String path, HttpMethod method, HttpHeaders headers, Mono<String> body) {
        String targetBaseUrl;

        if (path.startsWith("usuarios")) {
            targetBaseUrl = userServiceUrl;
        } else if (path.startsWith("auth")) {
            targetBaseUrl = userServiceUrl;
        } else if (path.startsWith("send-email")) {
            targetBaseUrl = emailServiceUrl;
        } else if (path.startsWith("eventos")) {
            targetBaseUrl = eventServiceUrl;
        } else {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Rota de serviço desconhecida"));
        }

        String targetUrl = targetBaseUrl + "/" + path;

        System.out.println("TARGET: " + targetUrl);

        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(targetUrl)
                .headers(h -> {
                    h.addAll(headers);
                    h.add("X-Gateway-Key", gatewaySecret);
                });

        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            spec.body(body, String.class);
        }

        return spec.retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ResponseStatusException(
                                response.statusCode(), "Erro no serviço alvo"))
                )
                .bodyToMono(String.class);
    }
}
