package com.automation.utils;

import com.automation.config.ConfigReader;
import com.automation.core.ExecutionManager;
import com.automation.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Captures and organizes screenshot evidence for passed, failed, and skipped tests.
 *
 * Why this class exists: evidence capture needs to be independent of test logic so it
 * can be triggered uniformly from {@code TestListener} regardless of how/why a test
 * ended, and so a screenshot failure never masks or replaces the real test result.
 */
public final class ScreenshotUtils {

    private static final Logger logger = FrameworkLogger.getLogger(ScreenshotUtils.class);

    private ScreenshotUtils() {
        // utility class - no instances
    }

    /**
     * Captures a screenshot for the given test/status and returns the absolute path, or
     * null if capture was skipped/failed. Capture failures are logged but never thrown,
     * so they cannot affect the originating test's pass/fail outcome.
     */
    public static String capture(WebDriver driver, String testName, String status) {
        if (!ConfigReader.isScreenshotEnabled()) {
            logger.debug("Screenshot capture disabled via configuration; skipping for {}", testName);
            return null;
        }
        if (driver == null) {
            logger.warn("No active WebDriver session; cannot capture screenshot for {}", testName);
            return null;
        }

        try {
            Path targetDir = ExecutionManager.getScreenshotDir(status);
            String fileName = testName + "_" + DateTimeUtils.getFileTimestamp() + ".png";
            Path destination = targetDir.resolve(fileName);

            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Screenshot saved: {}", destination.toAbsolutePath());
            return destination.toAbsolutePath().toString();
        } catch (IOException | RuntimeException e) {
            logger.error("Failed to capture screenshot for {}: {}", testName, e.getMessage());
            return null;
        }
    }
}
