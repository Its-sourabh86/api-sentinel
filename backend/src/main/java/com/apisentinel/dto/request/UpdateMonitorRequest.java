package com.apisentinel.dto.request;
import com.apisentinel.enums.HttpMethod;
import jakarta.validation.constraints.*;

public record UpdateMonitorRequest(

        @Size(min = 1, max = 100, message = "name must be 1-100 characters")
        String name,

        @Size(min = 1, max = 2048, message = "url must be 1-2048 characters")
        String url,

        HttpMethod method,

        @Min(value = 10, message = "intervalSeconds must be at least 10")
        Integer intervalSeconds,

        @Min(value = 1, message = "timeoutMs must be positive")
        @Max(value = 30000, message = "timeoutMs must be at most 30000")
        Integer timeoutMs,

        @Min(value = 100, message = "expectedStatus must be a valid HTTP status")
        @Max(value = 599, message = "expectedStatus must be a valid HTTP status")
        Integer expectedStatus
) {
}
