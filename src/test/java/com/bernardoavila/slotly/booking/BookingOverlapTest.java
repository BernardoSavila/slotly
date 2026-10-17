package com.bernardoavila.slotly.booking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingOverlapTest {

    private static final Instant NINE = Instant.parse("2026-11-20T09:00:00Z");
    private static final Instant TEN = Instant.parse("2026-11-20T10:00:00Z");

    @Test
    @DisplayName("rejects a booking that ends before it starts")
    void rejectsInvertedPeriod() {
        assertThatThrownBy(() -> new Booking(null, null, TEN, NINE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endsAt must be after startsAt");
    }

    @Test
    @DisplayName("rejects a booking with zero duration")
    void rejectsZeroDuration() {
        assertThatThrownBy(() -> new Booking(null, null, NINE, NINE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("accepts a valid period")
    void acceptsValidPeriod() {
        Booking booking = new Booking(null, null, NINE, TEN);
        assertThat(booking.getStartsAt()).isEqualTo(NINE);
        assertThat(booking.getEndsAt()).isEqualTo(TEN);
    }
}