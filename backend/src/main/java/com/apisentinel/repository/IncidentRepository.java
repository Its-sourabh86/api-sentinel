package com.apisentinel.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.apisentinel.entity.Incident;
import com.apisentinel.enums.IncidentStatus;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByMonitorIdAndStatus(Long monitorId, IncidentStatus status);

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findByMonitorId(Long monitorId, Pageable pageable);

    Page<Incident> findByStatusAndMonitorId(IncidentStatus status, Long monitorId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Incident i SET i.lastNotifiedAt = :at WHERE i.id = :id")
    int markNotified(@Param("id") Long id, @Param("at") Instant at);

    @Query("""
            SELECT i FROM Incident i
            WHERE i.monitorId IN (SELECT m.id FROM Monitor m WHERE m.ownerId = :ownerId)
            """)
    Page<Incident> findForOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("""
            SELECT i FROM Incident i
            WHERE i.status = :status
              AND i.monitorId IN (SELECT m.id FROM Monitor m WHERE m.ownerId = :ownerId)
            """)
    Page<Incident> findForOwnerAndStatus(@Param("ownerId") Long ownerId,
            @Param("status") IncidentStatus status,
            Pageable pageable);
}
