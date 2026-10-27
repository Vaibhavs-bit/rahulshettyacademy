package com.automation.utils;

import com.automation.config.ConfigReader;
import com.automation.logging.FrameworkLogger;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;
import java.util.Set;

/**
 * Centralizes every reusable Selenium WebElement/browser operation used across page
 * objects and tests.
 *
 * Why this class exists: without it, each page object would re-implement its own
 * clicking, typing, waiting, and synchronization logic, leading to duplicated code and
 * inconsistent behavior. Page objects and tests should call into this class rather than
 * use the Selenium API directly.
 *
 * Used by: BasePage and all page-specific classes.
 *
 * Example:
 *   elementActions.click(loginButton);
 *   elementActions.enterText(usernameField, username);
 */
public class ElementActions {

    private static final Logger logger = FrameworkLogger.getLogger(ElementActions.class);

    private final WebDriver driver;
    private final WaitUtils waits;
    private final Actions actions;

    public ElementActions(WebDriver driver) {
        this.driver = driver;
        this.waits = new WaitUtils(driver, ConfigReader.getExplicitWaitSeconds());
        this.actions = new Actions(driver);
    }

    // ---------------------------------------------------------------------
    // Lookup helpers
    // ---------------------------------------------------------------------

    /** Waits for presence in the DOM and returns the element; use when a page object only has a By locator. */
    public WebElement find(By locator) {
        return waits.waitForPresence(locator);
    }

    public List<WebElement> findAll(By locator) {
        return waits.waitForAllPresent(locator);
    }

    public int count(By locator) {
        return driver.findElements(locator).size();
    }

    // ---------------------------------------------------------------------
    // Basic WebElement operations
    // ---------------------------------------------------------------------

    public void click(WebElement element) {
        waits.waitForClickable(element).click();
        logger.debug("Clicked element: {}", element);
    }

    public void click(By locator) {
        waits.waitForClickable(locator).click();
        logger.debug("Clicked element located by: {}", locator);
    }

    /**
     * Clicks via JavaScript instead of a native click.
     *
     * Why: a normal click can fail when an element is covered by an overlay or is
     * technically present but not "interactable" according to Selenium. This is an
     * explicit, opt-in fallback - callers should only use it after a normal click has
     * failed and the cause has been understood, not as a default click strategy.
     */
    public void jsClick(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        logger.debug("JavaScript-clicked element: {}", element);
    }

    public void enterText(WebElement element, String text) {
        waits.waitForVisibility(element).sendKeys(text);
        logger.debug("Entered text into element: {}", element);
    }

    public void enterText(By locator, String text) {
        waits.waitForVisibility(locator).sendKeys(text);
        logger.debug("Entered text into element located by: {}", locator);
    }

    public void clearText(WebElement element) {
        element.clear();
    }

    public void clearAndEnterText(WebElement element, String text) {
        WebElement el = waits.waitForVisibility(element);
        el.clear();
        el.sendKeys(text);
        logger.debug("Cleared and entered text into element: {}", element);
    }

    public void clearAndEnterText(By locator, String text) {
        clearAndEnterText(find(locator), text);
    }

    public String getText(WebElement element) {
        return waits.waitForVisibility(element).getText();
    }

    public String getText(By locator) {
        return waits.waitForVisibility(locator).getText();
    }

    public String getAttribute(WebElement element, String attributeName) {
        return element.getAttribute(attributeName);
    }

    public boolean isDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    public boolean isDisplayed(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        return !elements.isEmpty() && elements.get(0).isDisplayed();
    }

    public boolean isEnabled(WebElement element) {
        return element.isEnabled();
    }

    public boolean isSelected(WebElement element) {
        return element.isSelected();
    }

    // ---------------------------------------------------------------------
    // Wait operations (thin, convenient pass-through to WaitUtils)
    // ---------------------------------------------------------------------

    public WebElement waitUntilVisible(By locator) {
        return waits.waitForVisibility(locator);
    }

    public WebElement waitUntilClickable(By locator) {
        return waits.waitForClickable(locator);
    }

    public WebElement waitUntilPresent(By locator) {
        return waits.waitForPresence(locator);
    }

    public boolean waitUntilInvisible(By locator) {
        return waits.waitForInvisibility(locator);
    }

    public boolean waitUntilTextPresent(WebElement element, String text) {
        return waits.waitForTextToBePresentInElement(element, text);
    }

    public boolean waitUntilUrlContains(String fraction) {
        return waits.waitForUrlContains(fraction);
    }

    // ---------------------------------------------------------------------
    // Dropdown operations
    // ---------------------------------------------------------------------

    public void selectByVisibleText(WebElement element, String visibleText) {
        new Select(element).selectByVisibleText(visibleText);
    }

    public void selectByValue(WebElement element, String value) {
        new Select(element).selectByValue(value);
    }

    public void selectByIndex(WebElement element, int index) {
        new Select(element).selectByIndex(index);
    }

    public String getSelectedOption(WebElement element) {
        return new Select(element).getFirstSelectedOption().getText();
    }

    /** Handles non-native dropdowns: clicks a trigger element, then clicks the matching option locator. */
    public void selectCustomDropdownOption(WebElement trigger, By optionLocator) {
        click(trigger);
        click(waits.waitForVisibility(optionLocator));
    }

    // ---------------------------------------------------------------------
    // Mouse operations
    // ---------------------------------------------------------------------

    public void hover(WebElement element) {
        actions.moveToElement(element).perform();
    }

    public void moveToElement(WebElement element) {
        actions.moveToElement(element).perform();
    }

    public void rightClick(WebElement element) {
        actions.contextClick(element).perform();
    }

    public void doubleClick(WebElement element) {
        actions.doubleClick(element).perform();
    }

    public void dragAndDrop(WebElement source, WebElement target) {
        actions.dragAndDrop(source, target).perform();
    }

    public void clickAndHold(WebElement element) {
        actions.clickAndHold(element).perform();
    }

    public void release() {
        actions.release().perform();
    }

    // ---------------------------------------------------------------------
    // Scroll operations
    // ---------------------------------------------------------------------

    public void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", element);
    }

    public void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    public void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    public void scrollBy(int xPixels, int yPixels) {
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(arguments[0], arguments[1]);", xPixels, yPixels);
    }

    // ---------------------------------------------------------------------
    // Keyboard operations
    // ---------------------------------------------------------------------

    public void pressEnter(WebElement element) {
        element.sendKeys(Keys.ENTER);
    }

    public void pressEscape() {
        actions.sendKeys(Keys.ESCAPE).perform();
    }

    public void pressTab(WebElement element) {
        element.sendKeys(Keys.TAB);
    }

    public void selectAllText(WebElement element) {
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
    }

    public void sendKeys(WebElement element, CharSequence... keys) {
        element.sendKeys(keys);
    }

    // ---------------------------------------------------------------------
    // Other browser operations
    // ---------------------------------------------------------------------

    public void acceptAlert() {
        driver.switchTo().alert().accept();
    }

    public void dismissAlert() {
        driver.switchTo().alert().dismiss();
    }

    public String getAlertText() {
        return driver.switchTo().alert().getText();
    }

    public void typeInAlert(String text) {
        driver.switchTo().alert().sendKeys(text);
    }

    public void switchToWindowByIndex(int index) {
        List<String> handles = List.copyOf(driver.getWindowHandles());
        driver.switchTo().window(handles.get(index));
    }

    /** Switches to the most recently opened window/tab - useful right after an action opens a new one. */
    public void switchToNewWindow(Set<String> handlesBeforeAction) {
        waits.waitForNumberOfWindowsToBe(handlesBeforeAction.size() + 1);
        for (String handle : driver.getWindowHandles()) {
            if (!handlesBeforeAction.contains(handle)) {
                driver.switchTo().window(handle);
                return;
            }
        }
    }

    public void switchToFrame(By locator) {
        driver.switchTo().frame(find(locator));
    }

    public void switchToFrame(int index) {
        driver.switchTo().frame(index);
    }

    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    public void uploadFile(WebElement fileInput, String absoluteFilePath) {
        fileInput.sendKeys(absoluteFilePath);
    }

    public void refreshPage() {
        driver.navigate().refresh();
    }

    public void navigateBack() {
        driver.navigate().back();
    }

    public void navigateForward() {
        driver.navigate().forward();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }
}
