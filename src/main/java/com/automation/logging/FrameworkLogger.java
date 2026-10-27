package com.automation.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Single entry point for obtaining a Log4j2 {@link Logger} across the framework.
 *
 * Why this class exists: centralizing logger creation keeps logging configuration
 * (pattern, levels, appenders) consistent and makes it trivial to swap the underlying
 * logging implementation later without touching every class that logs.
 */
public final class FrameworkLogger {

    private FrameworkLogger() {
        // utility class - no instances
    }

    public static Logger getLogger(Class<?> clazz) {
        return LogManager.getLogger(clazz);
    }
}
