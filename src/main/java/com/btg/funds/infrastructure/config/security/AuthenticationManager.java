package com.btg.funds.infrastructure.config.security;

import com.btg.funds.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        try {
            String username = jwtService.getUsername(token);
            if (username != null && jwtService.validate(token)) {
                return Mono.just(new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.emptyList()
                ));
            }
        } catch (Exception e) {
            return Mono.empty();
        }

        return Mono.empty();
    }
}
