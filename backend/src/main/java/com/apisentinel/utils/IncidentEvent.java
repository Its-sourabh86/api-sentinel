package com.apisentinel.utils;

import java.time.Instant;

import com.apisentinel.entity.Incident;
import com.apisentinel.enums.FailureCode;

public record IncidentEvent(
        Type type,
        Long incidentId,
        Long monitorId,
        String monitorName,
        FailureCode failureCode,
        String reason,
        int failureCount,
        Instant openedAt,
        Instant resolvedAt
) {
    public enum Type {
        OPENED,
        RESOLVED
    }

    public static IncidentEvent of(Type type, Incident i, String monitorName) {
        return new IncidentEvent(type, i.getId(), i.getMonitorId(), monitorName,
                i.getFailureCode(), i.getReason(), i.getFailureCount(),
                i.getOpenedAt(), i.getResolvedAt());
    }
}