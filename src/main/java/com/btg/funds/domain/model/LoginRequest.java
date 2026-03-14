package com.btg.funds.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO utilizado para recibir las credenciales de autenticación
 * enviadas por el cliente en una solicitud de login.
 */
@Getter
@Setter
@Schema(description = "Credenciales de autenticación del usuario")
public class LoginRequest {

    @Schema(description = "Nombre de usuario", example = "admin")
    private String username;

    @Schema(description = "Contraseña del usuario", example = "123456")
    private String password;
}
