package com.apisentinel.utils;

import com.apisentinel.enums.FailureCode;

import com.apisentinel.enums.ProbeOutcome;
import com.apisentinel.utils.ProbeResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(ResponseValidator.class);
    private static final int MAX_SCHEMA_ERRORS_SHOWN = 3;

    private final AssertionEvaluator evaluator;
    private final JsonSchemaChecker schemaChecker;
    private final ObjectMapper objectMapper;

    public ResponseValidator(AssertionEvaluator evaluator,
                             JsonSchemaChecker schemaChecker,
                             ObjectMapper objectMapper) {
        this.evaluator = evaluator;
        this.schemaChecker = schemaChecker;
        this.objectMapper = objectMapper;
    }

    public ValidationReport validate(ProbeResult probe, int expectedStatus,
                                     String responseSchema, List<AssertionSpec> specs) {

        // 1. Response aaya hi nahi
        if (probe.outcome() != ProbeOutcome.RESPONSE_RECEIVED) {
            return ValidationReport.failed(fromOutcome(probe.outcome()), probe.errorMessage(), List.of());
        }

        // 2. HTTP status galat
        if (!probe.httpStatus().equals(expectedStatus)) {
            return ValidationReport.failed(FailureCode.STATUS_MISMATCH,
                    "Expected HTTP " + expectedStatus + " but got " + probe.httpStatus(), List.of());
        }

        boolean hasSchema = responseSchema != null && !responseSchema.isBlank();

        // 3. Na schema, na assertion: status match hi kaafi
        if (specs.isEmpty() && !hasSchema) {
            return ValidationReport.passed(List.of());
        }

        // 4. Body JSON hai?
        JsonNode tree;
        try {
            tree = objectMapper.readTree(probe.body());
        } catch (JsonProcessingException e) {
            return ValidationReport.failed(FailureCode.INVALID_JSON,
                    "Response body is not valid JSON", List.of());
        }
        if (tree == null || tree.isMissingNode()) {
            return ValidationReport.failed(FailureCode.INVALID_JSON,
                    "Response body is empty", List.of());
        }

        // 5. Schema (structure)
        if (hasSchema) {
            ValidationReport schemaFailure = checkSchema(tree, responseSchema);
            if (schemaFailure != null) {
                return schemaFailure;
            }
        }

        // 6. Assertions (values)
        if (specs.isEmpty()) {
            return ValidationReport.passed(List.of());
        }

        DocumentContext doc = JsonPath.parse(probe.body());
        List<AssertionResult> results = specs.stream()
                .map(spec -> evaluator.evaluate(doc, spec))
                .toList();

        return results.stream()
                .filter(r -> !r.passed())
                .findFirst()
                .map(firstFailure -> ValidationReport.failed(
                        firstFailure.failureCode(), firstFailure.message(), results))
                .orElseGet(() -> ValidationReport.passed(results));
    }

    /** Schema theek ho toh null, warna failed report. */
    private ValidationReport checkSchema(JsonNode tree, String schemaText) {
        List<String> errors;
        try {
            errors = schemaChecker.check(tree, schemaText);
        } catch (RuntimeException e) {
            // Save ke waqt validate hota hai, yahan aana Sentinel ka apna bug hai
            log.error("Stored schema could not be evaluated", e);
            return ValidationReport.failed(FailureCode.PROBE_ERROR,
                    "Stored schema could not be evaluated", List.of());
        }

        if (errors.isEmpty()) {
            return null;
        }

        String shown = String.join("; ", errors.stream().limit(MAX_SCHEMA_ERRORS_SHOWN).toList());
        int hidden = errors.size() - MAX_SCHEMA_ERRORS_SHOWN;
        String reason = "Schema violation: " + shown + (hidden > 0 ? " (+" + hidden + " more)" : "");
        return ValidationReport.failed(FailureCode.SCHEMA_VIOLATION, reason, List.of());
    }

    private FailureCode fromOutcome(ProbeOutcome outcome) {
        return switch (outcome) {
            case TIMEOUT -> FailureCode.TIMEOUT;
            case CONNECTION_FAILED -> FailureCode.CONNECTION_FAILED;
            case INVALID_URL -> FailureCode.INVALID_URL;
            case RESPONSE_TOO_LARGE -> FailureCode.RESPONSE_TOO_LARGE;
            case ERROR, RESPONSE_RECEIVED -> FailureCode.PROBE_ERROR;
            case BLOCKED_TARGET -> FailureCode.BLOCKED_TARGET;
        };
    }
}
