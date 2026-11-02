package com.apisentinel.service;

import com.apisentinel.dto.response.*;
import com.apisentinel.entity.CheckResult;
import com.apisentinel.enums.HttpMethod;
import com.apisentinel.enums.Role;
import com.apisentinel.repository.CheckResultRepository;
import com.apisentinel.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorOverviewServiceTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(1L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

    @Mock
    private MonitorService monitorService;
    @Mock
    private CheckResultRepository checkResultRepository;
    @InjectMocks
    private MonitorOverviewService service;

    private MonitorResponse monitor(long id) {
        return new MonitorResponse(id, "m" + id, "https://example.com", HttpMethod.GET,
                60, 3000, 200, true, NOW, NOW);
    }

    private CheckResult failedCheck(long monitorId) {
        return new CheckResult(monitorId, NOW, 25, 200, com.apisentinel.enums.CheckStatus.FAILED,
                com.apisentinel.enums.FailureCode.JSON_ASSERTION_FAILED, "stock 0",
                com.apisentinel.enums.ProbeOutcome.RESPONSE_RECEIVED, com.apisentinel.enums.TriggerType.SCHEDULED);
    }

    @Test
    @DisplayName("har monitor ke saath uska apna latest check jude, jiska nahi uska null")
    void attachesLatestCheckPerMonitor() {
        when(monitorService.getAll(USER)).thenReturn(List.of(monitor(1), monitor(2)));
        when(checkResultRepository.findLatestForMonitors(List.of(1L, 2L)))
                .thenReturn(List.of(failedCheck(2)));

        List<MonitorListItemResponse> items = service.list(USER);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).lastCheck()).isNull();
        assertThat(items.get(1).lastCheck().status()).isEqualTo(com.apisentinel.enums.CheckStatus.FAILED);
        assertThat(items.get(1).lastCheck().failureCode()).isEqualTo("JSON_ASSERTION_FAILED");
    }

    @Test
    @DisplayName("monitors na ho toh check wali query chalti hi nahi")
    void emptyMonitors_noQuery() {
        when(monitorService.getAll(USER)).thenReturn(List.of());

        assertThat(service.list(USER)).isEmpty();
        verifyNoInteractions(checkResultRepository);
    }
}