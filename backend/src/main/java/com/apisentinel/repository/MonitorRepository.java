package com.apisentinel.repository;

import com.apisentinel.exception.ResourceNotFoundException;

import jakarta.persistence.LockModeType;

import com.apisentinel.entity.Monitor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

        default Monitor getByIdOrThrow(Long id) {
                return findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Monitor", id));
        }

        @Query(value = """
                        SELECT * FROM monitors
                        WHERE enabled = TRUE AND next_run_at <= :now
                        ORDER BY next_run_at
                        LIMIT :limit
                        FOR UPDATE SKIP LOCKED
                        """, nativeQuery = true)
        List<Monitor> findDueForUpdate(@Param("now") Instant now, @Param("limit") int limit);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT m FROM Monitor m WHERE m.id = :id")
        Optional<Monitor> findByIdForUpdate(@Param("id") Long id);

        List<Monitor> findByOwnerId(Long ownerId);
}
