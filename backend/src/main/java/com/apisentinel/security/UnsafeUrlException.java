package com.apisentinel.security;

public class UnsafeUrlException extends RuntimeException {

    public enum Reason {
        INVALID_URL,     // scheme, format, credentials ya host ki dikkat
        BLOCKED_TARGET,  // address policy ne roka
        UNRESOLVABLE     // DNS se IP nahi mila
    }

    private final Reason reason;

    public UnsafeUrlException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}