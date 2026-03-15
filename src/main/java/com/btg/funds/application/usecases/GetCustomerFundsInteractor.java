package com.btg.funds.application.usecases;

import com.btg.funds.application.ports.in.GetCustomerFundsUseCase;
import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.domain.model.Transaction;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.Comparator;

@RequiredArgsConstructor
public class GetCustomerFundsInteractor implements GetCustomerFundsUseCase {
    private final CustomerRepository customerRepository;

    @Override
    public Flux<Transaction> execute(String customerId) {
        return customerRepository.findTransactionsByCustomerId(customerId)
                .sort(Comparator.comparingLong(Transaction::getTimestamp).reversed())
                .switchIfEmpty(Flux.empty());
    }
}
