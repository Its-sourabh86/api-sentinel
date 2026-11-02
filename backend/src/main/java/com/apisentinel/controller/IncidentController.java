package com.apisentinel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.apisentinel.dto.response.IncidentResponse;
import com.apisentinel.dto.response.PageResponse;
import com.apisentinel.enums.IncidentStatus;
import com.apisentinel.security.AuthenticatedUser;
import com.apisentinel.service.IncidentService;
import com.apisentinel.service.MonitorAccess;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final MonitorAccess monitorAccess;

    public IncidentController(IncidentService incidentService,MonitorAccess monitorAccess) {
        this.incidentService = incidentService;
        this.monitorAccess = monitorAccess;
    }

    @GetMapping
    public ResponseEntity<PageResponse<IncidentResponse>> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long monitorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        if (monitorId != null) {
            monitorAccess.assertAccessible(monitorId, user);
        }
        return ResponseEntity.ok(incidentService.list(user, status, monitorId, page, size));
    }
}
