package com.btg.funds.application.ports.in;

import com.btg.funds.domain.model.Transaction;
import reactor.core.publisher.Mono;


public interface SubscribeFundUseCase {
    Mono<Transaction> execute(String customerId, String fundId, String notificationType);
}
