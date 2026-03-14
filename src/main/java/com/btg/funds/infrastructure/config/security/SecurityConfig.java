package com.btg.funds.infrastructure.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;

/**
 * Configuración de seguridad para la aplicación basada en Spring WebFlux.
 *
 * <p>Esta clase define la configuración principal de Spring Security en un
 * entorno reactivo, estableciendo el manejo de autenticación mediante JWT
 * y las reglas de autorización para los diferentes endpoints expuestos
 * por la aplicación.</p>
 *
 * <p>Características principales:</p>
 * <ul>
 *     <li>Deshabilita mecanismos de autenticación tradicionales como
 *     CSRF, formLogin y HTTP Basic.</li>
 *     <li>Utiliza un {@link JwtAuthenticationManager} para validar
 *     los tokens JWT recibidos en las peticiones.</li>
 *     <li>Emplea un {@link ServerSecurityContextRepository} para
 *     cargar el contexto de seguridad a partir del token JWT.</li>
 *     <li>Permite acceso público a endpoints de autenticación y documentación.</li>
 *     <li>Protege los endpoints de negocio requiriendo autenticación.</li>
 * </ul>
 *
 * <p>Autenticación:</p>
 * <ul>
 *     <li>Las peticiones autenticadas deben incluir el header:</li>
 * </ul>
 *
 * <pre>
 * Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 *
 * <p>Endpoints públicos:</p>
 * <ul>
 *     <li>/api/auth/** → endpoints de autenticación</li>
 *     <li>/v3/api-docs/** → documentación OpenAPI</li>
 *     <li>/swagger-ui/** → interfaz Swagger</li>
 * </ul>
 *
 * <p>Endpoints protegidos:</p>
 * <ul>
 *     <li>/api/funds/**</li>
 *     <li>/api/customers/**</li>
 * </ul>
 *
 * <p>Cualquier otro endpoint no especificado también requiere autenticación.</p>
 */
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Administrador encargado de autenticar tokens JWT. */
    private final JwtAuthenticationManager jwtAuthenticationManager;

    /** Repositorio encargado de cargar el SecurityContext a partir del JWT. */
    private final ServerSecurityContextRepository securityContextRepository;

    /**
     * Define la cadena de filtros de seguridad utilizada por Spring WebFlux.
     *
     * <p>Configura:</p>
     * <ul>
     *     <li>Desactivación de CSRF.</li>
     *     <li>Desactivación de autenticación basada en formulario.</li>
     *     <li>Desactivación de HTTP Basic.</li>
     *     <li>Uso de autenticación JWT.</li>
     *     <li>Reglas de autorización para los endpoints.</li>
     * </ul>
     *
     * @param http objeto {@link ServerHttpSecurity} utilizado para construir la configuración
     * @return cadena de filtros de seguridad configurada
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authenticationManager(jwtAuthenticationManager)
                .securityContextRepository(securityContextRepository)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/webjars/**"
                        ).permitAll()
                        .pathMatchers("/api/funds/**", "/api/customers/**").authenticated()
                        .anyExchange().authenticated()
                )
                .build();
    }
}
