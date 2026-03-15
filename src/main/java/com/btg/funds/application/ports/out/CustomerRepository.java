package com.btg.funds.application.ports.out;

import com.btg.funds.domain.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepository {
    Mono<Customer> findById(String id);
    Mono<Customer> save(Customer customer);
    Mono<Transaction> saveTransaction(Transaction tx);
    Flux<Transaction> findTransactionsByCustomerId(String id);
}
