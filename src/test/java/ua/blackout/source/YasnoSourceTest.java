package ua.blackout.source;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Only the cache rule; no network.
class YasnoSourceTest {

    private static final Instant LAST = Instant.parse("2026-11-18T12:00:00Z");

    @Test
    void dueWhenNothingWasFetched() {
        assertTrue(YasnoSource.isFetchDue(null, LAST));
    }

    @Test
    void notDueInsideFiveMinutes() {
        assertFalse(YasnoSource.isFetchDue(LAST, LAST.plusSeconds(4 * 60 + 59)));
    }

    @Test
    void dueAfterFiveMinutes() {
        assertTrue(YasnoSource.isFetchDue(LAST, LAST.plusSeconds(5 * 60)));
    }
}
