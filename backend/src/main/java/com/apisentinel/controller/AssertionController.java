package com.apisentinel.controller;
import com.apisentinel.dto.response.AssertionResponse;
import com.apisentinel.dto.request.CreateAssertionRequest;
import com.apisentinel.service.AssertionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/monitors/{monitorId}/assertions")
public class AssertionController {

    private final AssertionService assertionService;

    public AssertionController(AssertionService assertionService) {
        this.assertionService = assertionService;
    }

    @PostMapping
    public ResponseEntity<AssertionResponse> add(@PathVariable Long monitorId,
                                                 @Valid @RequestBody CreateAssertionRequest request) {
        AssertionResponse created = assertionService.add(monitorId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<AssertionResponse>> list(@PathVariable Long monitorId) {
        return ResponseEntity.ok(assertionService.list(monitorId));
    }

    @DeleteMapping("/{assertionId}")
    public ResponseEntity<Void> delete(@PathVariable Long monitorId,
                                       @PathVariable Long assertionId) {
        assertionService.delete(monitorId, assertionId);
        return ResponseEntity.noContent().build();
    }
}
