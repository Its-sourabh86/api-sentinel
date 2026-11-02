package com.apisentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.apisentinel.enums.HttpMethod;
@Entity
@Table(name = "monitors")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethod httpMethod = HttpMethod.GET;

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs;

    @Column(name = "expected_status", nullable = false)
    private Integer expectedStatus = 200;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "response_schema", columnDefinition = "TEXT")
    private String responseSchema;

    @Column(name = "next_run_at", nullable = false)
    private Instant nextRunAt;

    @Column(name = "owner_id", updatable = false)
    private Long ownerId;

    // JPA ko ye no-argument constructor chahiye
    protected Monitor() {
    }

    public Monitor(String name, String url, HttpMethod httpMethod,
            Integer intervalSeconds, Integer timeoutMs, Integer expectedStatus) {
        this.name = name;
        this.url = url;
        this.httpMethod = httpMethod;
        this.intervalSeconds = intervalSeconds;
        this.timeoutMs = timeoutMs;
        this.expectedStatus = expectedStatus;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.nextRunAt == null) {
            this.nextRunAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters (sirf wahi jo badalne chahiye)
    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getResponseSchema() {
        return responseSchema;
    }

    public void setResponseSchema(String responseSchema) {
        this.responseSchema = responseSchema;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    /** Agla check turant chalne ke liye (create, resume, interval change par). */
    public void rescheduleNow() {
        this.nextRunAt = Instant.now();
    }

    public Long getOwnerId() { return ownerId; }

    public void assignOwner(Long ownerId) {
    this.ownerId = ownerId;
    }

}
