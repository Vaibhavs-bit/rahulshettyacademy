package com.automation.config;

import com.automation.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads and exposes framework configuration from {@code config.properties}.
 *
 * Why this class exists: keeps environment-specific values (base URL, browser, timeouts,
 * toggles) out of Java source code so they can be changed without recompiling. Values
 * supplied as JVM system properties (e.g. "-Dbrowser=firefox" via the Maven Surefire plugin)
 * always take precedence over the file so CI pipelines can override settings per run.
 */
public final class ConfigReader {

    private static final Logger logger = FrameworkLogger.getLogger(ConfigReader.class);
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties not found on classpath (src/main/resources).");
            }
            properties.load(input);
            validateRequiredProperties();
            logger.info("Configuration loaded successfully from config.properties");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private ConfigReader() {
        // utility class - no instances
    }

    private static void validateRequiredProperties() {
        if (isBlank(properties.getProperty("base.url"))) {
            throw new RuntimeException("Required property 'base.url' is missing or empty in config.properties");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Resolves a property, preferring a JVM system property of the same name over the
     * value defined in config.properties. This is what allows "mvn test -Dbrowser=chrome"
     * to override the file-based default.
     */
    public static String get(String key) {
        String systemValue = System.getProperty(key);
        if (!isBlank(systemValue)) {
            return systemValue;
        }
        return properties.getProperty(key);
    }

    public static String getBaseUrl() {
        return get("base.url");
    }

    public static String getBrowser() {
        String browser = get("browser");
        return isBlank(browser) ? "chrome" : browser;
    }

    public static boolean isHeadless() {
        return Boolean.parseBoolean(get("headless"));
    }

    public static long getExplicitWaitSeconds() {
        return Long.parseLong(get("explicit.wait.seconds"));
    }

    public static long getPageLoadTimeoutSeconds() {
        return Long.parseLong(get("page.load.timeout.seconds"));
    }

    public static boolean isScreenshotEnabled() {
        return Boolean.parseBoolean(get("screenshot.enabled"));
    }

    public static boolean isReportEnabled() {
        return Boolean.parseBoolean(get("report.enabled"));
    }

    public static boolean isParallelEnabled() {
        return Boolean.parseBoolean(get("parallel.enabled"));
    }

    public static String getOutputDirectory() {
        return get("output.directory");
    }

    public static String getReportDirectory() {
        return get("report.directory");
    }
}
