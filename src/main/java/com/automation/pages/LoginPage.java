package com.automation.pages;

import com.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;

import com.automation.config.ConfigReader;

/**
 * Page object for the application's login page.
 */
public class LoginPage extends BasePage {

    private final By usernameField = By.id("userEmail");
    private final By passwordField = By.id("userPassword");
    private final By loginButton = By.id("login");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public void openApplication() {
        navigateTo(ConfigReader.getBaseUrl());
    }

    public void enterUsername(String username) {
        actions.clearAndEnterText(usernameField, username);
    }

    public void enterPassword(String password) {
        actions.clearAndEnterText(passwordField, password);
    }

    public void clickLogin() {
        actions.click(loginButton);
    }

    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    /** A successful login lands on the /dashboard route, so this needs no locator. Returns false (rather than
     *  throwing) when the redirect never happens, so negative-login scenarios can assert on the result. */
    public boolean isLoginSuccessful() {
        try {
            return waits.waitForUrlContains("/dashboard") && driver.getCurrentUrl().contains("/dashboard");
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Confirms the login form itself rendered, independent of any credentials. */
    public boolean isLoginPageDisplayed() {
        return actions.isDisplayed(usernameField) && actions.isDisplayed(passwordField) && actions.isDisplayed(loginButton);
    }
}
