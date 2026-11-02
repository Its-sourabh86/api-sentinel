package com.apisentinel.repository;

import com.apisentinel.entity.CheckResult;
import com.apisentinel.enums.TriggerType;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    Page<CheckResult> findByMonitorId(Long monitorId, Pageable pageable);

    List<CheckResult> findByMonitorIdOrderByStartedAtDescIdDesc(Long monitorId, Pageable pageable);

    List<CheckResult> findByMonitorIdAndTriggerTypeOrderByStartedAtDescIdDesc(
            Long monitorId, TriggerType triggerType, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT ON (monitor_id) *
            FROM check_results
            WHERE monitor_id IN (:ids)
            ORDER BY monitor_id, started_at DESC, id DESC
            """, nativeQuery = true)
    List<CheckResult> findLatestForMonitors(@Param("ids") Collection<Long> ids);
}
