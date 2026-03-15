package com.btg.funds.infrastructure.entrypoints.reactive.handler;

import com.btg.funds.application.ports.in.GetCustomerFundsUseCase;
import com.btg.funds.application.ports.in.SubscribeFundUseCase;
import com.btg.funds.application.ports.in.UnsubscribeFundUseCase;
import com.btg.funds.domain.exception.CustomerNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.exception.SubscriptionNotFoundException;
import com.btg.funds.domain.model.SubscriptionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomerHandler {
    private final SubscribeFundUseCase subscribeFundUseCase;
    private final UnsubscribeFundUseCase unsubscribeFundUseCase;
    private final GetCustomerFundsUseCase getCustomerFundsUseCase;

    public Mono<ServerResponse> subscribe(ServerRequest request) {
        String customerId = normalize(request.pathVariable("customerId"));
        if (isBlank(customerId)) {
            return error(HttpStatus.BAD_REQUEST, "customerId es obligatorio");
        }

        return request.bodyToMono(SubscriptionRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Body requerido")))
                .flatMap(r -> {
                    String fundId = normalize(r.fundId());
                    String notificationType = normalize(r.notificationType());

                    if (isBlank(fundId) || isBlank(notificationType)) {
                        return Mono.error(new IllegalArgumentException(
                                "fundId y notificationType son obligatorios"
                        ));
                    }

                    return subscribeFundUseCase.execute(customerId, fundId, notificationType);
                })
                .flatMap(transaction -> ServerResponse.ok().bodyValue(transaction))
                .switchIfEmpty(error(HttpStatus.NOT_FOUND, "No se pudo procesar la suscripción"))
                .onErrorResume(this::toErrorResponse);
    }

    public Mono<ServerResponse> unsubscribe(ServerRequest request) {
        String customerId = normalize(request.pathVariable("customerId"));
        if (isBlank(customerId)) {
            return error(HttpStatus.BAD_REQUEST, "customerId es obligatorio");
        }

        return request.bodyToMono(CancelSubscriptionRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Body requerido")))
                .flatMap(cancelRequest -> {
                    String fundId = normalize(cancelRequest.fundId());
                    if (isBlank(fundId)) {
                        return Mono.error(new IllegalArgumentException("fundId es obligatorio"));
                    }

                    return unsubscribeFundUseCase.execute(customerId, fundId)
                            .flatMap(t -> ServerResponse.ok().bodyValue(t));
                })
                .switchIfEmpty(error(HttpStatus.NOT_FOUND, "Suscripcion no encontrada para cancelar"))
                .onErrorResume(this::toErrorResponse);
    }

    public Mono<ServerResponse> getCustomerFunds(ServerRequest request) {
        String customerId = normalize(request.pathVariable("customerId"));
        if (isBlank(customerId)) {
            return error(HttpStatus.BAD_REQUEST, "customerId es obligatorio");
        }

        return getCustomerFundsUseCase.execute(customerId)
                .collectList()
                .flatMap(list -> ServerResponse.ok().bodyValue(list))
                .onErrorResume(this::toErrorResponse);
    }

    private Mono<ServerResponse> toErrorResponse(Throwable error) {
        String message = error.getMessage() == null ? "Error inesperado" : error.getMessage();
        String normalizedMessage = message.toLowerCase();

        if (error instanceof IllegalArgumentException) {
            log.warn("Solicitud invalida en CustomerHandler: {}", message);
            return error(HttpStatus.BAD_REQUEST, message);
        }

        if (error instanceof InsufficientBalanceException) {
            log.warn("Saldo insuficiente en CustomerHandler: {}", message);
            return error(HttpStatus.BAD_REQUEST, message);
        }

        if (error instanceof SubscriptionNotFoundException) {
            log.warn("Suscripcion no encontrada en CustomerHandler: {}", message);
            return error(HttpStatus.BAD_REQUEST, message);
        }

        if (error instanceof CustomerNotFoundException || error instanceof FundNotFoundException) {
            log.warn("Recurso no encontrado en CustomerHandler: {}", message);
            return error(HttpStatus.NOT_FOUND, message);
        }

        if (normalizedMessage.contains("no encontrado") || normalizedMessage.contains("no existe")) {
            log.warn("Error de negocio no encontrado en CustomerHandler: {}", message);
            return error(HttpStatus.NOT_FOUND, message);
        }

        if (normalizedMessage.contains("insuficiente")
                || normalizedMessage.contains("no esta vinculado")
                || normalizedMessage.contains("saldo disponible")) {
            log.warn("Error de negocio en CustomerHandler: {}", message);
            return error(HttpStatus.BAD_REQUEST, message);
        }

        log.error("Error inesperado en CustomerHandler", error);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno procesando la solicitud");
    }

    private Mono<ServerResponse> error(HttpStatus status, String message) {
        return ServerResponse.status(status)
                .bodyValue(Map.of("message", message, "status", status.value()));
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record CancelSubscriptionRequest(String fundId) {}
}
