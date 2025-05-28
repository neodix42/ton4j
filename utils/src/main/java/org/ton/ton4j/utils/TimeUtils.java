package org.ton.ton4j.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for time-related operations
 */
@Slf4j
public final class TimeUtils {
    
    private TimeUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Convert a Unix timestamp to a UTC date-time string
     * @param timestamp The Unix timestamp
     * @return The UTC date-time string
     */
    public static String toUTC(long timestamp) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
    }
    
    /**
     * Convert a Unix timestamp to a UTC time-only string
     * @param timestamp The Unix timestamp
     * @return The UTC time-only string
     */
    public static String toUTCTimeOnly(long timestamp) {
        return DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
    }
    
    /**
     * Get the current Unix timestamp
     * @return The current Unix timestamp
     */
    public static long now() {
        return Instant.now().getEpochSecond();
    }
    
    /**
     * Sleep for a specified number of seconds
     * @param seconds The number of seconds to sleep
     */
    public static void sleep(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Throwable e) {
            log.info(e.getMessage());
        }
    }
    
    /**
     * Sleep for a specified number of milliseconds
     * @param milliseconds The number of milliseconds to sleep
     */
    public static void sleepMs(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (Throwable e) {
            log.info(e.getMessage());
        }
    }
    
    /**
     * Sleep for a specified number of seconds with a message
     * @param seconds The number of seconds to sleep
     * @param text The message to log
     */
    public static void sleep(long seconds, String text) {
        try {
            log.info(String.format("pause %s seconds, %s", seconds, text));
            TimeUnit.SECONDS.sleep(seconds);
        } catch (Throwable e) {
            log.info(e.getMessage());
        }
    }
    
    /**
     * Sleep for a specified number of milliseconds with a message
     * @param milliseconds The number of milliseconds to sleep
     * @param text The message to log
     */
    public static void sleepMs(long milliseconds, String text) {
        try {
            log.info(String.format("pause %s milliseconds, %s", milliseconds, text));
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (Throwable e) {
            log.info(e.getMessage());
        }
    }
}
