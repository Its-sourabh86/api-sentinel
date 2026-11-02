package com.apisentinel.service;

import com.apisentinel.dto.response.LastCheckResponse;
import com.apisentinel.dto.response.MonitorListItemResponse;
import com.apisentinel.dto.response.MonitorResponse;
import com.apisentinel.entity.CheckResult;
import com.apisentinel.repository.CheckResultRepository;
import com.apisentinel.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MonitorOverviewService {

    private final MonitorService monitorService;
    private final CheckResultRepository checkResultRepository;

    public MonitorOverviewService(MonitorService monitorService,
                                  CheckResultRepository checkResultRepository) {
        this.monitorService = monitorService;
        this.checkResultRepository = checkResultRepository;
    }

    @Transactional(readOnly = true)
    public List<MonitorListItemResponse> list(AuthenticatedUser user) {
        List<MonitorResponse> monitors = monitorService.getAll(user);
        if (monitors.isEmpty()) {
            return List.of(); // khaali IN () list SQL error deti hai
        }

        List<Long> ids = monitors.stream().map(MonitorResponse::id).toList();
        Map<Long, CheckResult> latest = checkResultRepository.findLatestForMonitors(ids).stream()
                .collect(Collectors.toMap(CheckResult::getMonitorId, Function.identity()));

        return monitors.stream()
                .map(m -> new MonitorListItemResponse(m,
                        latest.containsKey(m.id()) ? LastCheckResponse.from(latest.get(m.id())) : null))
                .toList();
    }
}