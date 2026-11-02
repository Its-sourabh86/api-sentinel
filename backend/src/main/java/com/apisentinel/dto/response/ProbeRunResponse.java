package com.apisentinel.dto.response;

import com.apisentinel.enums.ProbeOutcome;

import com.apisentinel.utils.AssertionResult;
import com.apisentinel.enums.CheckStatus;
import com.apisentinel.enums.FailureCode;
import java.util.List;

public record ProbeRunResponse(
        Long monitorId,
        CheckStatus status,
        FailureCode failureCode,
        String reason,
        ProbeOutcome outcome,
        Integer httpStatus,
        long durationMs,
        List<AssertionResult> assertions
) {
}
