package com.apisentinel.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.apisentinel.config.IncidentProperties;
import com.apisentinel.entity.CheckResult;
import com.apisentinel.entity.Incident;
import com.apisentinel.entity.Monitor;
import com.apisentinel.enums.CheckStatus;
import com.apisentinel.enums.FailureCode;
import com.apisentinel.enums.HttpMethod;
import com.apisentinel.enums.IncidentStatus;
import com.apisentinel.enums.ProbeOutcome;
import com.apisentinel.enums.Role;
import com.apisentinel.enums.TriggerType;
import com.apisentinel.repository.CheckResultRepository;
import com.apisentinel.repository.IncidentRepository;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.security.AuthenticatedUser;
import com.apisentinel.utils.IncidentEvent;
import com.apisentinel.utils.ValidationReport;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

        private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");

        @Mock
        private MonitorRepository monitorRepository;
        @Mock
        private IncidentRepository incidentRepository;
        @Mock
        private CheckResultRepository checkResultRepository;
        @Mock
        private ApplicationEventPublisher eventPublisher;

        private IncidentService service() {
                return new IncidentService(monitorRepository, incidentRepository, checkResultRepository,
                                new IncidentProperties(3, 2), Clock.fixed(NOW, ZoneOffset.UTC), eventPublisher);
        }

        private CheckResult result(CheckStatus status) {
                return new CheckResult(1L, NOW, 10, 200, status,
                                status == CheckStatus.FAILED ? FailureCode.JSON_ASSERTION_FAILED : null,
                                null, ProbeOutcome.RESPONSE_RECEIVED, TriggerType.SCHEDULED);
        }

        private ValidationReport failedReport() {
                return ValidationReport.failed(FailureCode.JSON_ASSERTION_FAILED, "stock 0", List.of());
        }

        private void monitorExists() {
                when(monitorRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(
                                new Monitor("A", "https://example.com", HttpMethod.GET, 60, 3000, 200)));
        }

        @Test
        @DisplayName("3 consecutive failures par incident open")
        void opensAfterThreshold() {
                monitorExists();
                when(incidentRepository.findByMonitorIdAndStatus(1L, IncidentStatus.OPEN))
                                .thenReturn(Optional.empty());
                when(checkResultRepository.findByMonitorIdAndTriggerTypeOrderByStartedAtDescIdDesc(any(), any(), any()))
                                .thenReturn(List.of(result(CheckStatus.FAILED), result(CheckStatus.FAILED),
                                                result(CheckStatus.FAILED)));

                service().evaluate(1L, failedReport());

                verify(incidentRepository).save(any(Incident.class));
        }

        @Test
        @DisplayName("sirf 2 failures (threshold 3) par incident nahi")
        void doesNotOpenBelowThreshold() {
                monitorExists();
                when(incidentRepository.findByMonitorIdAndStatus(1L, IncidentStatus.OPEN))
                                .thenReturn(Optional.empty());
                when(checkResultRepository.findByMonitorIdAndTriggerTypeOrderByStartedAtDescIdDesc(any(), any(), any()))
                                .thenReturn(List.of(result(CheckStatus.FAILED), result(CheckStatus.FAILED),
                                                result(CheckStatus.PASSED)));

                service().evaluate(1L, failedReport());

                verify(incidentRepository, never()).save(any());
        }

        @Test
        @DisplayName("incident pehle se khula hai: failure par naya nahi, count badhe")
        void deduplicates() {
                monitorExists();
                Incident open = Incident.open(1L, FailureCode.JSON_ASSERTION_FAILED, "stock 0", 3, NOW);
                when(incidentRepository.findByMonitorIdAndStatus(1L, IncidentStatus.OPEN))
                                .thenReturn(Optional.of(open));

                service().evaluate(1L, failedReport());

                assertThat(open.getFailureCount()).isEqualTo(4);
                assertThat(open.getStatus()).isEqualTo(IncidentStatus.OPEN);
        }

        @Test
        @DisplayName("2 consecutive passes par incident resolve")
        void resolvesAfterRecovery() {
                monitorExists();
                Incident open = Incident.open(1L, FailureCode.JSON_ASSERTION_FAILED, "stock 0", 3, NOW);
                when(incidentRepository.findByMonitorIdAndStatus(1L, IncidentStatus.OPEN))
                                .thenReturn(Optional.of(open));
                when(checkResultRepository.findByMonitorIdAndTriggerTypeOrderByStartedAtDescIdDesc(any(), any(), any()))
                                .thenReturn(List.of(result(CheckStatus.PASSED), result(CheckStatus.PASSED)));

                service().evaluate(1L, ValidationReport.passed(List.of()));

                assertThat(open.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
                assertThat(open.getResolvedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("sirf 1 pass (threshold 2) par incident khula rahe")
        void staysOpenAfterSinglePass() {
                monitorExists();
                Incident open = Incident.open(1L, FailureCode.JSON_ASSERTION_FAILED, "stock 0", 3, NOW);
                when(incidentRepository.findByMonitorIdAndStatus(1L, IncidentStatus.OPEN))
                                .thenReturn(Optional.of(open));
                when(checkResultRepository.findByMonitorIdAndTriggerTypeOrderByStartedAtDescIdDesc(any(), any(), any()))
                                .thenReturn(List.of(result(CheckStatus.PASSED), result(CheckStatus.FAILED)));

                service().evaluate(1L, ValidationReport.passed(List.of()));

                assertThat(open.getStatus()).isEqualTo(IncidentStatus.OPEN);
        }

        @Test
        @DisplayName("pause par OPEN incident resolve ho jaye, event publish na ho")
        void resolvesOnPause() {
                Incident open = Incident.open(1L, FailureCode.JSON_ASSERTION_FAILED, "stock 0", 3, NOW);
                when(incidentRepository.findByMonitorIdAndStatus(1L, IncidentStatus.OPEN))
                                .thenReturn(Optional.of(open));

                service().resolveOnPause(1L);

                assertThat(open.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
                verify(eventPublisher, never()).publishEvent(any(IncidentEvent.class));
        }

        @Test
        @DisplayName("list: user ko apne, admin ko sab incidents")
        void list_scopedByRole() {
                when(incidentRepository.findForOwner(eq(42L), any(Pageable.class))).thenReturn(Page.empty());
                when(incidentRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

                service().list(new AuthenticatedUser(42L, Role.USER), null, null, 0, 20);
                verify(incidentRepository).findForOwner(eq(42L), any(Pageable.class));
                verify(incidentRepository, never()).findAll(any(Pageable.class));

                service().list(new AuthenticatedUser(1L, Role.ADMIN), null, null, 0, 20);
                verify(incidentRepository).findAll(any(Pageable.class));
        }
}
