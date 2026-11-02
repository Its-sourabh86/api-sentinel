package com.apisentinel.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("invalid email or password");
    }
}