package com.automation.tests;

import com.automation.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Business scenarios for the login page. Browser setup/teardown is inherited from
 * {@link BaseTest}; this class only contains readable test steps and assertions.
 *
 * Priority controls intra-class ordering (lower runs first); groups let testng.xml
 * include/exclude scenarios (e.g. smoke vs. regression vs. negative) without touching code.
 */
public class LoginTest extends BaseTest {

    @Test(priority = 1, groups = {"smoke", "regression"},
            description = "Verify the login page loads with its key form elements present")
    public void verifyLoginPageLoads() {
        LoginPage loginPage = new LoginPage(getDriver());

        logger.info("Open the application");
        loginPage.openApplication();

        logger.info("Verify login page elements are displayed");
        Assert.assertTrue(loginPage.isLoginPageDisplayed(), "Login page elements were not displayed");
    }

    @Test(priority = 2, groups = {"smoke", "regression"},
            description = "Verify successful login with valid credentials")
    public void verifySuccessfulLogin() {
        LoginPage loginPage = new LoginPage(getDriver());

        logger.info("Open the application");
        loginPage.openApplication();

        logger.info("Login with valid credentials");
        loginPage.login(getValidUsername(), getValidPassword());

        logger.info("Verify successful login");
        Assert.assertTrue(loginPage.isLoginSuccessful(), "Login was unsuccessful");
    }

    @Test(priority = 3, groups = {"regression", "negative"},
            description = "Verify login is rejected with invalid credentials")
    public void verifyLoginFailsWithInvalidCredentials() {
        LoginPage loginPage = new LoginPage(getDriver());

        logger.info("Open the application");
        loginPage.openApplication();

        logger.info("Login with invalid credentials");
        loginPage.login("invalid_user@example.com", "WrongPass123!");

        logger.info("Verify login is rejected");
        Assert.assertFalse(loginPage.isLoginSuccessful(), "Login unexpectedly succeeded with invalid credentials");
    }
@Test(priority = 4, groups = {"smoke"},
            description = "Verify login is rejected without credentials")
       public void verifyLoginFailsWithoutCredentials() {
        LoginPage loginPage = new LoginPage(getDriver());

        logger.info("Open the application");
        loginPage.openApplication();

        logger.info("dont enter any username password  and click on login button");
        loginPage.clickLogin();

        logger.info("Verify login is rejected");
        Assert.assertFalse(loginPage.isLoginSuccessful(), "Login unexpectedly succeeded with invalid credentials");
    }
}
