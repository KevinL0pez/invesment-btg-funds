package com.btg.funds.application.usecases;

import com.btg.funds.application.ports.in.UnsubscribeFundUseCase;
import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.application.ports.out.FundRepository;
import com.btg.funds.domain.exception.CustomerNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.exception.SubscriptionNotFoundException;
import com.btg.funds.domain.model.Transaction;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class UnsubscribeFundInteractor implements UnsubscribeFundUseCase {

    private final CustomerRepository customerRepository;
    private final FundRepository fundRepository;

    @Override
    public Mono<Transaction> execute(String customerId, String fundId) {
        return Mono.zip(
                customerRepository.findById(customerId)
                        .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId))),
                fundRepository.findById(fundId)
                        .switchIfEmpty(Mono.error(new FundNotFoundException(fundId)))
        ).flatMap(tuple -> {
            var customer = tuple.getT1();
            var fund = tuple.getT2();

            // Verificamos si realmente tiene la suscripción
            if (!customer.hasSubscription(fundId)) {
                return Mono.error(new SubscriptionNotFoundException(fund.getName()));
            }

            // Regla de negocio: El valor se retorna al cliente
            customer.addBalance(fund.getMinimumAmount());
            customer.removeSubscription(fundId);

            Transaction transaction = Transaction.builder()
                    .id(UUID.randomUUID().toString())
                    .customerId(customerId)
                    .fundId(fundId)
                    .fundName(fund.getName())
                    .amount(fund.getMinimumAmount())
                    .type("CANCELLATION") // Sugerencia: Usa un nombre estándar para el historial
                    .timestamp(System.currentTimeMillis())
                    .build();

            return customerRepository.save(customer)
                    .then(customerRepository.saveTransaction(transaction))
                    .thenReturn(transaction);
        });
    }

}
