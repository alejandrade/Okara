package io.shrouded.okara.util;

import com.google.cloud.Timestamp;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class TimestampUtils {

    private TimestampUtils() {
        // Utility class - private constructor
    }

    /**
     * Convert Timestamp to Instant
     */
    public static Instant toInstant(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toDate().toInstant();
    }

    /**
     * Get current time as Instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Calculate hours between a Timestamp and now
     */
    public static long hoursUntilNow(Timestamp timestamp) {
        if (timestamp == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.HOURS.between(toInstant(timestamp), now());
    }

    /**
     * Check if a Timestamp is within the last N hours
     */
    public static boolean isRecent(Timestamp timestamp, int hours) {
        if (timestamp == null) {
            return false;
        }
        return hoursUntilNow(timestamp) <= hours;
    }

    /**
     * Get the minimum (earliest) timestamp from an array
     */
    public static Timestamp min(Timestamp... timestamps) {
        Timestamp min = null;
        for (Timestamp timestamp : timestamps) {
            if (timestamp != null && (min == null || timestamp.compareTo(min) < 0)) {
                min = timestamp;
            }
        }
        return min != null ? min : Timestamp.MAX_VALUE;
    }
}