package com.apisentinel.utils;

import com.apisentinel.enums.FailureCode;

import com.apisentinel.enums.AssertionOperator;
public record AssertionResult(
        String path,
        AssertionOperator operator,
        String expected,
        String actual,
        boolean passed,
        String message,
        FailureCode failureCode
) {
    public static AssertionResult passed(AssertionSpec spec, String actual) {
        return new AssertionResult(spec.path(), spec.operator(), spec.expectedValue(),
                actual, true, "OK", null);
    }

    public static AssertionResult failed(AssertionSpec spec, String actual,
                                         String message, FailureCode code) {
        return new AssertionResult(spec.path(), spec.operator(), spec.expectedValue(),
                actual, false, message, code);
    }
}
