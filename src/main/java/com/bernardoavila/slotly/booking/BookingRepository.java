package com.bernardoavila.slotly.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
           SELECT COUNT(b) > 0 FROM Booking b
           WHERE b.resource.id = :resourceId
             AND b.startsAt < :endsAt
             AND :startsAt < b.endsAt
           """)
    boolean existsOverlapping(@Param("resourceId") Long resourceId,
                              @Param("startsAt") Instant startsAt,
                              @Param("endsAt") Instant endsAt);
}