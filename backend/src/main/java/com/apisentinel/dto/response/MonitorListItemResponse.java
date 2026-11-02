package com.apisentinel.dto.response;


public record MonitorListItemResponse(MonitorResponse monitor, LastCheckResponse lastCheck) {
}