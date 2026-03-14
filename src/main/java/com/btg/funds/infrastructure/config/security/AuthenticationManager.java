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
 * Implementación de {@link ReactiveAuthenticationManager} encargada de autenticar
 * solicitudes basadas en tokens JWT dentro de una aplicación reactiva con Spring WebFlux.
 *
 * <p>Este componente forma parte del flujo de seguridad de Spring Security y es
 * responsable de validar el token recibido en cada petición protegida.</p>
 *
 * <p>Proceso de autenticación:</p>
 * <ol>
 *     <li>Recibe el objeto {@link Authentication} que contiene el token JWT en
 *     sus credenciales.</li>
 *     <li>Extrae el token y obtiene el username utilizando {@link JwtService}.</li>
 *     <li>Valida la firma y la expiración del token.</li>
 *     <li>Si el token es válido, crea un {@link UsernamePasswordAuthenticationToken}
 *     autenticado con el username extraído.</li>
 *     <li>Si el token es inválido o ocurre una excepción, se retorna {@link Mono#empty()},
 *     indicando que la autenticación falló.</li>
 * </ol>
 *
 * <p>Este administrador se utiliza típicamente junto con un
 * {@link org.springframework.security.web.server.context.ServerSecurityContextRepository}
 * que extrae el token del header {@code Authorization} de las peticiones HTTP.</p>
 *
 * <p>El sistema utiliza autenticación <b>stateless</b>, por lo que cada petición
 * debe incluir un token válido en el header:</p>
 *
 * <pre>
 * Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {

    /** Servicio encargado de generar, extraer y validar tokens JWT. */
    private final JwtService jwtService;

    /**
     * Intenta autenticar una solicitud utilizando un token JWT.
     *
     * <p>El token se obtiene desde las credenciales del objeto
     * {@link Authentication}. Si el token es válido, se crea un
     * {@link UsernamePasswordAuthenticationToken} autenticado con el
     * username contenido en el token.</p>
     *
     * @param authentication objeto que contiene las credenciales de autenticación
     *                       (token JWT)
     * @return {@link Mono} que contiene el objeto {@link Authentication}
     * autenticado si el token es válido, o {@link Mono#empty()} si la
     * autenticación falla
     */
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
