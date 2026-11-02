package com.apisentinel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apisentinel.config.IncidentProperties;
import com.apisentinel.dto.response.IncidentResponse;
import com.apisentinel.dto.response.PageResponse;
import com.apisentinel.entity.CheckResult;
import com.apisentinel.entity.Incident;
import com.apisentinel.entity.Monitor;
import com.apisentinel.enums.CheckStatus;
import com.apisentinel.enums.IncidentStatus;
import com.apisentinel.enums.Role;
import com.apisentinel.enums.TriggerType;
import com.apisentinel.repository.CheckResultRepository;
import com.apisentinel.repository.IncidentRepository;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.security.AuthenticatedUser;
import com.apisentinel.utils.IncidentEvent;
import com.apisentinel.utils.ValidationReport;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_REASON_LENGTH = 500;

    private final MonitorRepository monitorRepository;
    private final IncidentRepository incidentRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentProperties props;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public IncidentService(MonitorRepository monitorRepository,
            IncidentRepository incidentRepository,
            CheckResultRepository checkResultRepository,
            IncidentProperties props,
            Clock clock,
            ApplicationEventPublisher eventPublisher) {
        this.monitorRepository = monitorRepository;
        this.incidentRepository = incidentRepository;
        this.checkResultRepository = checkResultRepository;
        this.props = props;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Check result save hone ke BAAD call hota hai. Incident open, update ya
     * resolve karta hai.
     */
    @Transactional
    public void evaluate(Long monitorId, ValidationReport report) {
        // Row lock: ek monitor ka evaluation ek time par ek hi thread karega
        Optional<Monitor> locked = monitorRepository.findByIdForUpdate(monitorId);
        if (locked.isEmpty()) {
            return; // monitor beech mein delete ho gaya
        }

        if (!locked.get().isEnabled()) {
            return; // monitor paused hai, naya incident nahi
        }
        String monitorName = locked.get().getName();

        Instant now = Instant.now(clock);
        boolean failed = report.status() == CheckStatus.FAILED;
        Optional<Incident> open = incidentRepository.findByMonitorIdAndStatus(monitorId, IncidentStatus.OPEN);

        if (open.isPresent()) {
            Incident incident = open.get();
            if (failed) {
                incident.recordFailure(report.failureCode(), truncate(report.reason()), now);
                log.debug("Incident updated incidentId={} failureCount={}",
                        incident.getId(), incident.getFailureCount());
            } else if (lastResultsAll(monitorId, props.recoveryThreshold(), CheckStatus.PASSED)) {
                incident.resolve(now);
                log.info("Incident RESOLVED incidentId={} failureCount={}",
                        incident.getId(), incident.getFailureCount());
                eventPublisher.publishEvent(
                        IncidentEvent.of(IncidentEvent.Type.RESOLVED, incident, monitorName));
            }
            incidentRepository.save(incident);
            return;
        }

        if (failed && lastResultsAll(monitorId, props.failureThreshold(), CheckStatus.FAILED)) {
            Incident incident = Incident.open(monitorId, report.failureCode(),
                    truncate(report.reason()), props.failureThreshold(), now);
            incidentRepository.save(incident);
            log.warn("Incident OPENED incidentId={} code={} reason={}",
                    incident.getId(), incident.getFailureCode(), incident.getReason());
            eventPublisher.publishEvent(
                    IncidentEvent.of(IncidentEvent.Type.OPENED, incident, monitorName));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> list(AuthenticatedUser user, IncidentStatus status,
            Long monitorId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "openedAt"));

        Page<Incident> result;
        if (monitorId != null) {
            // Controller pehle hi ownership check kar chuka hai
            result = status != null
                    ? incidentRepository.findByStatusAndMonitorId(status, monitorId, pageable)
                    : incidentRepository.findByMonitorId(monitorId, pageable);
        } else if (user.role() == Role.ADMIN) {
            result = status != null
                    ? incidentRepository.findByStatus(status, pageable)
                    : incidentRepository.findAll(pageable);
        } else {
            result = status != null
                    ? incidentRepository.findForOwnerAndStatus(user.id(), status, pageable)
                    : incidentRepository.findForOwner(user.id(), pageable);
        }
        return PageResponse.from(result, IncidentResponse::from);
    }

    /**
     * Last n results sabke sab expected status ke hain? n se kam results hain toh
     * false.
     */
    private boolean lastResultsAll(Long monitorId, int n, CheckStatus expected) {
        List<CheckResult> recent = checkResultRepository
                .findByMonitorIdAndTriggerTypeOrderByStartedAtDescIdDesc(
                        monitorId, TriggerType.SCHEDULED, PageRequest.of(0, n));
        return recent.size() == n
                && recent.stream().allMatch(r -> r.getResultStatus() == expected);
    }

    private String truncate(String reason) {
        if (reason == null)
            return null;
        return reason.length() <= MAX_REASON_LENGTH
                ? reason
                : reason.substring(0, MAX_REASON_LENGTH - 3) + "...";
    }

    @Transactional
    public void resolveOnPause(Long monitorId) {
        incidentRepository.findByMonitorIdAndStatus(monitorId, IncidentStatus.OPEN)
                .ifPresent(incident -> {
                    incident.resolve(Instant.now(clock));
                    incidentRepository.save(incident);
                    log.info("Incident closed because monitor was paused incidentId={}",
                            incident.getId());
                });
    }
}