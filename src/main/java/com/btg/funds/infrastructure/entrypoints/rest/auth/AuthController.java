package com.btg.funds.infrastructure.entrypoints.rest.auth;

import com.btg.funds.domain.model.LoginRequest;
import com.btg.funds.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Controlador REST encargado de gestionar la autenticación de usuarios
 * mediante generación de tokens JWT.
 *
 * <p>Este controlador expone endpoints relacionados con autenticación bajo
 * la ruta base {@code /api/auth}. Actualmente implementa un endpoint de login
 * que valida credenciales configuradas en propiedades y retorna un token JWT
 * válido si la autenticación es exitosa.</p>
 *
 * <p>Las credenciales utilizadas para la validación son de tipo demo y se
 * obtienen desde las propiedades de configuración:</p>
 *
 * <ul>
 *     <li>{@code security.auth.demo-username}</li>
 *     <li>{@code security.auth.demo-password}</li>
 * </ul>
 *
 * <p>Si las credenciales son correctas, se genera un token utilizando
 * {@link JwtService} que luego podrá ser utilizado en las peticiones
 * protegidas mediante el header:</p>
 *
 * <pre>
 * Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 *
 * <p>Este controlador está diseñado para aplicaciones reactivas
 * basadas en Spring WebFlux, por lo que utiliza {@link Mono} como
 * tipo de retorno.</p>
 */
@Tag(
        name = "Autenticación",
        description = "Endpoints encargados de autenticar usuarios y generar tokens JWT."
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Servicio encargado de generar y validar tokens JWT. */
    private final JwtService jwtService;

    /** Username de demostración utilizado para validar autenticación. */
    private final String demoUsername;

    /** Password de demostración utilizado para validar autenticación. */
    private final String demoPassword;

    /**
     * Constructor que inicializa el controlador con las dependencias necesarias
     * y las credenciales de autenticación configuradas en propiedades.
     *
     * @param jwtService servicio encargado de generar tokens JWT
     * @param demoUsername username permitido para autenticación demo
     * @param demoPassword password permitido para autenticación demo
     */
    public AuthController(
            JwtService jwtService,
            @Value("${security.auth.demo-username}") String demoUsername,
            @Value("${security.auth.demo-password}") String demoPassword
    ) {
        this.jwtService = jwtService;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    @Operation(
            summary = "Autenticar usuario",
            description = """
                    Valida las credenciales del usuario y genera un token JWT si son correctas.

                    Las credenciales válidas se configuran mediante propiedades:

                    - security.auth.demo-username
                    - security.auth.demo-password

                    Si la autenticación es exitosa se retorna un token JWT que debe enviarse
                    en las siguientes peticiones usando el header:

                    Authorization: Bearer <token>
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa, retorna el token JWT"),
            @ApiResponse(responseCode = "400", description = "Request inválido o campos faltantes", content = @Content),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @Content)
    })
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
        if (secureEquals(demoUsername, username) && secureEquals(demoPassword, password)) {
            return Mono.just(Map.of("token", jwtService.generateToken(username)));
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }

    private boolean secureEquals(String expected, String provided) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }
}
