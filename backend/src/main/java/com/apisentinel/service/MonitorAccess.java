package com.apisentinel.service;

import com.apisentinel.entity.Monitor;
import com.apisentinel.enums.Role;
import com.apisentinel.exception.ResourceNotFoundException;
import com.apisentinel.repository.MonitorRepository;
import com.apisentinel.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MonitorAccess {

    private static final Logger log = LoggerFactory.getLogger(MonitorAccess.class);

    private final MonitorRepository monitorRepository;

    public MonitorAccess(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    /** Monitor nahi hai YA tera nahi hai: dono par ek jaisa 404 (existence leak nahi hota). */
    public void assertAccessible(Long monitorId, AuthenticatedUser user) {
        Monitor monitor = monitorRepository.getByIdOrThrow(monitorId);
        if (!canAccess(monitor, user)) {
            log.warn("Access denied userId={} monitorId={}", user.id(), monitorId);
            throw new ResourceNotFoundException("Monitor", monitorId);
        }
    }

    public static boolean canAccess(Monitor monitor, AuthenticatedUser user) {
        if (user.role() == Role.ADMIN) {
            return true;
        }
        return monitor.getOwnerId() != null && monitor.getOwnerId().equals(user.id());
    }
}