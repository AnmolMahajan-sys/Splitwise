package com.example.splitwise.exception;

public class InvalidSettlementException extends RuntimeException {
    public InvalidSettlementException(String message) {
        super(message);
    }
}
