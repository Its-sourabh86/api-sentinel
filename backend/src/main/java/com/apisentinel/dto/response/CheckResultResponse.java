package com.apisentinel.dto.response;
import com.apisentinel.enums.ProbeOutcome;
import com.apisentinel.entity.CheckResult;
import com.apisentinel.enums.TriggerType;
import com.apisentinel.enums.CheckStatus;
import com.apisentinel.enums.FailureCode;
import java.time.Instant;

public record CheckResultResponse(
        Long id,
        Instant startedAt,
        long durationMs,
        Integer httpStatus,
        CheckStatus status,
        FailureCode failureCode,
        String reason,
        ProbeOutcome outcome,
        TriggerType trigger
) {
    public static CheckResultResponse from(CheckResult r) {
        return new CheckResultResponse(r.getId(), r.getStartedAt(), r.getDurationMs(),
                r.getHttpStatus(), r.getResultStatus(), r.getFailureCode(),
                r.getFailureReason(), r.getProbeOutcome(), r.getTriggerType());
    }
}
