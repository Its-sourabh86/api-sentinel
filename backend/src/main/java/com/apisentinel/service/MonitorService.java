package com.apisentinel.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.apisentinel.exception.InvalidRequestException;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.controller.MockWebhookReceiverController;
import com.apisentinel.dto.request.CreateMonitorRequest;
import com.apisentinel.dto.request.UpdateMonitorRequest;
import com.apisentinel.dto.response.MonitorResponse;
import com.apisentinel.entity.Monitor;
import com.apisentinel.enums.HttpMethod;
import com.apisentinel.enums.Role;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.security.AuthenticatedUser;
import com.apisentinel.security.UnsafeUrlException;
import com.apisentinel.security.UrlSafetyValidator;

import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private final MonitorRepository monitorRepository;
    private final UrlSafetyValidator urlSafetyValidator;
    private final IncidentService incidentService;

    public MonitorService(MonitorRepository monitorRepository, UrlSafetyValidator urlSafetyValidator,
            IncidentService incidentService) {
        this.monitorRepository = monitorRepository;
        this.urlSafetyValidator = urlSafetyValidator;
        this.incidentService = incidentService;
    }

    @Transactional
    public MonitorResponse create(CreateMonitorRequest request, Long ownerId) {
        assertUrlAllowed(request.url());
        HttpMethod method = request.method() != null ? request.method() : HttpMethod.GET;
        Integer expectedStatus = request.expectedStatus() != null ? request.expectedStatus() : 200;

        Monitor monitor = new Monitor(
                request.name(),
                request.url(),
                method,
                request.intervalSeconds(),
                request.timeoutMs(),
                expectedStatus

        );
        monitor.assignOwner(ownerId);
        Monitor saved = monitorRepository.save(monitor);
        return MonitorResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public MonitorResponse getById(Long id) {
        Monitor monitor = monitorRepository.getByIdOrThrow(id);
        return MonitorResponse.from(monitor);
    }

    @Transactional(readOnly = true)
    public List<MonitorResponse> getAll(AuthenticatedUser user) {
        List<Monitor> monitors = user.role() == Role.ADMIN
                ? monitorRepository.findAll()
                : monitorRepository.findByOwnerId(user.id());
        return monitors.stream().map(MonitorResponse::from).toList();
    }

    @Transactional
    public MonitorResponse update(Long id, UpdateMonitorRequest request) {
        Monitor monitor = monitorRepository.getByIdOrThrow(id);
        if (request.url() != null) {
            assertUrlAllowed(request.url());
        }
        if (request.name() != null)
            monitor.setName(request.name());
        if (request.url() != null)
            monitor.setUrl(request.url());
        if (request.method() != null)
            monitor.setHttpMethod(request.method());
        if (request.intervalSeconds() != null) {
            monitor.setIntervalSeconds(request.intervalSeconds());
            monitor.rescheduleNow(); // 1 ghante se 30s kiya toh ghante bhar wait na karna pade
        }
        if (request.timeoutMs() != null)
            monitor.setTimeoutMs(request.timeoutMs());
        if (request.expectedStatus() != null)
            monitor.setExpectedStatus(request.expectedStatus());

        Monitor saved = monitorRepository.save(monitor);
        return MonitorResponse.from(saved);
    }

    @Transactional
    public MonitorResponse pause(Long id) {
        Monitor monitor = monitorRepository.getByIdOrThrow(id);
        monitor.setEnabled(false);
        Monitor saved = monitorRepository.save(monitor);
        incidentService.resolveOnPause(id);
        log.info("Monitor paused id={}", id);
        return MonitorResponse.from(saved);
    }

    @Transactional
    public MonitorResponse resume(Long id) {
        Monitor monitor = monitorRepository.getByIdOrThrow(id);
        monitor.setEnabled(true);
        monitor.rescheduleNow(); // pause ke dauran next_run_at purana ho chuka hai
        Monitor saved = monitorRepository.save(monitor);
        return MonitorResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        Monitor monitor = monitorRepository.getByIdOrThrow(id);
        monitorRepository.delete(monitor);
    }

    private void assertUrlAllowed(String url) {
        try {
            urlSafetyValidator.validate(url);
        } catch (UnsafeUrlException e) {
            if (e.reason() == UnsafeUrlException.Reason.UNRESOLVABLE) {
                // Service abhi deploy na hui ho sakti hai. Probe time par dobara check hoga.
                log.info("Monitor URL host does not resolve right now, saving anyway");
                return;
            }
            throw new InvalidRequestException(e.getMessage());
        }
    }
}
