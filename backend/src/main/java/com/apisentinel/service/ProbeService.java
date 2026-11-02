package com.apisentinel.service;

import com.apisentinel.dto.request.ProbeRequest;
import com.apisentinel.dto.response.ProbeRunResponse;
import com.apisentinel.utils.HttpProbeExecutor;
import com.apisentinel.utils.ProbeResult;

import com.apisentinel.entity.Monitor;
import com.apisentinel.repository.MonitorAssertionRepository;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.enums.TriggerType;
import com.apisentinel.service.CheckResultService;
import com.apisentinel.utils.AssertionSpec;
import com.apisentinel.enums.CheckStatus;
import com.apisentinel.utils.ResponseValidator;
import com.apisentinel.utils.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProbeService {

    private static final Logger log = LoggerFactory.getLogger(ProbeService.class);

    private final MonitorRepository monitorRepository;
    private final MonitorAssertionRepository assertionRepository;
    private final HttpProbeExecutor probeExecutor;
    private final ResponseValidator responseValidator;
    private final CheckResultService checkResultService;
    private final IncidentService incidentService;

    public ProbeService(MonitorRepository monitorRepository,
            MonitorAssertionRepository assertionRepository,
            HttpProbeExecutor probeExecutor,
            ResponseValidator responseValidator,
            CheckResultService checkResultService,
            IncidentService incidentService) {
        this.monitorRepository = monitorRepository;
        this.assertionRepository = assertionRepository;
        this.probeExecutor = probeExecutor;
        this.responseValidator = responseValidator;
        this.checkResultService = checkResultService;
        this.incidentService = incidentService;
    }

    public ProbeRunResponse runNow(Long monitorId) {
        return execute(monitorId, TriggerType.MANUAL);
    }

    // Jaan-boojh kar @Transactional nahi: HTTP call ke dauran DB connection khula
    // nahi rakhna
    public ProbeRunResponse execute(Long monitorId, TriggerType trigger) {
        MDC.put("monitorId", String.valueOf(monitorId));
        try {
            Monitor monitor = monitorRepository.getByIdOrThrow(monitorId);

            List<AssertionSpec> specs = assertionRepository.findByMonitorIdOrderByIdAsc(monitorId)
                    .stream()
                    .map(a -> new AssertionSpec(a.getJsonPath(), a.getOperator(), a.getExpectedValue()))
                    .toList();

            log.debug("Starting check trigger={} assertions={}", trigger, specs.size());

            Instant startedAt = Instant.now();
            ProbeResult result = probeExecutor.execute(new ProbeRequest(
                    monitor.getUrl(), monitor.getHttpMethod(), monitor.getTimeoutMs()));

            ValidationReport report = responseValidator.validate(result, monitor.getExpectedStatus(),
                    monitor.getResponseSchema(), specs);

            checkResultService.record(monitorId, startedAt, result, report, trigger);

            if (trigger == TriggerType.SCHEDULED) {
                try {
                    incidentService.evaluate(monitorId, report);
                } catch (Exception e) {
                    log.error("Incident evaluation failed", e);
                }
            }

            if (report.status() == CheckStatus.PASSED) {
                log.info("Check PASSED httpStatus={} durationMs={} trigger={}",
                        result.httpStatus(), result.durationMs(), trigger);
            } else {
                log.warn("Check FAILED code={} httpStatus={} durationMs={} trigger={} reason={}",
                        report.failureCode(), result.httpStatus(), result.durationMs(),
                        trigger, report.reason());
            }

            return new ProbeRunResponse(monitor.getId(), report.status(), report.failureCode(),
                    report.reason(), result.outcome(), result.httpStatus(),
                    result.durationMs(), report.assertions());
        } finally {
            MDC.remove("monitorId");
        }
    }
}
