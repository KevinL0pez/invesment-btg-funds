package com.btg.funds.domain.exception;

public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(String fundName) {
        super("Ya no se encuentra suscrito al fondo " + fundName);
    }
}
