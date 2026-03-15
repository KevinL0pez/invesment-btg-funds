package com.btg.funds.application.usecases;

import com.btg.funds.application.ports.in.SubscribeFundUseCase;
import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.application.ports.out.FundRepository;
import com.btg.funds.application.ports.out.NotificationPort;
import com.btg.funds.domain.exception.CustomerNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.service.FundValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SubscribeFundInteractor implements SubscribeFundUseCase {

    private static final ZoneId NOTIFICATION_ZONE = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CustomerRepository customerRepository;
    private final FundRepository fundRepository;
    private final NotificationPort notificationPort;

    @Override
    public Mono<Transaction> execute(String customerId, String fundId, String notificationType) {
        return Mono.zip(
                        customerRepository.findById(customerId)
                                .switchIfEmpty(Mono.error(new CustomerNotFoundException(customerId))),
                        fundRepository.findById(fundId)
                                .switchIfEmpty(Mono.error(new FundNotFoundException(fundId)))
                )
                .flatMap(tuple -> {
                    Customer customer = tuple.getT1();
                    Fund fund = tuple.getT2();

                    FundValidationService.validateSubscription(customer, fund);

                    customer.subtractBalance(fund.getMinimumAmount());
                    customer.addSubscription(fund.getId());

                    Transaction transaction = Transaction.builder()
                            .id(UUID.randomUUID().toString())
                            .customerId(customer.getId())
                            .fundId(fund.getId())
                            .fundName(fund.getName())
                            .type("OPENING")
                            .amount(fund.getMinimumAmount())
                            .timestamp(System.currentTimeMillis())
                            .build();

                    return customerRepository.save(customer)
                            .then(customerRepository.saveTransaction(transaction))
                            .flatMap(savedTx ->
                                    notificationPort.send(customer, buildSubscriptionMessage(customer, fund, savedTx), notificationType)
                                            .onErrorResume(e -> {
                                                log.error("Error enviando notificación, pero la transacción se guardó");
                                                return Mono.empty();
                                            })
                                            .thenReturn(savedTx)
                            );
                });
    }

    private String buildSubscriptionMessage(Customer customer, Fund fund, Transaction transaction) {
        String timestamp = DATE_FORMATTER.format(
                Instant.ofEpochMilli(transaction.getTimestamp()).atZone(NOTIFICATION_ZONE)
        );
        BigDecimal newBalance = customer.getBalance();

        return String.format(
                "Suscripcion confirmada.%nCliente: %s%nFondo: %s%nMonto debitado: %s%nSaldo disponible: %s%nFecha: %s%nId transaccion: %s",
                customer.getName(),
                fund.getName(),
                transaction.getAmount().toPlainString(),
                newBalance.toPlainString(),
                timestamp,
                transaction.getId()
        );
    }
}
