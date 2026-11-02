package com.apisentinel.utils;
import com.apisentinel.enums.AssertionOperator;
public record AssertionSpec(String path, AssertionOperator operator, String expectedValue) {
}
