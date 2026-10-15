package com.bernardoavila.slotly.booking;

import java.time.Instant;

public class OverlappingBookingException extends RuntimeException {

    public OverlappingBookingException(Long resourceId, Instant startsAt, Instant endsAt) {
        super("Resource " + resourceId + " is already booked between " + startsAt + " and " + endsAt);
    }
}