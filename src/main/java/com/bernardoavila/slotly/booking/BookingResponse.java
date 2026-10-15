package com.bernardoavila.slotly.booking;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long resourceId,
        String resourceName,
        Long userId,
        String userName,
        Instant startsAt,
        Instant endsAt
) {}