package com.apisentinel.repository;
import com.apisentinel.entity.MonitorAssertion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorAssertionRepository extends JpaRepository<MonitorAssertion, Long> {

    List<MonitorAssertion> findByMonitorIdOrderByIdAsc(Long monitorId);

    Optional<MonitorAssertion> findByIdAndMonitorId(Long id, Long monitorId);
}
