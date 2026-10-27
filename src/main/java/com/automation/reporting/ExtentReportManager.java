package com.automation.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.automation.config.ConfigReader;
import com.automation.core.ExecutionManager;
import com.automation.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;

/**
 * Owns the single consolidated ExtentReports instance for a suite run.
 *
 * Why this class exists: ExtentReports requires one shared ExtentReports instance that
 * every test attaches entries to, but each test thread needs its own "current test" node.
 * A ThreadLocal<ExtentTest> keeps parallel executions from attaching steps/screenshots
 * to the wrong test, while the single ExtentReports instance produces one HTML file.
 */
public final class ExtentReportManager {

    private static final Logger logger = FrameworkLogger.getLogger(ExtentReportManager.class);

    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();

    private ExtentReportManager() {
        // utility class - no instances
    }

    public static synchronized void initReport() {
        if (extentReports != null) {
            return;
        }
        String reportPath = ExecutionManager.getReportDir().resolve("AutomationReport.html").toString();

        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setDocumentTitle("Automation Report");
        sparkReporter.config().setReportName("Selenium Automation Suite - " + ExecutionManager.getExecutionId());

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Execution ID", ExecutionManager.getExecutionId());
        extentReports.setSystemInfo("Browser", ConfigReader.getBrowser());
        extentReports.setSystemInfo("Headless", String.valueOf(ConfigReader.isHeadless()));
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));

        logger.info("ExtentReports initialized at: {}", reportPath);
    }

    public static ExtentTest createTest(String name, String description) {
        ExtentTest test = extentReports.createTest(name, description);
        currentTest.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return currentTest.get();
    }

    public static void removeTest() {
        currentTest.remove();
    }

    public static synchronized void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
            logger.info("ExtentReports flushed to disk.");
        }
    }
}
