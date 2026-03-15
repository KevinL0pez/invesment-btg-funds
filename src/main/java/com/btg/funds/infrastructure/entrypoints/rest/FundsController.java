package com.btg.funds.infrastructure.entrypoints.rest;

import com.btg.funds.application.ports.out.FundRepositoryPort;
import com.btg.funds.domain.model.Fund;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Controlador REST encargado de exponer los endpoints relacionados con los fondos.
 *
 * <p>Este controlador permite consultar los fondos disponibles en el sistema
 * a través de una API reactiva basada en Spring WebFlux. La documentación
 * de los endpoints se genera automáticamente mediante OpenAPI / Swagger.</p>
 *
 * <p>Endpoint base:</p>
 * <pre>
 * /api/funds
 * </pre>
 */
@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
@Tag(name = "Funds", description = "Operaciones relacionadas con la gestión de fondos")
public class FundsController {

    /**
     * Puerto de acceso al repositorio de fondos definido en la capa de dominio.
     */
    private final FundRepositoryPort fundRepositoryPort;

    /**
     * Obtiene la lista completa de fondos disponibles.
     *
     * <p>Retorna un flujo reactivo con todos los fondos registrados
     * en el sistema.</p>
     *
     * @return flujo reactivo {@link Flux} que contiene los fondos disponibles.
     */
    @Operation(
            summary = "Consultar todos los fondos",
            description = "Obtiene la lista completa de fondos disponibles en el sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fondos obtenidos correctamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping
    public Flux<Fund> getAllFunds() {
        return fundRepositoryPort.findAll();
    }
}
