package com.btg.funds.infrastructure.config;

import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.application.ports.out.FundRepository;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer {

    private static final String DEFAULT_FUNDS_TABLE = "funds";
    private static final String DEFAULT_CUSTOMERS_TABLE = "customers";
    private static final String DEFAULT_TRANSACTIONS_TABLE = "transactions";
    private static final String INITIAL_CUSTOMER_ID = "kevinavs.2001@gmail.com";

    private final FundRepository fundRepository;
    private final CustomerRepository customerRepository;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final AppProperties appProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        log.info("🚀 Iniciando carga de datos sincronizada...");

        Mono.fromFuture(this::createTableFunds)
                .then(Mono.fromFuture(this::createTableCustomers))
                .then(Mono.fromFuture(this::createTableTransactions))
                .thenMany(Flux.fromIterable(getInitialFunds())
                        .concatMap(fund -> fundRepository.save(fund)
                                .doOnSuccess(f -> log.info("📌 Fondo guardado: {}", f.getName()))))
                .then(customerRepository.findById(INITIAL_CUSTOMER_ID)
                        .switchIfEmpty(Mono.defer(() -> {
                            log.info("👤 Creando cliente inicial...");
                            return customerRepository.save(createInitialCustomer());
                        })))
                .doOnSuccess(v -> log.info("✅ ¡Todo cargado con éxito!"))
                .doOnError(e -> log.error("❌ Error en la inicialización", e))
                .subscribe();
    }

    private CompletableFuture<Void> createTableFunds() {
        return createGenericTable(resolveTableName(appProperties.getDynamodbAws().getFundsTable(), DEFAULT_FUNDS_TABLE), "id");
    }

    private CompletableFuture<Void> createTableCustomers() {
        return createGenericTable(resolveTableName(appProperties.getDynamodbAws().getCustomersTable(), DEFAULT_CUSTOMERS_TABLE), "id");
    }

    private CompletableFuture<Void> createTableTransactions() {
        String tableName = resolveTableName(
                appProperties.getDynamodbAws().getTransactionsTable(),
                DEFAULT_TRANSACTIONS_TABLE
        );
        return createTableIfNotExists(
                tableName,
                CreateTableRequest.builder()
                        .tableName(tableName)
                        .attributeDefinitions(
                                AttributeDefinition.builder().attributeName("customerId").attributeType(ScalarAttributeType.S).build(),
                                AttributeDefinition.builder().attributeName("timestamp").attributeType(ScalarAttributeType.N).build()
                        )
                        .keySchema(
                                KeySchemaElement.builder().attributeName("customerId").keyType(KeyType.HASH).build(),
                                KeySchemaElement.builder().attributeName("timestamp").keyType(KeyType.RANGE).build()
                        )
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build()
        );
    }

    private CompletableFuture<Void> createGenericTable(String tableName, String partitionKey) {
        return createTableIfNotExists(
                tableName,
                CreateTableRequest.builder()
                        .tableName(tableName)
                        .attributeDefinitions(
                                AttributeDefinition.builder()
                                        .attributeName(partitionKey)
                                        .attributeType(ScalarAttributeType.S)
                                        .build()
                        )
                        .keySchema(
                                KeySchemaElement.builder()
                                        .attributeName(partitionKey)
                                        .keyType(KeyType.HASH)
                                        .build()
                        )
                        .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build()
        );
    }

    private CompletableFuture<Void> createTableIfNotExists(String tableName, CreateTableRequest createRequest) {
        return dynamoDbAsyncClient.describeTable(r -> r.tableName(tableName))
                .thenCompose(resp -> waitForTableActive(tableName)
                        .thenRun(() -> log.info("Tabla {} ya existe y está activa.", tableName)))
                .handle((ok, ex) -> {
                    if (ex == null) {
                        return CompletableFuture.<Void>completedFuture(null);
                    }
                    Throwable cause = unwrapCompletionException(ex);
                    if (cause instanceof ResourceNotFoundException) {
                        log.info("Tabla {} no existe. Creándola...", tableName);
                        return dynamoDbAsyncClient.createTable(createRequest)
                                .thenCompose(created -> waitForTableActive(tableName))
                                .thenRun(() -> log.info("Tabla {} creada y activa.", tableName));
                    }
                    return CompletableFuture.<Void>failedFuture(cause);
                })
                .thenCompose(future -> future);
    }

    private CompletableFuture<Void> waitForTableActive(String tableName) {
        DynamoDbAsyncWaiter waiter = dynamoDbAsyncClient.waiter();
        return waiter.waitUntilTableExists(r -> r.tableName(tableName))
                .thenAccept(response -> log.debug("Espera finalizada para la tabla {}", tableName));
    }

    private Throwable unwrapCompletionException(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    private String resolveTableName(String configuredName, String defaultName) {
        if (configuredName == null || configuredName.isBlank()) {
            return defaultName;
        }
        return configuredName;
    }

    private List<Fund> getInitialFunds() {
        return List.of(
                new Fund("1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV"),
                new Fund("2", "FPV_BTG_PACTUAL_ECOPETROL", new BigDecimal("125000"), "FPV"),
                new Fund("3", "DEUDAPRIVADA", new BigDecimal("50000"), "FIC"),
                new Fund("4", "FDO-ACCIONES", new BigDecimal("250000"), "FIC"),
                new Fund("5", "FPV_BTG_PACTUAL_DINAMICA", new BigDecimal("100000"), "FPV")
        );
    }

    private Customer createInitialCustomer() {
        return Customer.builder()
                .id(INITIAL_CUSTOMER_ID)
                .name("Kevin Lopez")
                .email(INITIAL_CUSTOMER_ID)
                .phoneNumber("+573217517125")
                .balance(new BigDecimal("500000"))
                .activeSubscriptions(new HashSet<>())
                .build();
    }
}
