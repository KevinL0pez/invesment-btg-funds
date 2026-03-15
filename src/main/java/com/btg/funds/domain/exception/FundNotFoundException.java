package com.btg.funds.domain.exception;

public class FundNotFoundException extends RuntimeException {

    public FundNotFoundException(String fundId) {
        super("Fondo no encontrado: " + fundId);
    }
}
