package com.apisentinel.dto.request;
import com.apisentinel.enums.HttpMethod;
import jakarta.validation.constraints.*;

public record CreateMonitorRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url,

        HttpMethod method,

        @NotNull(message = "intervalSeconds is required")
        @Min(value = 10, message = "intervalSeconds must be at least 10")
        Integer intervalSeconds,

        @NotNull(message = "timeoutMs is required")
        @Min(value = 1, message = "timeoutMs must be positive")
        @Max(value = 30000, message = "timeoutMs must be at most 30000")
        Integer timeoutMs,

        @Min(value = 100, message = "expectedStatus must be a valid HTTP status")
        @Max(value = 599, message = "expectedStatus must be a valid HTTP status")
        Integer expectedStatus
) {
}
