package com.apisentinel.dto.response;
import com.apisentinel.enums.AssertionOperator;
import com.apisentinel.enums.AssertionType;
import com.apisentinel.entity.MonitorAssertion;
import java.time.Instant;

public record AssertionResponse(
        Long id,
        AssertionType type,
        String path,
        AssertionOperator operator,
        String expectedValue,
        Instant createdAt
) {
    public static AssertionResponse from(MonitorAssertion a) {
        return new AssertionResponse(a.getId(), a.getAssertionType(), a.getJsonPath(),
                a.getOperator(), a.getExpectedValue(), a.getCreatedAt());
    }
}
