package com.btg.funds.domain.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("Cliente no encontrado: " + customerId);
    }
}
