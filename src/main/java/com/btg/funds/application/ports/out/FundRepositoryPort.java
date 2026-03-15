package com.btg.funds.application.ports.out;

import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FundRepositoryPort {
    Flux<Fund> findAll();
    Mono<Customer> getCustomerById(String id);
    Mono<Fund> getFundById(String id);
    Mono<Void> updateCustomerBalance(Customer customer);
    Mono<Transaction> saveTransaction(Transaction transaction);
    Mono<Fund> saveFund(Fund fund);
}
