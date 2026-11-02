package com.apisentinel.utils;


import com.apisentinel.enums.ProbeOutcome;

public record ProbeResult(
        ProbeOutcome outcome,
        Integer httpStatus,
        long durationMs,
        String body,
        String errorMessage
) {
    public static ProbeResult responseReceived(int httpStatus, long durationMs, String body) {
        return new ProbeResult(ProbeOutcome.RESPONSE_RECEIVED, httpStatus, durationMs, body, null);
    }

    public static ProbeResult failure(ProbeOutcome outcome, long durationMs, String errorMessage) {
        return new ProbeResult(outcome, null, durationMs, null, errorMessage);
    }
}
