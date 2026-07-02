package com.openbake.server.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Mirrors backend/app/utils/timezone.py's to_ist/to_ist_iso — DB timestamps are stored as naive UTC. */
public final class IstTime {

    private static final ZoneOffset IST_OFFSET = ZoneOffset.ofHoursMinutes(5, 30);

    private IstTime() {
    }

    public static String toIso(LocalDateTime utcNaive) {
        if (utcNaive == null) {
            return null;
        }
        OffsetDateTime ist = utcNaive.atOffset(ZoneOffset.UTC).withOffsetSameInstant(IST_OFFSET);
        return ist.toString();
    }

    public static String nowIso() {
        return toIso(LocalDateTime.now(ZoneOffset.UTC));
    }
}
