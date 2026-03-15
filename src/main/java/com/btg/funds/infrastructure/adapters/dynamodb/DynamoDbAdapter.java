package com.btg.funds.infrastructure.adapters.dynamodb;

import com.btg.funds.application.ports.out.FundRepositoryPort;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.infrastructure.config.AppProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Repository
@Slf4j
@Data
@Builder
@DynamoDbBean
public class DynamoDbAdapter implements FundRepositoryPort {

    private final DynamoDbEnhancedAsyncClient enhancedClient;
    private final AppProperties props;

    public DynamoDbAdapter(
            @org.springframework.beans.factory.annotation.Qualifier("dynamoDbEnhancedAsyncClient")
            DynamoDbEnhancedAsyncClient enhancedClient,
            AppProperties props) {
        this.enhancedClient = enhancedClient;
        this.props = props;
    }

    @Override
    public Flux<Fund> findAll() {
        DynamoDbAsyncTable<Fund> table = enhancedClient.table(
                props.getDynamodbAws().getFundsTable(),
                TableSchema.fromBean(Fund.class));
        return Flux.from(table.scan().items());
    }

    @Override
    public Mono<Customer> getCustomerById(String id) {
        DynamoDbAsyncTable<Customer> table = enhancedClient.table(
                props.getDynamodbAws().getCustomersTable(),
                TableSchema.fromBean(Customer.class));
        return Mono.fromFuture(table.getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    @Override
    public Mono<Fund> getFundById(String id) {
        DynamoDbAsyncTable<Fund> table = enhancedClient.table(
                props.getDynamodbAws().getFundsTable(),
                TableSchema.fromBean(Fund.class));
        return Mono.fromFuture(table.getItem(r -> r.key(k -> k.partitionValue(id))));
    }

    @Override
    public Mono<Void> updateCustomerBalance(Customer customer) {
        DynamoDbAsyncTable<Customer> table = enhancedClient.table(
                props.getDynamodbAws().getCustomersTable(),
                TableSchema.fromBean(Customer.class));
        return Mono.fromFuture(table.updateItem(customer)).then();
    }

    @Override
    public Mono<Transaction> saveTransaction(Transaction transaction) {
        DynamoDbAsyncTable<Transaction> table = enhancedClient.table(
                props.getDynamodbAws().getTransactionsTable(),
                TableSchema.fromBean(Transaction.class));
        return Mono.fromFuture(table.putItem(transaction)).thenReturn(transaction);
    }

    @Override
    public Mono<Fund> saveFund(Fund fund) {
        DynamoDbAsyncTable<Fund> table = enhancedClient.table(
                props.getDynamodbAws().getFundsTable(),
                TableSchema.fromBean(Fund.class));
        return Mono.fromFuture(table.putItem(fund)).thenReturn(fund);
    }
}
