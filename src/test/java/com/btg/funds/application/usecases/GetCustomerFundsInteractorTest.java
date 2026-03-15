package com.btg.funds.application.usecases;

import com.btg.funds.application.ports.out.CustomerRepository;
import com.btg.funds.domain.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCustomerFundsInteractorTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private GetCustomerFundsInteractor interactor;

    @Test
    void shouldReturnTransactionsSortedDescByTimestamp() {
        Transaction older = Transaction.builder().id("t1").timestamp(100L).build();
        Transaction newer = Transaction.builder().id("t2").timestamp(200L).build();

        when(customerRepository.findTransactionsByCustomerId("c1"))
                .thenReturn(Flux.just(older, newer));

        StepVerifier.create(interactor.execute("c1").map(Transaction::getId))
                .expectNext("t2", "t1")
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenNoTransactions() {
        when(customerRepository.findTransactionsByCustomerId("c1"))
                .thenReturn(Flux.empty());

        StepVerifier.create(interactor.execute("c1"))
                .verifyComplete();
    }
}
