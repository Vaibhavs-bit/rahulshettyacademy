package com.automation.base;

import com.automation.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * Owns WebDriver creation and lifecycle.
 *
 * Why this class exists: browser setup/teardown logic (options, timeouts, headless mode)
 * needs one owner so it is configured identically for every test. WebDriver instances are
 * stored in a {@link ThreadLocal} rather than a static field so parallel TestNG execution
 * gives each thread its own isolated browser session instead of sharing one driver, which
 * would cause tests to interfere with each other.
 */
public final class DriverFactory {

    private static final Logger logger = FrameworkLogger.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverFactory() {
        // utility class - no instances
    }

    public static WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    /** Creates a new WebDriver for the current thread. Selenium Manager resolves the matching driver binary automatically. */
    public static void initDriver(String browser, boolean headless, long pageLoadTimeoutSeconds) {
        WebDriver driver = createDriver(browser, headless);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(pageLoadTimeoutSeconds));
        driverThreadLocal.set(driver);
        logger.info("Initialized {} WebDriver (headless={}) on thread {}", browser, headless, Thread.currentThread().getId());
    }

    private static WebDriver createDriver(String browser, boolean headless) {
        switch (browser == null ? "chrome" : browser.toLowerCase()) {
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (headless) {
                    firefoxOptions.addArguments("-headless");
                }
                return new FirefoxDriver(firefoxOptions);

            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--window-size=1920,1080");
                if (headless) {
                    edgeOptions.addArguments("--headless=new");
                }
                return new EdgeDriver(edgeOptions);

            case "chrome":
            default:
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--window-size=1920,1080");
                if (headless) {
                    chromeOptions.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");
                }
                return new ChromeDriver(chromeOptions);
        }
    }

    /** Quits the current thread's WebDriver and removes the ThreadLocal reference to avoid leaks. */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
            logger.info("Quit WebDriver on thread {}", Thread.currentThread().getId());
        }
    }
}
