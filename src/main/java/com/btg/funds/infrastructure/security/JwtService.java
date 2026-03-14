package com.btg.funds.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Servicio encargado de la generación, extracción y validación de tokens JWT.
 *
 * Utiliza la librería JJWT y el algoritmo de firma HS256 para garantizar la
 * integridad del token mediante una clave secreta.
 *
 * Responsabilidades principales:
 * - Generar tokens JWT para usuarios autenticados.
 * - Extraer información (username) desde un token.
 * - Validar la integridad y expiración del token.
 */
@Service
public class JwtService {

    /** Clave criptográfica utilizada para firmar y verificar los tokens JWT. */
    private final SecretKey key;

    /** Tiempo de expiración del token en milisegundos. */
    private final long expirationMs;

    /**
     * Constructor que inicializa el servicio JWT a partir de propiedades externas.
     *
     * @param secret clave secreta usada para firmar los tokens JWT.
     *               Debe tener al menos 32 bytes para garantizar la seguridad
     *               requerida por el algoritmo HS256.
     * @param expirationMs tiempo de expiración del token en milisegundos.
     *
     * @throws IllegalStateException si la clave secreta tiene menos de 32 bytes.
     */
    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long expirationMs
    ) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret debe tener al menos 32 bytes para HS256"
            );
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT para un usuario autenticado.
     *
     * @param username nombre del usuario que se almacenará como subject en el token
     * @return token JWT firmado
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)       // usuario propietario del token
                .issuedAt(new Date(System.currentTimeMillis())) // fecha de emisión
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // expiración
                .signWith(key) // firma del token (algoritmo inferido desde la clave)
                .compact();
    }

    /**
     * Extrae el username almacenado en el token JWT.
     *
     * <p>El método primero valida la firma del token utilizando la clave configurada
     * y luego obtiene el campo {@code subject} del payload.</p>
     *
     * @param token token JWT firmado
     * @return username contenido en el subject del token
     */
    public String getUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Valida la integridad y vigencia del token.
     *
     * <p>Este método realiza las siguientes verificaciones:</p>
     * <ul>
     *     <li>Que el token tenga una firma válida.</li>
     *     <li>Que el token no haya expirado.</li>
     * </ul>
     *
     * <p>Si ocurre cualquier excepción durante el proceso de parsing o validación
     * (token inválido, manipulado o expirado), el método retorna {@code false}.</p>
     *
     * @param token token JWT a validar
     * @return {@code true} si el token es válido y no ha expirado,
     *         {@code false} en caso contrario
     */
    public boolean validate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
