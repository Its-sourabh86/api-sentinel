package com.apisentinel.controller;


import com.apisentinel.dto.response.ProbeRunResponse;
import com.apisentinel.service.ProbeService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
  @RequestMapping("/api/v1/monitors")
public class ProbeController {

    private final ProbeService probeService;

    public ProbeController(ProbeService probeService) {
        this.probeService = probeService;
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<ProbeRunResponse> run(@PathVariable Long id) {
        return ResponseEntity.ok(probeService.runNow(id));
    }
} 
