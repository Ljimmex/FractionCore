package pl.Ljimmex.fractionCore.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeUtilTest {

    @Test
    void currentEpochSecondsIsDividedMillis() {
        long before = System.currentTimeMillis() / 1000;
        long actual = TimeUtil.currentEpochSeconds();
        long after = System.currentTimeMillis() / 1000;

        assertTrue(actual >= before && actual <= after,
                "currentEpochSeconds() should equal System.currentTimeMillis() / 1000");
    }

    @Test
    void currentEpochMillisMatchesSystem() {
        long before = System.currentTimeMillis();
        long actual = TimeUtil.currentEpochMillis();
        long after = System.currentTimeMillis();

        assertTrue(actual >= before && actual <= after,
                "currentEpochMillis() should equal System.currentTimeMillis()");
    }

    @Test
    void secondsAreLessThanMillis() {
        assertEquals(TimeUtil.currentEpochMillis() / 1000, TimeUtil.currentEpochSeconds());
    }

    @Test
    void currentEpochSecondsAndMillisAreConsistent() {
        long seconds = TimeUtil.currentEpochSeconds();
        long millis = TimeUtil.currentEpochMillis();

        assertTrue(millis >= seconds * 1000L && millis <= seconds * 1000L + 999L,
                "millis must fall within the second returned by currentEpochSeconds()");
    }

    @Test
    void currentEpochSecondsIsNonNegative() {
        assertTrue(TimeUtil.currentEpochSeconds() >= 0,
                "epoch seconds should never be negative");
    }

    @Test
    void currentEpochMillisIsNonNegative() {
        assertTrue(TimeUtil.currentEpochMillis() >= 0,
                "epoch millis should never be negative");
    }

    @Test
    void repeatedCallsAreNonDecreasing() {
        long firstSeconds = TimeUtil.currentEpochSeconds();
        long secondSeconds = TimeUtil.currentEpochSeconds();

        assertTrue(secondSeconds >= firstSeconds,
                "successive calls to currentEpochSeconds() must not go backwards");
    }
}
