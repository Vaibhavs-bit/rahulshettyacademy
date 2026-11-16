// Seed test case definitions for the Test Case Tracker.
// Add baseline cases here, or use the "Add Test Case" button in the UI (saved to
// the browser's local storage so your edits persist across reloads).
// pageClassFile / testClassFile are repo-relative paths to the real .java files - set these
// so the "Code" button can show/edit the actual Page Object and TestNG source (requires
// running tools/testcase-tracker/server.js; see README section 15).
const SEED_TESTCASES = [
  {
    id: "TC-001",
    title: "Verify login page loads with key form elements present",
    description: "Confirms the login page renders the username, password, and submit controls.",
    priority: "High",
    requirement: "N/A",
    automated: true,
    automationKey: "com.automation.tests.LoginTest#verifyLoginPageLoads",
    pageClassFile: "src/main/java/com/automation/pages/LoginPage.java",
    testClassFile: "src/test/java/com/automation/tests/LoginTest.java",
    groups: "smoke, regression",
    steps: "1. Open the application\n2. Observe the login page",
    expectedResult: "Username field, password field, and login button are all displayed.",
    manualStatus: "Not Run",
    manualLastRun: null,
    manualHistory: []
  },
  {
    id: "TC-002",
    title: "Verify successful login with valid credentials",
    description: "Confirms a user with valid credentials can log in successfully.",
    priority: "High",
    requirement: "N/A",
    automated: true,
    automationKey: "com.automation.tests.LoginTest#verifySuccessfulLogin",
    pageClassFile: "src/main/java/com/automation/pages/LoginPage.java",
    testClassFile: "src/test/java/com/automation/tests/LoginTest.java",
    groups: "smoke, regression",
    steps: "1. Open the application\n2. Enter valid username and password\n3. Submit the login form",
    expectedResult: "User is logged in and lands on the post-login page.",
    manualStatus: "Not Run",
    manualLastRun: null,
    manualHistory: []
  },
  {
    id: "TC-003",
    title: "Verify login is rejected with invalid credentials",
    description: "Confirms login fails gracefully when invalid credentials are submitted.",
    priority: "Medium",
    requirement: "N/A",
    automated: true,
    automationKey: "com.automation.tests.LoginTest#verifyLoginFailsWithInvalidCredentials",
    pageClassFile: "src/main/java/com/automation/pages/LoginPage.java",
    testClassFile: "src/test/java/com/automation/tests/LoginTest.java",
    groups: "regression, negative",
    steps: "1. Open the application\n2. Enter an invalid username and password\n3. Submit the login form",
    expectedResult: "Login is rejected and the user remains on the login page.",
    manualStatus: "Not Run",
    manualLastRun: null,
    manualHistory: []
  }
];
