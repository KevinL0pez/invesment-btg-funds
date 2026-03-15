package com.btg.funds.infrastructure.config.security;

import com.btg.funds.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * Implementación básica de {@link ReactiveAuthenticationManager} utilizada para
 * construir un objeto {@link Authentication} a partir de un token JWT recibido
 * en la solicitud.
 *
 * <p>Este componente forma parte del flujo de seguridad de Spring Security
 * en aplicaciones reactivas (Spring WebFlux). Su responsabilidad es transformar
 * el token recibido en el objeto {@link Authentication} que será almacenado
 * posteriormente en el {@link org.springframework.security.core.context.SecurityContext}.</p>
 *
 * <p>Funcionamiento:</p>
 * <ol>
 *     <li>Recibe un objeto {@link Authentication} que contiene el token JWT en
 *     las credenciales.</li>
 *     <li>Extrae el token desde {@code authentication.getCredentials()}.</li>
 *     <li>Crea un {@link UsernamePasswordAuthenticationToken} utilizando el
 *     token como principal y credencial.</li>
 *     <li>Retorna el objeto autenticado envuelto en un {@link Mono}.</li>
 * </ol>
 *
 * <p>Nota:</p>
 * <ul>
 *     <li>Esta implementación no valida la firma ni la expiración del token.</li>
 *     <li>Normalmente la validación del JWT se delega a un servicio adicional
 *     (por ejemplo {@code JwtService}).</li>
 *     <li>Se utiliza principalmente como paso intermedio dentro del flujo de
 *     autenticación reactivo.</li>
 * </ul>
 *
 * <p>Las peticiones autenticadas deben incluir el encabezado HTTP:</p>
 *
 * <pre>
 * Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwtService;

    /**
     * Construye un objeto {@link Authentication} a partir del token JWT
     * recibido en la solicitud.
     *
     * @param authentication objeto que contiene las credenciales
     *                       (token JWT) extraídas de la petición HTTP
     * @return {@link Mono} con el objeto {@link Authentication} generado
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = authentication.getCredentials().toString();

        if (!jwtService.validate(token)) {
            return Mono.empty();
        }

        String username = jwtService.getUsername(token);
        if (username == null || username.isBlank()) {
            return Mono.empty();
        }

        return Mono.just(new UsernamePasswordAuthenticationToken(
                username,
                token,
                Collections.emptyList()
        ));
    }
}
