package com.automation.core;

import com.automation.config.ConfigReader;
import com.automation.logging.FrameworkLogger;
import com.automation.utils.DateTimeUtils;
import com.automation.utils.FileUtils;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a single unique execution ID per suite run and provides the organized
 * output directories (screenshots, reports) that are keyed off it.
 *
 * Why this class exists: without a shared execution ID, screenshots, logs, and the
 * HTML report from the same run would be scattered across unrelated folders, making
 * it hard to bundle and share evidence for a single test run. The ID is generated once
 * when this class is first loaded (effectively suite start) and reused everywhere.
 */
public final class ExecutionManager {

    private static final Logger logger = FrameworkLogger.getLogger(ExecutionManager.class);

    private static final String DATE_FOLDER = DateTimeUtils.getDateFolder();
    private static final String EXECUTION_ID = "execution_" + DateTimeUtils.getExecutionTimestamp();

    private static final AtomicInteger totalTests = new AtomicInteger(0);
    private static final AtomicInteger passedTests = new AtomicInteger(0);
    private static final AtomicInteger failedTests = new AtomicInteger(0);
    private static final AtomicInteger skippedTests = new AtomicInteger(0);

    private static final long suiteStartTime = System.currentTimeMillis();

    private ExecutionManager() {
        // utility class - no instances
    }

    public static String getExecutionId() {
        return EXECUTION_ID;
    }

    public static String getDateFolder() {
        return DATE_FOLDER;
    }

    public static Path getOutputRoot() {
        return Path.of(ConfigReader.getOutputDirectory(), DATE_FOLDER, EXECUTION_ID);
    }

    public static Path getReportDir() {
        Path path = Path.of(ConfigReader.getReportDirectory(), DATE_FOLDER, EXECUTION_ID);
        return FileUtils.createDirectories(path);
    }

    /** Returns (creating if necessary) the folder for a given evidence status: passed/failed/skipped. */
    public static Path getScreenshotDir(String status) {
        Path path = getOutputRoot().resolve(status);
        return FileUtils.createDirectories(path);
    }

    public static void incrementTotal() {
        totalTests.incrementAndGet();
    }

    public static void incrementPassed() {
        passedTests.incrementAndGet();
    }

    public static void incrementFailed() {
        failedTests.incrementAndGet();
    }

    public static void incrementSkipped() {
        skippedTests.incrementAndGet();
    }

    /** Writes execution-summary.txt into the report directory and logs the same summary. */
    public static void writeSummaryFile() {
        long durationMillis = System.currentTimeMillis() - suiteStartTime;
        Path reportDir = getReportDir();
        String summary = String.format(
                "Execution ID       : %s%n" +
                "Total tests        : %d%n" +
                "Passed             : %d%n" +
                "Failed             : %d%n" +
                "Skipped            : %d%n" +
                "Duration (ms)      : %d%n" +
                "Report directory   : %s%n" +
                "Screenshot root    : %s%n" +
                "Log file           : %s%n",
                EXECUTION_ID, totalTests.get(), passedTests.get(), failedTests.get(), skippedTests.get(),
                durationMillis, reportDir.toAbsolutePath(), getOutputRoot().toAbsolutePath(),
                Path.of("test-output", "execution.log").toAbsolutePath());

        FileUtils.writeTextFile(reportDir.resolve("execution-summary.txt"), summary);
        logger.info("Execution summary:{}{}", System.lineSeparator(), summary);
    }
}
