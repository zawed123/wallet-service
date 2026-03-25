package com.rs.payments.wallet.exception;

public class InsufficientFundsException extends RuntimeException{

    public InsufficientFundsException(String message) {
        super(message);
    }
}
