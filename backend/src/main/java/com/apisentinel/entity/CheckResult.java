package com.apisentinel.entity;
import com.apisentinel.enums.ProbeOutcome;
import com.apisentinel.enums.TriggerType;
import com.apisentinel.enums.CheckStatus;
import com.apisentinel.enums.FailureCode;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitor_id", nullable = false, updatable = false)
    private Long monitorId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "duration_ms", nullable = false, updatable = false)
    private long durationMs;

    @Column(name = "http_status", updatable = false)
    private Integer httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20, updatable = false)
    private CheckStatus resultStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 30, updatable = false)
    private FailureCode failureCode;

    @Column(name = "failure_reason", length = 500, updatable = false)
    private String failureReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "probe_outcome", nullable = false, length = 30, updatable = false)
    private ProbeOutcome probeOutcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20, updatable = false)
    private TriggerType triggerType;

    protected CheckResult() {
    }

    public CheckResult(Long monitorId, Instant startedAt, long durationMs, Integer httpStatus,
                       CheckStatus resultStatus, FailureCode failureCode, String failureReason,
                       ProbeOutcome probeOutcome, TriggerType triggerType) {
        this.monitorId = monitorId;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
        this.httpStatus = httpStatus;
        this.resultStatus = resultStatus;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.probeOutcome = probeOutcome;
        this.triggerType = triggerType;
    }

    public Long getId() { return id; }
    public Long getMonitorId() { return monitorId; }
    public Instant getStartedAt() { return startedAt; }
    public long getDurationMs() { return durationMs; }
    public Integer getHttpStatus() { return httpStatus; }
    public CheckStatus getResultStatus() { return resultStatus; }
    public FailureCode getFailureCode() { return failureCode; }
    public String getFailureReason() { return failureReason; }
    public ProbeOutcome getProbeOutcome() { return probeOutcome; }
    public TriggerType getTriggerType() { return triggerType; }
}
