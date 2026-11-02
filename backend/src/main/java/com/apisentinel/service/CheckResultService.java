package com.apisentinel.service;
import com.apisentinel.dto.response.PageResponse;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.utils.ProbeResult;
import com.apisentinel.dto.response.CheckResultResponse;
import com.apisentinel.entity.CheckResult;
import com.apisentinel.enums.TriggerType;
import com.apisentinel.repository.CheckResultRepository;
import com.apisentinel.utils.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CheckResultService {

    private static final Logger log = LoggerFactory.getLogger(CheckResultService.class);

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_REASON_LENGTH = 500;

    private final CheckResultRepository checkResultRepository;
    private final MonitorRepository monitorRepository;

    public CheckResultService(CheckResultRepository checkResultRepository,
                              MonitorRepository monitorRepository) {
        this.checkResultRepository = checkResultRepository;
        this.monitorRepository = monitorRepository;
    }

    @Transactional
    public CheckResult record(Long monitorId, Instant startedAt, ProbeResult probe,
                              ValidationReport report, TriggerType trigger) {
        CheckResult entity = new CheckResult(
                monitorId,
                startedAt,
                probe.durationMs(),
                probe.httpStatus(),
                report.status(),
                report.failureCode(),
                truncate(report.reason()),
                probe.outcome(),
                trigger);

        CheckResult saved = checkResultRepository.save(entity);
        log.debug("Check result saved resultId={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<CheckResultResponse> list(Long monitorId, int page, int size) {
        monitorRepository.getByIdOrThrow(monitorId); // monitor na ho toh 404

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        PageRequest pageRequest = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "startedAt"));

        return PageResponse.from(
                checkResultRepository.findByMonitorId(monitorId, pageRequest),
                CheckResultResponse::from);
    }

    private String truncate(String reason) {
        if (reason == null) return null;
        return reason.length() <= MAX_REASON_LENGTH
                ? reason
                : reason.substring(0, MAX_REASON_LENGTH - 3) + "...";
    }
}
