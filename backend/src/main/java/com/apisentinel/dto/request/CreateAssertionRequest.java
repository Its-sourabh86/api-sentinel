package com.apisentinel.dto.request;
import com.apisentinel.enums.AssertionOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssertionRequest(

        @NotBlank(message = "path is required")
        @Size(max = 500, message = "path must be at most 500 characters")
        String path,

        @NotNull(message = "operator is required")
        AssertionOperator operator,

        @Size(max = 500, message = "expectedValue must be at most 500 characters")
        String expectedValue
) {
}
