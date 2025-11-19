package com.eventos.eventsgateway.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

        return spec.exchangeToMono(clientResponse -> {

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

    private boolean methodIs(HttpMethod method) {
        return this.currentMethod == method;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Mono<Void> executePostActionEmail(String path, String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);

            JsonNode data = json.path("data");
            JsonNode userNode = null;
            Integer eventId = null;

            if (data.has("user")) {
                userNode = data.path("user");
                eventId = data.path("id_evento").asInt();
            }

            if (data.has("subscription")) {
                JsonNode sub = data.path("subscription");
                userNode = sub.path("user");
                eventId = sub.path("id_evento").asInt();
            }

            if (userNode == null || userNode.isMissingNode()) {
                return Mono.empty();
            }

            String email = userNode.path("email").asText(null);
            String userName = userNode.path("nome").asText(null);

            if (email == null || userName == null) {
                return Mono.empty();
            }

            // POST /inscricoes
            if (path.equals("inscricoes") && methodIs(HttpMethod.POST)) {

                if (eventId == null) {
                    return Mono.empty();
                }

                return webClient.get()
                        .uri(eventServiceUrl + "/eventos/" + eventId)
                        .header("X-Gateway-Key", gatewaySecret)
                        .retrieve()
                        .bodyToMono(String.class)
                        .flatMap(eventResponse -> {
                            try {
                                JsonNode eventJson = objectMapper.readTree(eventResponse);
                                String eventName = eventJson.path("data").path("nome").asText("");

                                String subject = "Inscrição realizada";
                                String body = "Olá " + userName + ",\n\n"
                                        + "Sua inscrição no evento '" + eventName + "' foi registrada com sucesso.";

                                return sendEmail(email, subject, body);
                            } catch (Exception e) {
                                return Mono.empty();
                            }
                        });
            }

            // PUT /inscricoes/{id} (cancelamento)
            if (path.matches("inscricoes/[0-9]+") && methodIs(HttpMethod.PUT)) {

                if (eventId == null) {
                    return Mono.empty();
                }

                return webClient.get()
                        .uri(eventServiceUrl + "/eventos/" + eventId)
                        .header("X-Gateway-Key", gatewaySecret)
                        .retrieve()
                        .bodyToMono(String.class)
                        .flatMap(eventResponse -> {
                            try {
                                JsonNode eventJson = objectMapper.readTree(eventResponse);
                                String eventName = eventJson.path("data").path("nome").asText("");

                                String subject = "Inscrição cancelada";
                                String body = "Olá " + userName + ",\n\n"
                                        + "Sua inscrição no evento '" + eventName + "' foi cancelada.";

                                return sendEmail(email, subject, body);
                            } catch (Exception e) {
                                return Mono.empty();
                            }
                        });
            }

            // POST /inscricoes/{id}/checkin
            if (path.matches("inscricoes/[0-9]+/checkin") && methodIs(HttpMethod.POST)) {

                if (eventId == null) {
                    return Mono.empty();
                }

                return webClient.get()
                        .uri(eventServiceUrl + "/eventos/" + eventId)
                        .header("X-Gateway-Key", gatewaySecret)
                        .retrieve()
                        .bodyToMono(String.class)
                        .flatMap(eventResponse -> {
                            try {
                                JsonNode eventJson = objectMapper.readTree(eventResponse);
                                String eventName = eventJson.path("data").path("nome").asText("");

                                String subject = "Check-in realizado";
                                String body = "Olá " + userName + ",\n\n"
                                        + "Seu check-in no evento '" + eventName + "' foi confirmado.";

                                return sendEmail(email, subject, body);
                            } catch (Exception e) {
                                return Mono.empty();
                            }
                        });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Mono.empty();
    }

    private Mono<Void> sendEmail(String to, String subject, String body) {
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
