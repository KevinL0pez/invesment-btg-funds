package com.btg.funds.application.ports.in;

import com.btg.funds.domain.model.Transaction;
import reactor.core.publisher.Flux;

public interface GetCustomerFundsUseCase {
    Flux<Transaction> execute(String customerId);
}
