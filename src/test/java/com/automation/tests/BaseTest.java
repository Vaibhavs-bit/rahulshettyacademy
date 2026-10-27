package com.automation.tests;

import com.automation.base.DriverFactory;
import com.automation.config.ConfigReader;
import com.automation.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Parent for every test class: owns WebDriver setup/teardown so individual test classes
 * only contain business scenarios, not browser plumbing.
 *
 * Why this class exists: every test needs an isolated browser session created before it
 * runs and cleaned up afterward, including when the test itself fails. Centralizing that
 * here means it happens identically and reliably for every test class.
 */
public abstract class BaseTest {

    protected static final Logger logger = FrameworkLogger.getLogger(BaseTest.class);
    private static Properties testData;

    @BeforeMethod(alwaysRun = true)
    @Parameters({"browser", "headless"})
    public void setUp(@Optional("") String browserParam, @Optional("") String headlessParam) {
        String browser = !browserParam.isBlank() ? browserParam : ConfigReader.getBrowser();
        boolean headless = !headlessParam.isBlank() ? Boolean.parseBoolean(headlessParam) : ConfigReader.isHeadless();

        DriverFactory.initDriver(browser, headless, ConfigReader.getPageLoadTimeoutSeconds());
        logger.info("Browser session ready: browser={}, headless={}", browser, headless);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        // Teardown runs even when the test or an assertion above failed, so the browser
        // session is never leaked between tests.
        DriverFactory.quitDriver();
    }

    protected WebDriver getDriver() {
        return DriverFactory.getDriver();
    }

    /** Lazily loads testdata/login-data.properties so credentials never need to be hardcoded in a test. */
    protected synchronized Properties getTestData() {
        if (testData == null) {
            testData = new Properties();
            try (InputStream input = getClass().getClassLoader().getResourceAsStream("testdata/login-data.properties")) {
                if (input == null) {
                    throw new RuntimeException("testdata/login-data.properties not found on classpath");
                }
                testData.load(input);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load test data", e);
            }
        }
        return testData;
    }

    protected String getValidUsername() {
        return resolveCredential("valid.username", "VALID_USERNAME");
    }

    protected String getValidPassword() {
        return resolveCredential("valid.password", "VALID_PASSWORD");
    }

    /** Prefers a -D system property, then an env var, over the (non-secret) properties file - so real credentials never need to be committed. */
    private String resolveCredential(String propertyKey, String envVarName) {
        String systemValue = System.getProperty(propertyKey);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue;
        }
        String envValue = System.getenv(envVarName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return getTestData().getProperty(propertyKey);
    }
}
