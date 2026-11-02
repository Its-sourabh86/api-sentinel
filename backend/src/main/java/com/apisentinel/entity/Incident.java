package com.apisentinel.entity;

import java.time.Instant;

import com.apisentinel.enums.FailureCode;
import com.apisentinel.enums.IncidentStatus;

import jakarta.persistence.*;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_id", nullable = false, updatable = false)
    private Long monitorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 30)
    private FailureCode failureCode;

    @Column(length = 500)
    private String reason;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    @Column(name = "last_failure_at", nullable = false)
    private Instant lastFailureAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "last_notified_at", updatable = false)
    private Instant lastNotifiedAt;

    public Instant getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    protected Incident() {
    }

    public static Incident open(Long monitorId, FailureCode code, String reason,
            int failureCount, Instant now) {
        Incident i = new Incident();
        i.monitorId = monitorId;
        i.status = IncidentStatus.OPEN;
        i.failureCode = code;
        i.reason = reason;
        i.failureCount = failureCount;
        i.openedAt = now;
        i.lastFailureAt = now;
        return i;
    }

    /**
     * Incident khula hai aur ek aur failure aaya: naya incident nahi, sirf count
     * badhao.
     */
    public void recordFailure(FailureCode code, String reason, Instant now) {
        this.failureCount++;
        this.lastFailureAt = now;
        this.failureCode = code;
        this.reason = reason;
    }

    public void resolve(Instant now) {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public FailureCode getFailureCode() {
        return failureCode;
    }

    public String getReason() {
        return reason;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getLastFailureAt() {
        return lastFailureAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}