package pl.Ljimmex.fractionCore.util;

/**
 * Utility class for time-related operations.
 * Replaces scattered {@code System.currentTimeMillis() / 1000} calls
 * with a single, testable source of epoch seconds.
 */
public final class TimeUtil {

    private TimeUtil() {
        throw new AssertionError("Utility class");
    }

    /**
     * @return current Unix epoch time in seconds
     */
    public static long currentEpochSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * @return current Unix epoch time in milliseconds
     */
    public static long currentEpochMillis() {
        return System.currentTimeMillis();
    }
}
