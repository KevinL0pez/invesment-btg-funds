package com.btg.funds.infrastructure.entrypoints.rest.auth;

import com.btg.funds.domain.model.LoginRequest;
import com.btg.funds.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtService jwtService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(jwtService, "admin", "funds2026");
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("funds2026");
        when(jwtService.generateToken("admin")).thenReturn("jwt-token");

        StepVerifier.create(controller.login(request))
                .assertNext(response -> assertEquals("jwt-token", response.get("token")))
                .verifyComplete();
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsNull() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.login(null)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("incorrect");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.login(request)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
