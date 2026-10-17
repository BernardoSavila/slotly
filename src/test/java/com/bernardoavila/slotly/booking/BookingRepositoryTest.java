package com.bernardoavila.slotly.booking;

import com.bernardoavila.slotly.PostgresTestBase;
import com.bernardoavila.slotly.resource.Resource;
import com.bernardoavila.slotly.resource.ResourceRepository;
import com.bernardoavila.slotly.user.User;
import com.bernardoavila.slotly.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BookingRepositoryTest extends PostgresTestBase {

    private static final Instant NINE = Instant.parse("2026-11-20T09:00:00Z");
    private static final Instant TEN = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant ELEVEN = Instant.parse("2026-11-20T11:00:00Z");
    private static final Instant EIGHT = Instant.parse("2026-11-20T08:00:00Z");

    @Autowired private BookingRepository bookingRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private UserRepository userRepository;

    private Resource room;

    @BeforeEach
    void setUp() {
        room = resourceRepository.save(new Resource("Sala Azul", "Com projetor"));
        User user = userRepository.save(new User("teste@example.com", "Teste"));
        bookingRepository.save(new Booking(room, user, NINE, TEN));
    }

    @Test
    @DisplayName("detects an identical period")
    void detectsIdentical() {
        assertThat(bookingRepository.existsOverlapping(room.getId(), NINE, TEN)).isTrue();
    }

    @Test
    @DisplayName("detects a period starting in the middle")
    void detectsStartingInside() {
        assertThat(bookingRepository.existsOverlapping(room.getId(), TEN.minusSeconds(1800), ELEVEN)).isTrue();
    }

    @Test
    @DisplayName("detects a period that fully contains the existing one")
    void detectsEnclosing() {
        assertThat(bookingRepository.existsOverlapping(room.getId(), EIGHT, ELEVEN)).isTrue();
    }

    @Test
    @DisplayName("allows a period starting exactly when the other ends")
    void allowsBackToBackAfter() {
        assertThat(bookingRepository.existsOverlapping(room.getId(), TEN, ELEVEN)).isFalse();
    }

    @Test
    @DisplayName("allows a period ending exactly when the other starts")
    void allowsBackToBackBefore() {
        assertThat(bookingRepository.existsOverlapping(room.getId(), EIGHT, NINE)).isFalse();
    }

    @Test
    @DisplayName("ignores bookings of other resources")
    void ignoresOtherResources() {
        Resource other = resourceRepository.save(new Resource("Sala Verde", null));
        assertThat(bookingRepository.existsOverlapping(other.getId(), NINE, TEN)).isFalse();
    }
}