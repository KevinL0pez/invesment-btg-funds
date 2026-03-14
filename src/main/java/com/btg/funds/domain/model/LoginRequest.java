package com.btg.funds.domain.model;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO utilizado para recibir las credenciales de autenticación
 * enviadas por el cliente en una solicitud de login.
 */
@Getter
@Setter
public class LoginRequest {
    private String username;
    private String password;
}
