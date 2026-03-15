package com.btg.funds.infrastructure.entrypoints.reactive.handler;

import com.btg.funds.application.ports.in.GetCustomerFundsUseCase;
import com.btg.funds.application.ports.in.SubscribeFundUseCase;
import com.btg.funds.application.ports.in.UnsubscribeFundUseCase;
import com.btg.funds.domain.exception.CustomerNotFoundException;
import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.exception.SubscriptionNotFoundException;
import com.btg.funds.domain.model.SubscriptionRequest;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.infrastructure.entrypoints.reactive.router.CustomerRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerHandlerTest {

    @Mock
    private SubscribeFundUseCase subscribeFundUseCase;
    @Mock
    private UnsubscribeFundUseCase unsubscribeFundUseCase;
    @Mock
    private GetCustomerFundsUseCase getCustomerFundsUseCase;

    @InjectMocks
    private CustomerHandler handler;

    @Mock
    private ServerRequest request;

    @Test
    void shouldReturnBadRequestWhenCustomerIdIsBlankOnSubscribe() {
        when(request.pathVariable("customerId")).thenReturn(" ");

        StepVerifier.create(handler.subscribe(request).map(ServerResponse::statusCode))
                .assertNext(status -> assertEquals(HttpStatus.BAD_REQUEST, status))
                .verifyComplete();
    }

    @Test
    void shouldMapInsufficientBalanceToBadRequest() {
        SubscriptionRequest body = new SubscriptionRequest(null, "f1", "EMAIL", BigDecimal.TEN);

        when(request.pathVariable("customerId")).thenReturn("c1");
        when(request.bodyToMono(eq(SubscriptionRequest.class))).thenReturn(Mono.just(body));
        when(subscribeFundUseCase.execute("c1", "f1", "EMAIL"))
                .thenReturn(Mono.error(new InsufficientBalanceException("Fund X")));

        StepVerifier.create(handler.subscribe(request).map(ServerResponse::statusCode))
                .assertNext(status -> assertEquals(HttpStatus.BAD_REQUEST, status))
                .verifyComplete();
    }

    @Test
    void shouldReturnOkForGetCustomerFunds() {
        Transaction tx = Transaction.builder().id("t1").timestamp(1L).build();
        when(request.pathVariable("customerId")).thenReturn("c1");
        when(getCustomerFundsUseCase.execute("c1")).thenReturn(Flux.just(tx));

        StepVerifier.create(handler.getCustomerFunds(request).map(ServerResponse::statusCode))
                .assertNext(status -> assertEquals(HttpStatus.OK, status))
                .verifyComplete();
    }

    @Test
    void shouldReturnBadRequestWhenSubscribeBodyMissingRequiredFields() {
        SubscriptionRequest body = new SubscriptionRequest(null, " ", " ", BigDecimal.TEN);

        when(request.pathVariable("customerId")).thenReturn("c1");
        when(request.bodyToMono(eq(SubscriptionRequest.class))).thenReturn(Mono.just(body));

        StepVerifier.create(handler.subscribe(request).map(ServerResponse::statusCode))
                .assertNext(status -> assertEquals(HttpStatus.BAD_REQUEST, status))
                .verifyComplete();
    }

    @Test
    void shouldReturnNotFoundWhenSubscribeUseCaseReturnsEmpty() {
        SubscriptionRequest body = new SubscriptionRequest(null, "f1", "EMAIL", BigDecimal.TEN);

        when(request.pathVariable("customerId")).thenReturn("c1");
        when(request.bodyToMono(eq(SubscriptionRequest.class))).thenReturn(Mono.just(body));
        when(subscribeFundUseCase.execute("c1", "f1", "EMAIL")).thenReturn(Mono.empty());

        StepVerifier.create(handler.subscribe(request).map(ServerResponse::statusCode))
                .assertNext(status -> assertEquals(HttpStatus.NOT_FOUND, status))
                .verifyComplete();
    }

    @Test
    void shouldReturnNotFoundWhenSubscribeCustomerDoesNotExist() {
        SubscriptionRequest body = new SubscriptionRequest(null, "f1", "EMAIL", BigDecimal.TEN);

        when(request.pathVariable("customerId")).thenReturn("c1");
        when(request.bodyToMono(eq(SubscriptionRequest.class))).thenReturn(Mono.just(body));
        when(subscribeFundUseCase.execute("c1", "f1", "EMAIL"))
                .thenReturn(Mono.error(new CustomerNotFoundException("c1")));

        StepVerifier.create(handler.subscribe(request).map(ServerResponse::statusCode))
                .assertNext(status -> assertEquals(HttpStatus.NOT_FOUND, status))
                .verifyComplete();
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedError() {
        SubscriptionRequest body = new SubscriptionRequest(null, "f1", "EMAIL", BigDecimal.TEN);

        when(request.pathVariable("customerId")).thenReturn("c1");
        when(request.bodyToMono(eq(SubscriptionRequest.class))).thenReturn(Mono.just(body));
        when(subscribeFundUseCase.execute("c1", "f1", "EMAIL"))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(handler.subscribe(request).map(ServerResponse::statusCode))
                .assertNext(status -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status))
                .verifyComplete();
    }

    @Test
    void shouldReturnBadRequestWhenUnsubscribeFundIdMissing() {
        WebTestClient client = routerClient();

        client.post()
                .uri("/api/customers/c1/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnBadRequestWhenUnsubscribeSubscriptionDoesNotExist() {
        WebTestClient client = routerClient();
        when(unsubscribeFundUseCase.execute("c1", "f1"))
                .thenReturn(Mono.error(new SubscriptionNotFoundException("Fund X")));

        client.post()
                .uri("/api/customers/c1/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"fundId\":\"f1\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturnNotFoundWhenUnsubscribeUseCaseReturnsEmpty() {
        WebTestClient client = routerClient();
        when(unsubscribeFundUseCase.execute("c1", "f1")).thenReturn(Mono.empty());

        client.post()
                .uri("/api/customers/c1/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"fundId\":\"f1\"}")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturnOkWhenUnsubscribeSucceeds() {
        WebTestClient client = routerClient();
        Transaction tx = Transaction.builder().id("tx1").fundId("f1").timestamp(1L).build();
        when(unsubscribeFundUseCase.execute("c1", "f1")).thenReturn(Mono.just(tx));

        client.post()
                .uri("/api/customers/c1/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"fundId\":\"f1\"}")
                .exchange()
                .expectStatus().isOk();
    }

    private WebTestClient routerClient() {
        CustomerHandler localHandler = new CustomerHandler(
                subscribeFundUseCase,
                unsubscribeFundUseCase,
                getCustomerFundsUseCase
        );
        RouterFunction<ServerResponse> routes = new CustomerRouter().fundRoutes(localHandler);
        return WebTestClient.bindToRouterFunction(routes).build();
    }
}
