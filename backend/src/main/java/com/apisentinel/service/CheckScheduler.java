package com.apisentinel.service;

import com.apisentinel.entity.Monitor;

import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.service.ProbeService;
import com.apisentinel.enums.TriggerType;
import com.apisentinel.config.SchedulerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@ConditionalOnProperty(name = "sentinel.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class CheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(CheckScheduler.class);

    private final MonitorClaimService claimService;
    private final ProbeService probeService;
    private final ThreadPoolExecutor workerPool;
    private final SchedulerProperties props;

    // Jin monitors ka check abhi chal raha hai (overlap rokne ke liye)
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public CheckScheduler(MonitorClaimService claimService,
                          ProbeService probeService,
                          ThreadPoolExecutor checkWorkerPool,
                          SchedulerProperties props) {
        this.claimService = claimService;
        this.probeService = probeService;
        this.workerPool = checkWorkerPool;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${sentinel.scheduler.tick-ms:5000}")
    public void tick() {
        try {
            // Backpressure: utna hi claim karo jitni queue mein jagah hai
            int capacity = workerPool.getQueue().remainingCapacity();
            if (capacity == 0) {
                log.warn("Worker queue is full, skipping this tick");
                return;
            }

            List<Long> ids = claimService.claimDue(Math.min(props.batchSize(), capacity));
            if (ids.isEmpty()) {
                return;
            }
            log.debug("Claimed {} due monitors", ids.size());
            ids.forEach(this::dispatch);
        } catch (Exception e) {
            log.error("Scheduler tick failed", e);
        }
    }

    private void dispatch(Long monitorId) {
        if (!inFlight.add(monitorId)) {
            log.warn("Skipping check, previous run still in progress monitorId={}", monitorId);
            return;
        }
        try {
            workerPool.execute(() -> runCheck(monitorId));
        } catch (RejectedExecutionException e) {
            inFlight.remove(monitorId);
            log.warn("Check rejected, worker pool saturated monitorId={}", monitorId);
        }
    }

    private void runCheck(Long monitorId) {
        try {
            probeService.execute(monitorId, TriggerType.SCHEDULED);
        } catch (ResourceNotFoundException e) {
            log.info("Monitor deleted before its check ran monitorId={}", monitorId);
        } catch (Exception e) {
            log.error("Scheduled check crashed monitorId={}", monitorId, e);
        } finally {
            inFlight.remove(monitorId);
        }
    }
}
