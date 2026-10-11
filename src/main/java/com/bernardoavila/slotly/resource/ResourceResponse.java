package com.bernardoavila.slotly.resource;

import java.time.Instant;

public record ResourceResponse(
        Long id,
        String name,
        String description,
        Instant createdAt
){ }
