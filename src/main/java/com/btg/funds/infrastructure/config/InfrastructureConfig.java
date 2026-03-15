package com.btg.funds.infrastructure.config;

import com.btg.funds.infrastructure.adapters.dynamodb.entities.CustomerEntity;
import com.btg.funds.infrastructure.adapters.dynamodb.entities.FundEntity;
import com.btg.funds.infrastructure.adapters.dynamodb.entities.TransactionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.ses.SesAsyncClient;

import java.net.URI;

@Configuration
@Slf4j
public class InfrastructureConfig {

    private final AppProperties props;

    public InfrastructureConfig(AppProperties props) {
        this.props = props;
    }

    private StaticCredentialsProvider getCredentials() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        props.getAws().getAccessKey(),
                        props.getAws().getSecretKey()
                )
        );
    }

    @Bean
    @Primary
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        return DynamoDbAsyncClient.builder()
                .region(Region.of(props.getAws().getRegion()))
                .endpointOverride(URI.create(props.getAws().getEndpoint()))
                .credentialsProvider(getCredentials())
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .build();
    }

    @Bean
    @Primary
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient asyncClient) {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(asyncClient)
                .build();
    }

    @Bean
    @Primary
    public SesAsyncClient sesAsyncClient(AppProperties props) {
        var builder = SesAsyncClient.builder()
                .region(Region.of(props.getAws().getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                props.getAws().getAccessKey(),
                                props.getAws().getSecretKey()
                        )
                ));

        String sesEndpoint = props.getAws().getSesEndpoint();
        if (sesEndpoint != null && !sesEndpoint.isBlank()) {
            String normalized = sesEndpoint.trim();
            if (normalized.contains("dynamodb")) {
                log.warn("AWS_SES_ENDPOINT apunta a DynamoDB ({}). Se ignora para SES.", normalized);
            } else {
                builder.endpointOverride(URI.create(normalized));
                log.info("SES endpoint override activo: {}", normalized);
            }
        }

        return builder.build();
    }

    @Bean
    public DynamoDbAsyncTable<FundEntity> fundTable(DynamoDbEnhancedAsyncClient enhancedClient) {
        String tableName = resolveTableName(props.getDynamodbAws().getFundsTable(), "funds");
        return enhancedClient.table(tableName, TableSchema.fromBean(FundEntity.class));
    }

    @Bean
    public DynamoDbAsyncTable<CustomerEntity> customerTable(DynamoDbEnhancedAsyncClient enhancedClient) {
        String tableName = resolveTableName(props.getDynamodbAws().getCustomersTable(), "customers");
        return enhancedClient.table(tableName, TableSchema.fromBean(CustomerEntity.class));
    }

    @Bean
    public DynamoDbAsyncTable<TransactionEntity> transactionTable(DynamoDbEnhancedAsyncClient enhancedClient) {
        String tableName = resolveTableName(props.getDynamodbAws().getTransactionsTable(), "transactions");
        return enhancedClient.table(tableName, TableSchema.fromBean(TransactionEntity.class));
    }

    private String resolveTableName(String configuredName, String defaultName) {
        if (configuredName == null || configuredName.isBlank()) {
            return defaultName;
        }
        return configuredName;
    }
}
