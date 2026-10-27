package com.automation.base;

import com.automation.config.ConfigReader;
import com.automation.logging.FrameworkLogger;
import com.automation.utils.ElementActions;
import com.automation.utils.WaitUtils;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;

/**
 * Parent class for every page object.
 *
 * Why this class exists: gives every page object access to the same WebDriver, the
 * shared {@link ElementActions}/{@link WaitUtils} helpers, and a logger, without each
 * page object re-creating them.
 */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final ElementActions actions;
    protected final WaitUtils waits;
    protected final Logger logger;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.actions = new ElementActions(driver);
        this.waits = new WaitUtils(driver, ConfigReader.getExplicitWaitSeconds());
        this.logger = FrameworkLogger.getLogger(getClass());
    }

    public void navigateTo(String url) {
        logger.info("Navigating to: {}", url);
        driver.get(url);
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
