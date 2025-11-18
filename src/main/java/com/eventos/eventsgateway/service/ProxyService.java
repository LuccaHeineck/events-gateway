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
import org.springframework.http.*;

import java.util.Map;

@Service
public class ProxyService {

    private final WebClient webClient;

    @Value("${user.service.url}")
    private String userServiceUrl;

    @Value("${email.service.url}")
    private String emailServiceUrl;

    @Value("${event.service.url}")
    private String eventServiceUrl;

    @Value("${certificates.service.url}")
    private String certificatesServiceUrl;

    @Value("${gateway.secret}")
    private String gatewaySecret;

    private HttpMethod currentMethod;

    public ProxyService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<ResponseEntity<?>> forward(String path, HttpMethod method, HttpHeaders headers, Mono<String> body) {

        this.currentMethod = method;

        String targetBaseUrl;

        if (path.startsWith("usuarios")) {
            targetBaseUrl = userServiceUrl;
        } else if (path.startsWith("auth")) {
            targetBaseUrl = userServiceUrl;
        } else if (path.startsWith("inscricoes")) {
            targetBaseUrl = eventServiceUrl;
        } else if (path.startsWith("eventos")) {
            targetBaseUrl = eventServiceUrl;
        } else if (path.startsWith("send-email")) {
            targetBaseUrl = emailServiceUrl;
        } else if (path.startsWith("certificados")) {
            targetBaseUrl = certificatesServiceUrl;
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

        // ← troca fundamental: usar exchangeToMono porque retrieve() só trata JSON/texto
        return spec.exchangeToMono(clientResponse -> {

            // Pegar o content-type real retornado pelo serviço
            String contentType = clientResponse.headers().asHttpHeaders()
                    .getFirst("Content-Type");

            // Retorno é PDF → responder como binário
            if (contentType != null && contentType.contains("application/pdf")) {

                HttpHeaders responseHeaders = clientResponse.headers().asHttpHeaders();

                return clientResponse.bodyToMono(byte[].class)
                        .flatMap(bytes -> {

                            return executePostActionEmail(path, "OK")
                                    .thenReturn(
                                            ResponseEntity.ok()
                                                    .headers(responseHeaders)
                                                    .body(bytes)
                                    );
                        });
            }

            // Caso normal (JSON / texto)
            return clientResponse.bodyToMono(String.class)
                    .flatMap(text -> {
                        return executePostActionEmail(path, text)
                                .thenReturn(
                                        ResponseEntity.status(clientResponse.statusCode())
                                                .contentType(clientResponse.headers().contentType().orElse(null))
                                                .body(text)
                                );
                    });
        });
    }


    private Mono<Void> executePostActionEmail(String path, String responseBody) {

        // POST /inscricoes → inscrever
        if (path.equals("inscricoes") && methodIs(HttpMethod.POST)) {
            return sendEmail("Inscrição realizada", "Sua inscrição foi registrada.");
        }

        // PUT /inscricoes/{id} → cancelar
        if (path.matches("inscricoes/[0-9]+") && methodIs(HttpMethod.PUT)) {
            return sendEmail("Inscrição cancelada", "Sua inscrição foi cancelada.");
        }

        // POST /inscricoes/{id}/checkin → confirmar presença
        if (path.matches("inscricoes/[0-9]+/checkin") && methodIs(HttpMethod.POST)) {
            return sendEmail("Presença confirmada", "Sua presença foi confirmada.");
        }

        return Mono.empty();
    }

    private boolean methodIs(HttpMethod m) {
        return currentMethod == m;
    }

    // Enviar email
    private Mono<Void> sendEmail(String subject, String body) {

        String to = "usuario@teste.com"; // Ajustável futuramente

        Map<String, Object> emailPayload = Map.of(
                "to", to,
                "subject", subject,
                "body", body
        );

        return webClient.post()
                .uri(emailServiceUrl + "/send-email")
                .header("X-Gateway-Key", gatewaySecret)
                .bodyValue(emailPayload)
                .retrieve()
                .bodyToMono(String.class)
                .then()
                .onErrorResume(err -> {
                    System.err.println("Falha ao enviar email: " + err.getMessage());
                    return Mono.empty();
                });
    }
}
