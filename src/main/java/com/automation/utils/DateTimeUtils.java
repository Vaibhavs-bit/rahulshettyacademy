package com.automation.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralizes date/time formatting so execution IDs, folder names, and screenshot
 * filenames stay consistent across the framework instead of each class formatting
 * timestamps its own way.
 */
public final class DateTimeUtils {

    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter EXECUTION_ID_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HHmmssSSS");

    private DateTimeUtils() {
        // utility class - no instances
    }

    public static String getDateFolder() {
        return LocalDate.now().format(DATE_FOLDER_FORMAT);
    }

    public static String getExecutionTimestamp() {
        return LocalDateTime.now().format(EXECUTION_ID_FORMAT);
    }

    /** Millisecond-precision timestamp used in screenshot filenames to avoid collisions in parallel runs. */
    public static String getFileTimestamp() {
        return LocalDateTime.now().format(FILE_TIMESTAMP_FORMAT);
    }
}
