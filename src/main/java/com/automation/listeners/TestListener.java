package com.automation.listeners;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.automation.base.DriverFactory;
import com.automation.config.ConfigReader;
import com.automation.core.ExecutionManager;
import com.automation.logging.FrameworkLogger;
import com.automation.reporting.ExtentReportManager;
import com.automation.utils.ScreenshotUtils;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Bridges TestNG's test lifecycle to logging, screenshot capture, and ExtentReports.
 *
 * Why this class exists: capturing evidence and report entries inside every test method
 * would be repetitive and easy to forget. A listener guarantees it happens consistently
 * for every test, regardless of how it passes, fails, or gets skipped.
 */
public class TestListener implements ITestListener {

    private static final Logger logger = FrameworkLogger.getLogger(TestListener.class);

    @Override
    public void onStart(ITestContext context) {
        logger.info("Suite started: {}", context.getSuite().getName());
        if (ConfigReader.isReportEnabled()) {
            ExtentReportManager.initReport();
        }
    }

    @Override
    public void onFinish(ITestContext context) {
        if (ConfigReader.isReportEnabled()) {
            ExtentReportManager.flushReport();
        }
        ExecutionManager.writeSummaryFile();
        logger.info("Suite finished: {}", context.getSuite().getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        ExecutionManager.incrementTotal();
        String name = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        logger.info("Test started: {}", name);
        if (ConfigReader.isReportEnabled()) {
            ExtentReportManager.createTest(name, description);
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExecutionManager.incrementPassed();
        String name = result.getMethod().getMethodName();
        logger.info("Test passed: {}", name);
        attachScreenshot(name, "passed", Status.PASS, "Test passed");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExecutionManager.incrementFailed();
        String name = result.getMethod().getMethodName();
        logger.error("Test failed: {}", name, result.getThrowable());
        if (ConfigReader.isReportEnabled() && ExtentReportManager.getTest() != null) {
            ExtentReportManager.getTest().fail(result.getThrowable());
        }
        attachScreenshot(name, "failed", Status.FAIL, "Test failed: " + result.getThrowable());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExecutionManager.incrementSkipped();
        String name = result.getMethod().getMethodName();
        logger.warn("Test skipped: {}", name, result.getThrowable());
        attachScreenshot(name, "skipped", Status.SKIP, "Test skipped: " + result.getThrowable());
    }

    private void attachScreenshot(String testName, String status, Status extentStatus, String message) {
        WebDriver driver = DriverFactory.getDriver();
        String path = ScreenshotUtils.capture(driver, testName, status);

        if (ConfigReader.isReportEnabled() && ExtentReportManager.getTest() != null) {
            if (path != null) {
                try {
                    ExtentReportManager.getTest().log(extentStatus, message,
                            MediaEntityBuilder.createScreenCaptureFromPath(path).build());
                } catch (Exception e) {
                    logger.error("Failed to attach screenshot to report for {}", testName, e);
                    ExtentReportManager.getTest().log(extentStatus, message);
                }
            } else {
                ExtentReportManager.getTest().log(extentStatus, message);
            }
        }
        ExtentReportManager.removeTest();
    }
}
