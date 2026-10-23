package com.example.splitwise.exception;

public class DuplicateGroupMembershipException extends RuntimeException{

    public DuplicateGroupMembershipException(String message) {
        super(message);
    }
}
