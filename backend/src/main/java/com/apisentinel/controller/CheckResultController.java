package com.apisentinel.controller;
import com.apisentinel.dto.response.PageResponse;
import com.apisentinel.dto.response.CheckResultResponse;
import com.apisentinel.service.CheckResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monitors/{monitorId}/results")
public class CheckResultController {

    private final CheckResultService checkResultService;

    public CheckResultController(CheckResultService checkResultService) {
        this.checkResultService = checkResultService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CheckResultResponse>> list(
            @PathVariable Long monitorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(checkResultService.list(monitorId, page, size));
    }
}
