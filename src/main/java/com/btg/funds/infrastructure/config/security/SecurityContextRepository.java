package com.btg.funds.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Implementación de {@link ServerSecurityContextRepository} encargada de
 * recuperar el {@link SecurityContext} a partir de un token JWT enviado
 * en el encabezado HTTP Authorization.
 *
 * <p>Este componente se utiliza en aplicaciones reactivas con
 * Spring WebFlux y Spring Security para resolver la autenticación
 * en cada petición entrante.</p>
 *
 * <p>Funcionamiento:</p>
 * <ul>
 *     <li>Lee el header {@code Authorization} de la solicitud HTTP.</li>
 *     <li>Verifica que el header tenga el esquema {@code Bearer}.</li>
 *     <li>Extrae el token JWT.</li>
 *     <li>Delegar la validación del token al {@link JwtAuthenticationManager}.</li>
 *     <li>Si el token es válido, construye un {@link SecurityContext}
 *     con la autenticación resultante.</li>
 * </ul>
 *
 * <p>Este repositorio no persiste el contexto de seguridad entre
 * solicitudes, ya que en arquitecturas basadas en JWT la autenticación
 * es <b>stateless</b>. Cada petición debe incluir su propio token.</p>
 */
@Component
@RequiredArgsConstructor
public class SecurityContextRepository implements ServerSecurityContextRepository {

    /**
     * Administrador de autenticación encargado de validar el token JWT
     * y construir el objeto correspondiente.
     */
    private final JwtAuthenticationManager authManager;

    /**
     * No persiste el {@link SecurityContext}, ya que el sistema utiliza
     * autenticación basada en tokens (stateless).
     *
     * @param exchange intercambio HTTP actual
     * @param context contexto de seguridad generado
     * @return {@link Mono#empty()} indicando que no se guarda estado
     */
    @Override
    public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
        return Mono.empty();
    }

    /**
     * Carga el {@link SecurityContext} a partir del token JWT presente
     * en el encabezado Authorization de la solicitud.
     *
     * <p>Si el header Authorization contiene un token con el formato:</p>
     *
     * <pre>
     * Authorization: Bearer &lt;jwt-token&gt;
     * </pre>
     *
     * <p>El token es extraído y enviado al {@link JwtAuthenticationManager}
     * para su validación. Si la autenticación es exitosa, se crea un
     * {@link SecurityContextImpl} con la información autenticada.</p>
     *
     * @param exchange intercambio HTTP actual
     * @return un {@link Mono} que contiene el {@link SecurityContext}
     *         autenticado, o {@link Mono#empty()} si no hay token
     */
    @Override
    public Mono<SecurityContext> load(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return authManager.authenticate(new UsernamePasswordAuthenticationToken(token, token))
                    .map(SecurityContextImpl::new);
        }
        return Mono.empty();
    }
}
