// booking/BookingRequest.java
package com.bernardoavila.slotly.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record BookingRequest(
        @NotNull(message = "resourceId is required") Long resourceId,
        @NotNull(message = "userId is required") Long userId,
        @NotNull(message = "startsAt is required") @Future Instant startsAt,
        @NotNull(message = "endsAt is required") @Future Instant endsAt
) {}