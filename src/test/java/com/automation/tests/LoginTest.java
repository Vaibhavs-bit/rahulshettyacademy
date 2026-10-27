package com.automation.tests;

import com.automation.pages.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Business scenarios for the login page. Browser setup/teardown is inherited from
 * {@link BaseTest}; this class only contains readable test steps and assertions.
 */
public class LoginTest extends BaseTest {

    @Test(description = "Verify successful login with valid credentials")
    public void verifySuccessfulLogin() {
        LoginPage loginPage = new LoginPage(getDriver());

        logger.info("Open the application");
        loginPage.openApplication();

        logger.info("Login with valid credentials");
        loginPage.login(getValidUsername(), getValidPassword());

        logger.info("Verify successful login");
        Assert.assertTrue(loginPage.isLoginSuccessful(), "Login was unsuccessful");
    }
}
