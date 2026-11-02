package com.apisentinel.enums;

public enum AssertionOperator {
    EQ(true, false),
    NEQ(true, false),
    GT(true, true),
    GTE(true, true),
    LT(true, true),
    LTE(true, true),
    EXISTS(false, false),
    NOT_EMPTY(false, false),
    AGE_LTE(true, true);

    private final boolean requiresExpectedValue;
    private final boolean numeric;

    AssertionOperator(boolean requiresExpectedValue, boolean numeric) {
        this.requiresExpectedValue = requiresExpectedValue;
        this.numeric = numeric;
    }

    public boolean requiresExpectedValue() { return requiresExpectedValue; }
    public boolean isNumeric() { return numeric; }
}
