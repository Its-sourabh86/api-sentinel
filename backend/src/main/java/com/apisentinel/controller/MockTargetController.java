package com.apisentinel.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/mock")
@ConditionalOnProperty(name = "sentinel.mock.enabled", havingValue = "true")
public class MockTargetController {

    @GetMapping("/inventory/{id}")
    public ResponseEntity<Map<String, Object>> inventory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int stock,
            @RequestParam(defaultValue = "0") long delayMs,
            @RequestParam(defaultValue = "200") int status,
            @RequestParam(defaultValue = "0") long ageSeconds,
            @RequestParam(defaultValue = "false") boolean stockAsText) throws InterruptedException {

        if (delayMs > 0) {
            Thread.sleep(delayMs);
        }

        Object stockValue = stockAsText ? String.valueOf(stock) : stock;

        Map<String, Object> body = Map.of("data", Map.of(
                "productId", id,
                "stock", stockValue,
                "updatedAt", Instant.now().minusSeconds(ageSeconds).toString()));

        return ResponseEntity.status(status).body(body);
    }
}