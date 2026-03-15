package com.btg.funds.infrastructure.config.security;

import com.btg.funds.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationManagerTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationManager jwtAuthenticationManager;

    @Test
    void shouldAuthenticateValidToken() {
        when(jwtService.validate("valid-token")).thenReturn(true);
        when(jwtService.getUsername("valid-token")).thenReturn("kevin");

        StepVerifier.create(
                        jwtAuthenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken("valid-token", "valid-token")
                        )
                )
                .assertNext(auth -> {
                    org.junit.jupiter.api.Assertions.assertEquals("kevin", auth.getPrincipal());
                    org.junit.jupiter.api.Assertions.assertTrue(auth.isAuthenticated());
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenTokenIsInvalid() {
        when(jwtService.validate("invalid-token")).thenReturn(false);

        StepVerifier.create(
                        jwtAuthenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken("invalid-token", "invalid-token")
                        )
                )
                .verifyComplete();
    }
}
