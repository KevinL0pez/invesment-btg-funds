package com.btg.funds.application.usecases;

import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.application.ports.out.FundRepository;
import com.btg.funds.application.ports.out.NotificationPort;
import com.btg.funds.domain.exception.CustomerNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscribeFundInteractorTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private FundRepository fundRepository;
    @Mock
    private NotificationPort notificationPort;

    @InjectMocks
    private SubscribeFundInteractor interactor;

    @Test
    void shouldSubscribeAndPersistTransaction() {
        Customer customer = Customer.builder()
                .id("c1")
                .name("Kevin")
                .balance(new BigDecimal("500000"))
                .activeSubscriptions(new HashSet<>())
                .build();
        Fund fund = Fund.builder()
                .id("f1")
                .name("Fund Test")
                .minimumAmount(new BigDecimal("100000"))
                .build();

        when(customerRepository.findById("c1")).thenReturn(Mono.just(customer));
        when(fundRepository.findById("f1")).thenReturn(Mono.just(fund));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(customerRepository.saveTransaction(any(Transaction.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(notificationPort.send(any(), any(), eq("EMAIL"))).thenReturn(Mono.empty());

        StepVerifier.create(interactor.execute("c1", "f1", "EMAIL"))
                .assertNext(tx -> {
                    assertEquals("c1", tx.getCustomerId());
                    assertEquals("f1", tx.getFundId());
                    assertEquals("OPENING", tx.getType());
                    assertEquals(new BigDecimal("100000"), tx.getAmount());
                })
                .verifyComplete();

        assertEquals(new BigDecimal("400000"), customer.getBalance());
        assertTrue(customer.getActiveSubscriptions().contains("f1"));
    }

    @Test
    void shouldReturnCustomerNotFound() {
        when(customerRepository.findById("c1")).thenReturn(Mono.empty());
        when(fundRepository.findById("f1")).thenReturn(Mono.just(Fund.builder().id("f1").minimumAmount(BigDecimal.ONE).build()));

        StepVerifier.create(interactor.execute("c1", "f1", "SMS"))
                .expectError(CustomerNotFoundException.class)
                .verify();
    }

    @Test
    void shouldReturnFundNotFound() {
        when(customerRepository.findById("c1")).thenReturn(Mono.just(Customer.builder().id("c1").balance(new BigDecimal("1000")).build()));
        when(fundRepository.findById("f1")).thenReturn(Mono.empty());

        StepVerifier.create(interactor.execute("c1", "f1", "SMS"))
                .expectError(FundNotFoundException.class)
                .verify();
    }

    @Test
    void shouldReturnInsufficientBalanceWhenBalanceTooLow() {
        Customer customer = Customer.builder().id("c1").balance(new BigDecimal("10")).build();
        Fund fund = Fund.builder().id("f1").name("Fund X").minimumAmount(new BigDecimal("20")).build();

        when(customerRepository.findById("c1")).thenReturn(Mono.just(customer));
        when(fundRepository.findById("f1")).thenReturn(Mono.just(fund));

        StepVerifier.create(interactor.execute("c1", "f1", "SMS"))
                .expectError(InsufficientBalanceException.class)
                .verify();
    }
}
