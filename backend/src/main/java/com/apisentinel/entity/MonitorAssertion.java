package com.apisentinel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.apisentinel.enums.AssertionOperator;
import com.apisentinel.enums.AssertionType;
@Entity
@Table(name = "monitor_assertions")
public class MonitorAssertion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    @Enumerated(EnumType.STRING)
    @Column(name = "assertion_type", nullable = false, length = 30)
    private AssertionType assertionType;

    @Column(name = "json_path", nullable = false, length = 500)
    private String jsonPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssertionOperator operator;

    @Column(name = "expected_value", length = 500)
    private String expectedValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MonitorAssertion() {
    }

    public MonitorAssertion(Monitor monitor, AssertionType assertionType, String jsonPath,
                            AssertionOperator operator, String expectedValue) {
        this.monitor = monitor;
        this.assertionType = assertionType;
        this.jsonPath = jsonPath;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public AssertionType getAssertionType() { return assertionType; }
    public String getJsonPath() { return jsonPath; }
    public AssertionOperator getOperator() { return operator; }
    public String getExpectedValue() { return expectedValue; }
    public Instant getCreatedAt() { return createdAt; }
}
