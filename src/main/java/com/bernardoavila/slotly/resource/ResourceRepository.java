package com.bernardoavila.slotly.resource;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdForUpdate(@Param("id") Long id);
}


