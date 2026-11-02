package com.apisentinel.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mock")
@ConditionalOnProperty(name = "sentinel.mock.enabled", havingValue = "true")
public class MockWebhookReceiverController {

    private static final Logger log = LoggerFactory.getLogger(MockWebhookReceiverController.class);

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(
            @RequestBody String body,
            @RequestHeader(value = "X-Sentinel-Signature", required = false) String signature,
            @RequestParam(defaultValue = "200") int status) {
        log.info("MOCK webhook received status={} signature={} body={}", status, signature, body);
        return ResponseEntity.status(status).build();
    }

    @GetMapping("/redirect")
    public ResponseEntity<Void> redirect(@RequestParam String to,
            @RequestParam(defaultValue = "302") int status) {
        return ResponseEntity.status(status).header("Location", to).build();
    }

    @GetMapping("/redirect-loop")
    public ResponseEntity<Void> redirectLoop() {
        return ResponseEntity.status(302).header("Location", "/mock/redirect-loop").build();
    }
}
