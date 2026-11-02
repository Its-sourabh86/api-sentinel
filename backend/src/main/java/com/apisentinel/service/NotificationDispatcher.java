package com.apisentinel.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.apisentinel.config.NotificationProperties;
import com.apisentinel.repository.IncidentRepository;
import com.apisentinel.utils.IncidentEvent;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationProperties props;
    private final WebhookSender webhookSender;
    private final IncidentRepository incidentRepository;
    private final Clock clock;

    private final AtomicInteger threadCounter = new AtomicInteger(1);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 2, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(100),
            runnable -> new Thread(runnable, "notify-worker-" + threadCounter.getAndIncrement()),
            new ThreadPoolExecutor.AbortPolicy());

    public NotificationDispatcher(NotificationProperties props,
                                  WebhookSender webhookSender,
                                  IncidentRepository incidentRepository,
                                  Clock clock) {
        this.props = props;
        this.webhookSender = webhookSender;
        this.incidentRepository = incidentRepository;
        this.clock = clock;
    }

    /** Incident ki transaction commit hone ke BAAD chalta hai. Rollback hua toh chalta hi nahi. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIncidentEvent(IncidentEvent event) {
        if (!props.enabled()) {
            log.debug("Notifications disabled, skipping incidentId={}", event.incidentId());
            return;
        }
        try {
            executor.execute(() -> deliver(event));
        } catch (RejectedExecutionException e) {
            log.warn("Notification dropped, queue full type={} incidentId={}",
                    event.type(), event.incidentId());
        }
    }

    private void deliver(IncidentEvent event) {
        MDC.put("monitorId", String.valueOf(event.monitorId()));
        try {
            if (webhookSender.send(event)) {
                incidentRepository.markNotified(event.incidentId(), Instant.now(clock));
            }
        } catch (Exception e) {
            log.error("Notification delivery crashed incidentId={}", event.incidentId(), e);
        } finally {
            MDC.remove("monitorId");
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
