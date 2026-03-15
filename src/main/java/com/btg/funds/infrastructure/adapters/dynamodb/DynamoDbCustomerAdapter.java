package com.btg.funds.infrastructure.adapters.dynamodb;

import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.infrastructure.adapters.dynamodb.entities.CustomerEntity;
import com.btg.funds.infrastructure.adapters.dynamodb.entities.TransactionEntity;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Set;

@Repository
@Slf4j
@Data
@Builder
@DynamoDbBean
public class DynamoDbCustomerAdapter implements CustomerRepository {

    private final DynamoDbAsyncTable<CustomerEntity> customerTable;
    private final DynamoDbAsyncTable<TransactionEntity> transactionTable;

    @Override
    public Mono<Customer> findById(String id) {
        log.info("🔍 Buscando cliente: {}", id);
        return Mono.fromFuture(customerTable.getItem(Key.builder().partitionValue(id).build()))
                .map(this::toDomain)
                .doOnSuccess(c -> {
                    if (c == null) log.warn("Cliente no encontrado en DB: {}", id);
                });
    }

    @Override
    public Mono<Customer> save(Customer customer) {
        return Mono.fromFuture(customerTable.putItem(toEntity(customer)))
                .thenReturn(customer);
    }

    @Override
    public Mono<Transaction> saveTransaction(Transaction tx) {
        log.info("📝 Registrando transacción para: {}", tx.getCustomerId());
        return Mono.fromFuture(transactionTable.putItem(TransactionEntity.fromDomain(tx)))
                .thenReturn(tx);
    }

    @Override
    public Flux<Transaction> findTransactionsByCustomerId(String id) {
        QueryConditional query = QueryConditional.keyEqualTo(Key.builder().partitionValue(id).build());
        return Flux.from(transactionTable.query(query).items())
                .map(TransactionEntity::toDomain);
    }

    private Customer toDomain(CustomerEntity entity) {
        if (entity == null) return null;
        return Customer.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .balance(entity.getBalance())
                .activeSubscriptions(entity.getActiveSubscriptions())
                .build();
    }

    private CustomerEntity toEntity(Customer customer) {
        return CustomerEntity.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .balance(customer.getBalance())
                .activeSubscriptions(normalizeStringSet(customer.getActiveSubscriptions()))
                .build();
    }

    private Set<String> normalizeStringSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values;
    }
}
