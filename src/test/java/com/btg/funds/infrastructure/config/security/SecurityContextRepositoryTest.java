package com.btg.funds.infrastructure.config.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityContextRepositoryTest {

    @Mock
    private JwtAuthenticationManager jwtAuthenticationManager;

    @InjectMocks
    private SecurityContextRepository repository;

    @Test
    void shouldLoadSecurityContextWhenBearerTokenExists() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("kevin", "token");

        when(jwtAuthenticationManager.authenticate(any())).thenReturn(Mono.just(authentication));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/funds")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(repository.load(exchange))
                .assertNext(context -> org.junit.jupiter.api.Assertions.assertEquals("kevin", context.getAuthentication().getPrincipal()))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenAuthorizationHeaderMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/funds").build());

        StepVerifier.create(repository.load(exchange))
                .verifyComplete();
    }
}
