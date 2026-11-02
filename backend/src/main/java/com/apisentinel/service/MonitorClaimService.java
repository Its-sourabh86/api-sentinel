package com.apisentinel.service;
import com.apisentinel.entity.Monitor;
import com.apisentinel.repository.MonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class MonitorClaimService {

    private final MonitorRepository monitorRepository;
    private final Clock clock;

    public MonitorClaimService(MonitorRepository monitorRepository, Clock clock) {
        this.monitorRepository = monitorRepository;
        this.clock = clock;
    }

    /** Due monitors ko lock karke unka next_run_at aage badhata hai, aur ids lautata hai. */
    @Transactional
    public List<Long> claimDue(int limit) {
        Instant now = Instant.now(clock);
        List<Monitor> due = monitorRepository.findDueForUpdate(now, limit);

        for (Monitor monitor : due) {
            monitor.setNextRunAt(now.plusSeconds(monitor.getIntervalSeconds()));
        }
        monitorRepository.saveAll(due);

        return due.stream().map(Monitor::getId).toList();
    }
}
