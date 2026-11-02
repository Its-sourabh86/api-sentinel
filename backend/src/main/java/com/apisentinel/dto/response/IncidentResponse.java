package com.apisentinel.dto.response;

import java.time.Instant;

import com.apisentinel.entity.Incident;
import com.apisentinel.enums.FailureCode;
import com.apisentinel.enums.IncidentStatus;

public record IncidentResponse(
        Long id,
        Long monitorId,
        IncidentStatus status,
        FailureCode failureCode,
        String reason,
        int failureCount,
        Instant openedAt,
        Instant lastFailureAt,
        Instant resolvedAt
) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(i.getId(), i.getMonitorId(), i.getStatus(),
                i.getFailureCode(), i.getReason(), i.getFailureCount(),
                i.getOpenedAt(), i.getLastFailureAt(), i.getResolvedAt());
    }

    
}
