package com.btg.funds.domain.service;

import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.model.Customer;
import com.btg.funds.domain.model.Fund;

public class FundValidationService {

    public static void validateSubscription(Customer customer, Fund fund) {
        if (customer.getBalance().compareTo(fund.getMinimumAmount()) < 0) {
            throw new InsufficientBalanceException(fund.getName());
        }
    }
}
