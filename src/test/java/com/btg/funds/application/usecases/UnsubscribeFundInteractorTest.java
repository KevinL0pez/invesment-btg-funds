package com.btg.funds.application.usecases;

import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.application.ports.out.FundRepository;
import com.btg.funds.domain.exception.SubscriptionNotFoundException;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnsubscribeFundInteractorTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private FundRepository fundRepository;

    @InjectMocks
    private UnsubscribeFundInteractor interactor;

    @Test
    void shouldUnsubscribeAndCreateCancellationTransaction() {
        Customer customer = Customer.builder()
                .id("c1")
                .balance(new BigDecimal("500000"))
                .activeSubscriptions(new HashSet<>(Set.of("f1")))
                .build();
        Fund fund = Fund.builder()
                .id("f1")
                .name("Fund A")
                .minimumAmount(new BigDecimal("100000"))
                .build();

        when(customerRepository.findById("c1")).thenReturn(Mono.just(customer));
        when(fundRepository.findById("f1")).thenReturn(Mono.just(fund));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(customerRepository.saveTransaction(any(Transaction.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(interactor.execute("c1", "f1"))
                .assertNext(tx -> {
                    assertEquals("CANCELLATION", tx.getType());
                    assertEquals("f1", tx.getFundId());
                })
                .verifyComplete();
    }

    @Test
    void shouldFailWhenSubscriptionDoesNotExist() {
        Customer customer = Customer.builder()
                .id("c1")
                .balance(new BigDecimal("500000"))
                .activeSubscriptions(Set.of())
                .build();
        Fund fund = Fund.builder()
                .id("f1")
                .name("Fund A")
                .minimumAmount(new BigDecimal("100000"))
                .build();

        when(customerRepository.findById("c1")).thenReturn(Mono.just(customer));
        when(fundRepository.findById("f1")).thenReturn(Mono.just(fund));

        StepVerifier.create(interactor.execute("c1", "f1"))
                .expectError(SubscriptionNotFoundException.class)
                .verify();
    }
}
