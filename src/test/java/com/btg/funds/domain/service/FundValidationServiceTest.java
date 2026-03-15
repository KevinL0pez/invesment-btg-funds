package com.btg.funds.domain.service;

import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FundValidationServiceTest {

    @Test
    void shouldThrowWhenCustomerHasInsufficientBalance() {
        Customer customer = Customer.builder()
                .id("c1")
                .balance(new BigDecimal("1000"))
                .build();
        Fund fund = Fund.builder()
                .id("f1")
                .name("Fund A")
                .minimumAmount(new BigDecimal("2000"))
                .build();

        assertThrows(InsufficientBalanceException.class, () -> FundValidationService.validateSubscription(customer, fund));
    }

    @Test
    void shouldPassWhenCustomerHasEnoughBalance() {
        Customer customer = Customer.builder()
                .id("c1")
                .balance(new BigDecimal("3000"))
                .build();
        Fund fund = Fund.builder()
                .id("f1")
                .minimumAmount(new BigDecimal("2000"))
                .build();

        assertDoesNotThrow(() -> FundValidationService.validateSubscription(customer, fund));
    }
}
