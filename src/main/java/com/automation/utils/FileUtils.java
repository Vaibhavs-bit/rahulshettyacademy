package com.automation.utils;

import com.automation.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Thin wrapper around java.nio.file for the handful of filesystem operations the
 * framework needs (creating output directories). Using NIO instead of java.io keeps
 * the framework dependency-free for simple file/directory management.
 */
public final class FileUtils {

    private static final Logger logger = FrameworkLogger.getLogger(FileUtils.class);

    private FileUtils() {
        // utility class - no instances
    }

    public static Path createDirectories(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.debug("Created directory: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create directory: {}", path, e);
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
        return path;
    }

    public static void writeTextFile(Path path, String content) {
        try {
            createDirectories(path.getParent());
            Files.writeString(path, content);
            logger.info("Wrote file: {}", path.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write file: {}", path, e);
        }
    }
}
