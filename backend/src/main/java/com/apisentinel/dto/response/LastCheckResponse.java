package com.apisentinel.dto.response;

import com.apisentinel.entity.CheckResult;
import com.apisentinel.enums.CheckStatus;

import java.time.Instant;

public record LastCheckResponse(
        CheckStatus status,
        String failureCode,
        String reason,
        Integer httpStatus,
        long durationMs,
        Instant checkedAt
) {
    public static LastCheckResponse from(CheckResult r) {
        return new LastCheckResponse(
                r.getResultStatus(),
                r.getFailureCode() == null ? null : r.getFailureCode().name(),
                r.getFailureReason(),
                r.getHttpStatus(),
                r.getDurationMs(),
                r.getStartedAt());
    }
}
