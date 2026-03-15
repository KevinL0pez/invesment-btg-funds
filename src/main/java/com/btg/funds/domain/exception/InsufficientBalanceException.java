package com.btg.funds.domain.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String fundName) {
        super("No tiene saldo disponible para vincularse al fondo " + fundName);
    }
}
