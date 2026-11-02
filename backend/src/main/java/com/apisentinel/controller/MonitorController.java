package com.apisentinel.controller;

import com.apisentinel.dto.request.CreateMonitorRequest;
import com.apisentinel.dto.request.UpdateMonitorRequest;
import com.apisentinel.dto.response.MonitorListItemResponse;
import com.apisentinel.dto.response.MonitorResponse;
import com.apisentinel.security.AuthenticatedUser;
import com.apisentinel.service.MonitorAccess;
import com.apisentinel.service.MonitorOverviewService;
import com.apisentinel.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/monitors")
public class MonitorController {

    private final MonitorService monitorService;
    private final MonitorAccess monitorAccess;
    private final MonitorOverviewService overviewService;

    public MonitorController(MonitorService monitorService, MonitorAccess monitorAccess, MonitorOverviewService overviewService) {
        this.monitorService = monitorService;
        this.monitorAccess = monitorAccess;
        this.overviewService = overviewService;
    }

    @GetMapping
    public ResponseEntity<List<MonitorListItemResponse>> getAll(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(overviewService.list(user));
    }

    @PostMapping
    public ResponseEntity<MonitorResponse> create(@Valid @RequestBody CreateMonitorRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        MonitorResponse created = monitorService.create(request, user.id());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getById(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        monitorAccess.assertAccessible(id, user);
        return ResponseEntity.ok(monitorService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MonitorResponse> update(@PathVariable Long id,
            @Valid @RequestBody UpdateMonitorRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        monitorAccess.assertAccessible(id, user);
        return ResponseEntity.ok(monitorService.update(id, request));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<MonitorResponse> pause(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        monitorAccess.assertAccessible(id, user);
        return ResponseEntity.ok(monitorService.pause(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<MonitorResponse> resume(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        monitorAccess.assertAccessible(id, user);
        return ResponseEntity.ok(monitorService.resume(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user) {
        monitorAccess.assertAccessible(id, user);
        monitorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
