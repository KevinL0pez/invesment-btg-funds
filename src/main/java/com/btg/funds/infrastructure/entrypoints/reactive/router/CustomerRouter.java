package com.btg.funds.infrastructure.entrypoints.reactive.router;

import com.btg.funds.infrastructure.entrypoints.reactive.handler.CustomerHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Configuración de rutas funcionales para la gestión de clientes y fondos.
 *
 * <p>Esta clase define los endpoints disponibles para suscribir, cancelar
 * suscripciones y consultar los fondos asociados a un cliente.</p>
 */
@Configuration
@Tag(name = "Customers", description = "Operaciones relacionadas con clientes y fondos")
public class CustomerRouter {
    /**
     * Define las rutas HTTP expuestas por el servicio.
     *
     * @param handler manejador responsable de procesar las solicitudes.
     * @return router con los endpoints configurados.
     */
    @Bean
    @RouterOperations({

            @RouterOperation(
                    path = "/api/customers/{customerId}/subscribe",
                    method = RequestMethod.POST,
                    beanClass = CustomerHandler.class,
                    beanMethod = "subscribe",
                    operation = @Operation(
                            operationId = "subscribeCustomer",
                            summary = "Suscribir cliente a un fondo",
                            description = "Permite registrar la suscripción de un cliente a un fondo de inversión",
                            parameters = {
                                    @Parameter(
                                            name = "customerId",
                                            description = "Identificador del cliente",
                                            required = true,
                                            in = ParameterIn.PATH
                                    )
                            }
                    )
            ),

            @RouterOperation(
                    path = "/api/customers/{customerId}/unsubscribe",
                    method = RequestMethod.POST,
                    beanClass = CustomerHandler.class,
                    beanMethod = "unsubscribe",
                    operation = @Operation(
                            operationId = "unsubscribeCustomer",
                            summary = "Cancelar suscripción a fondo",
                            description = "Permite cancelar la suscripción de un cliente a un fondo",
                            parameters = {
                                    @Parameter(
                                            name = "customerId",
                                            description = "Identificador del cliente",
                                            required = true,
                                            in = ParameterIn.PATH
                                    )
                            }
                    )
            ),

            @RouterOperation(
                    path = "/api/customers/{customerId}/customer-funds",
                    method = RequestMethod.GET,
                    beanClass = CustomerHandler.class,
                    beanMethod = "getCustomerFunds",
                    operation = @Operation(
                            operationId = "getCustomerFunds",
                            summary = "Consultar fondos del cliente",
                            description = "Obtiene la lista de fondos a los que está suscrito un cliente",
                            parameters = {
                                    @Parameter(
                                            name = "customerId",
                                            description = "Identificador del cliente",
                                            required = true,
                                            in = ParameterIn.PATH
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> fundRoutes(CustomerHandler handler) {
        return route(POST("/api/customers/{customerId}/subscribe"), handler::subscribe)
                .andRoute(POST("/api/customers/{customerId}/unsubscribe"), handler::unsubscribe)
                .andRoute(GET("/api/customers/{customerId}/customer-funds"), handler::getCustomerFunds);
    }
}
