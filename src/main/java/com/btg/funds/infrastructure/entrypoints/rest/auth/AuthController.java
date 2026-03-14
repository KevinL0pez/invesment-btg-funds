package com.btg.funds.infrastructure.entrypoints.rest.auth;

import com.btg.funds.domain.model.LoginRequest;
import com.btg.funds.infrastructure.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final String demoUsername;
    private final String demoPassword;

    public AuthController(
            JwtService jwtService,
            @Value("${security.auth.demo-username}") String demoUsername,
            @Value("${security.auth.demo-password}") String demoPassword
    ) {
        this.jwtService = jwtService;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    @PostMapping("/login")
    public Mono<Map<String, String>> login(@RequestBody(required = false) LoginRequest credentials) {
        if (credentials == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body requerido");
        }

        String username = credentials.getUsername() == null ? null : credentials.getUsername().trim();
        String password = credentials.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "username y password son obligatorios"
            );
        }

        // Validación de credenciales demo configurable via propiedades.
        if (demoUsername.equals(username) && demoPassword.equals(password)) {
            return Mono.just(Map.of("token", jwtService.generateToken(username)));
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }
}
