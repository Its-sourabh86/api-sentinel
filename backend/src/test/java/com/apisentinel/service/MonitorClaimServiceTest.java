package com.apisentinel.service;
import com.apisentinel.entity.Monitor;
import com.apisentinel.enums.HttpMethod;
import com.apisentinel.repository.MonitorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorClaimServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private MonitorRepository monitorRepository;

    @Test
    @DisplayName("claim: next_run_at = now + interval ho aur save call ho")
    void claimDue_advancesNextRunAt() {
        Monitor monitor = new Monitor("A", "https://example.com", HttpMethod.GET, 60, 3000, 200);
        when(monitorRepository.findDueForUpdate(NOW, 10)).thenReturn(List.of(monitor));

        List<Long> ids = new MonitorClaimService(monitorRepository, clock).claimDue(10);

        assertThat(ids).hasSize(1);
        assertThat(monitor.getNextRunAt()).isEqualTo(Instant.parse("2026-10-16T10:01:00Z"));
        verify(monitorRepository).saveAll(List.of(monitor));
    }

    @Test
    @DisplayName("claim: koi monitor due nahi toh khaali list")
    void claimDue_nothingDue() {
        when(monitorRepository.findDueForUpdate(NOW, 10)).thenReturn(List.of());

        assertThat(new MonitorClaimService(monitorRepository, clock).claimDue(10)).isEmpty();
    }
}
