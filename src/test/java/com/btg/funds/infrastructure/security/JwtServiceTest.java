package com.btg.funds.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "this_is_a_secure_secret_with_32_bytes_min";

    @Test
    void shouldGenerateAndValidateToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);

        String token = jwtService.generateToken("admin");

        assertNotNull(token);
        assertTrue(jwtService.validate(token));
        assertEquals("admin", jwtService.getUsername(token));
    }

    @Test
    void shouldRejectShortSecret() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtService("short_secret", 60_000)
        );

        assertTrue(exception.getMessage().contains("al menos 32 bytes"));
    }

    @Test
    void shouldInvalidateCorruptedToken() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken("admin");
        String corrupted = token.substring(0, token.length() - 2) + "xx";

        assertFalse(jwtService.validate(corrupted));
    }
}
