package com.opensurvey.surveys;

import java.time.Instant;

public record Survey(
        String source,
        String id,
        String title,
        Status status,
        Instant updatedAt,
        int rewardMin,
        int rewardMax,
        int estimatedMinutes
) {}
