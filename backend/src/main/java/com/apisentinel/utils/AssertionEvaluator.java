package com.apisentinel.utils;

import com.apisentinel.enums.FailureCode;

import com.apisentinel.enums.AssertionOperator;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Map;

@Component
public class AssertionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AssertionEvaluator.class);
    private static final int MAX_ACTUAL_LENGTH = 200;

    private final Clock clock;

    public AssertionEvaluator(Clock clock) {
        this.clock = clock;
    }

    public AssertionResult evaluate(DocumentContext doc, AssertionSpec spec) {
        Object actual;
        try {
            actual = doc.read(spec.path());
        } catch (JsonPathException e) {
            return AssertionResult.failed(spec, null,
                    "Path not found: " + spec.path(), FailureCode.JSON_ASSERTION_FAILED);
        }

        if (spec.operator() == AssertionOperator.AGE_LTE) {
            return evaluateFreshness(actual, spec);
        }

        boolean passed = switch (spec.operator()) {
            case EXISTS -> true;
            case NOT_EMPTY -> isNotEmpty(actual);
            case EQ -> isEqual(actual, spec.expectedValue());
            case NEQ -> !isEqual(actual, spec.expectedValue());
            case GT, GTE, LT, LTE -> compare(actual, spec);
            case AGE_LTE -> throw new IllegalStateException("handled above");
        };

        String shown = display(actual);
        if (passed) {
            return AssertionResult.passed(spec, shown);
        }
        String expectedPart = spec.expectedValue() != null ? " " + spec.expectedValue() : "";
        return AssertionResult.failed(spec, shown,
                "Expected " + spec.path() + " " + spec.operator() + expectedPart + ", actual " + shown,
                FailureCode.JSON_ASSERTION_FAILED);
    }

    private AssertionResult evaluateFreshness(Object actual, AssertionSpec spec) {
        String shown = display(actual);

        if (!(actual instanceof String text)) {
            return AssertionResult.failed(spec, shown,
                    spec.path() + " must be an ISO-8601 timestamp string, actual " + shown,
                    FailureCode.JSON_ASSERTION_FAILED);
        }

        Instant timestamp;
        try {
            timestamp = OffsetDateTime.parse(text.trim()).toInstant();
        } catch (DateTimeParseException e) {
            log.debug("Unparseable timestamp in freshness check");
            return AssertionResult.failed(spec, shown,
                    spec.path() + " is not a valid ISO-8601 timestamp, actual " + shown,
                    FailureCode.JSON_ASSERTION_FAILED);
        }

        long maxAgeSeconds = new BigDecimal(spec.expectedValue()).longValue();
        long ageSeconds = Duration.between(timestamp, Instant.now(clock)).getSeconds();

        // Negative age = timestamp future mein hai (clock skew). Ise fresh maante hain.
        if (ageSeconds <= maxAgeSeconds) {
            return AssertionResult.passed(spec, shown);
        }
        return AssertionResult.failed(spec, shown,
                "Data is " + ageSeconds + "s old, max allowed " + maxAgeSeconds
                        + "s (" + spec.path() + "=" + shown + ")",
                FailureCode.STALE_DATA);
    }

    private boolean isNotEmpty(Object actual) {
        if (actual == null) return false;
        if (actual instanceof String s) return !s.isBlank();
        if (actual instanceof Collection<?> c) return !c.isEmpty();
        if (actual instanceof Map<?, ?> m) return !m.isEmpty();
        return true;
    }

    private boolean isEqual(Object actual, String expected) {
        if (actual instanceof Number) {
            BigDecimal a = toNumber(actual);
            BigDecimal e = parse(expected);
            return a != null && e != null && a.compareTo(e) == 0;
        }
        return String.valueOf(actual).equals(expected);
    }

    private boolean compare(Object actual, AssertionSpec spec) {
        BigDecimal a = toNumber(actual);
        BigDecimal e = parse(spec.expectedValue());
        if (a == null || e == null) return false;

        int c = a.compareTo(e);
        return switch (spec.operator()) {
            case GT -> c > 0;
            case GTE -> c >= 0;
            case LT -> c < 0;
            case LTE -> c <= 0;
            default -> false;
        };
    }

    private BigDecimal toNumber(Object value) {
        if (!(value instanceof Number)) return null;
        return parse(value.toString());
    }

    private BigDecimal parse(String text) {
        if (text == null) return null;
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String display(Object actual) {
        if (actual == null) return "null";
        String text = actual.toString();
        return text.length() > MAX_ACTUAL_LENGTH ? text.substring(0, MAX_ACTUAL_LENGTH) + "..." : text;
    }
}
