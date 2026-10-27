# Selenium Automation Framework

A reusable, maintainable Selenium WebDriver UI automation framework built with **Java 17**,
**Selenium 4**, **TestNG**, and **Maven**, using the **Page Object Model**. Every run produces
detailed logs, organized pass/fail/skip screenshots, and a consolidated **ExtentReports** HTML
report.

## 1. Technology Stack

| Concern              | Technology                                   |
|-----------------------|-----------------------------------------------|
| Language              | Java 17                                       |
| Browser automation    | Selenium WebDriver 4.x (Selenium Manager)     |
| Test runner           | TestNG                                        |
| Build tool            | Maven                                         |
| Reporting             | ExtentReports (Spark HTML reporter)           |
| Logging               | Log4j2                                        |
| Design pattern        | Page Object Model                             |

Browser driver binaries are **not** bundled or manually downloaded - Selenium 4's built-in
Selenium Manager resolves the correct driver for the configured browser automatically.

## 2. Architecture / Folder Structure

```
selenium-automation-framework/
├── pom.xml
├── testng.xml (src/test/resources)
├── src/main/java/com/automation/
│   ├── base/        DriverFactory (WebDriver lifecycle), BasePage (page object parent)
│   ├── config/      ConfigReader (config.properties + system property overrides)
│   ├── core/        ExecutionManager (execution IDs, output directories, run summary)
│   ├── pages/        Page objects (e.g. LoginPage)
│   ├── utils/        ElementActions, WaitUtils, ScreenshotUtils, FileUtils, DateTimeUtils
│   ├── listeners/    TestListener (TestNG lifecycle -> logging/screenshots/report)
│   ├── reporting/    ExtentReportManager (consolidated HTML report)
│   └── logging/      FrameworkLogger (Log4j2 facade)
├── src/main/resources/
│   ├── config.properties
│   ├── log4j2.xml
│   └── testdata/login-data.properties
└── src/test/java/com/automation/tests/
    ├── BaseTest.java       (per-test browser setup/teardown)
    ├── LoginTest.java      (successful + invalid login scenarios)
    └── NavigationTest.java (page-load verification)
```

### Why each major component exists
- **DriverFactory** – creates/destroys WebDriver instances behind a `ThreadLocal`, so parallel
  tests never share a browser session.
- **ElementActions** – every reusable Selenium interaction (click, type, waits, dropdowns,
  mouse, scroll, keyboard, alerts, frames, windows, file upload) lives here so page objects
  never call the raw Selenium API directly.
- **WaitUtils** – a single, consistent explicit-wait layer used by `ElementActions`; the
  framework never relies on `Thread.sleep()`.
- **BasePage** – gives every page object the driver plus shared `ElementActions`/`WaitUtils`.
- **ConfigReader** – loads `config.properties`; JVM system properties (`-Dbrowser=...`) always
  win, which is how `mvn test -Dbrowser=firefox` overrides the file.
- **ExecutionManager** – generates one execution ID per suite run and derives the
  `test-output/<date>/<executionId>/...` and `reports/<date>/<executionId>/...` paths from it,
  so a single run's logs, screenshots, and report can be shared together.
- **TestListener** – implements `ITestListener`; on every test start/pass/fail/skip it updates
  counters, captures a screenshot, and writes an entry (with the screenshot attached) to the
  ExtentReports instance.
- **ExtentReportManager** – owns the single `ExtentReports` instance; uses a
  `ThreadLocal<ExtentTest>` so parallel tests don't attach steps to the wrong test.

## 3. Prerequisites

- JDK 17+
- Maven 3.9+
- Google Chrome and/or Firefox/Edge installed locally (for non-headless/local runs)

## 4. Configuring the Application Under Test

`src/main/resources/config.properties` controls the target application and defaults:

```properties
base.url=https://the-internet.herokuapp.com/login
browser=chrome
headless=false
explicit.wait.seconds=15
page.load.timeout.seconds=30
screenshot.enabled=true
report.enabled=true
parallel.enabled=false
output.directory=test-output
report.directory=reports
```

> `base.url` currently points at **the-internet.herokuapp.com**, a public Selenium training
> site, so the framework runs out of the box. Replace it with your real application's URL for
> actual project use.

Any property can be overridden from the command line without editing the file:

```bash
mvn clean test -Dbrowser=firefox -Dheadless=true
```

## 5. Supplying Test Credentials Safely

Credentials live in `src/main/resources/testdata/login-data.properties`, never in test code.
The shipped file contains **the-internet.herokuapp.com's publicly documented demo
credentials** - not secrets. For a real application:
- Replace the values with your own test-account credentials.
- Prefer environment variables or a secrets manager for anything sensitive, and have
  `BaseTest.getTestData()` read from there instead of (or in addition to) the properties file.
- Never commit real credentials.

## 6. Running Tests

```bash
mvn clean test                          # run the full suite (src/test/resources/testng.xml)
mvn clean test -Dtest=LoginTest         # run a single test class
mvn clean test -Dbrowser=chrome         # force Chrome
mvn clean test -Dbrowser=firefox        # force Firefox
mvn clean test -Dbrowser=edge           # force Edge
mvn clean test -Dheadless=true          # run headless (recommended for CI)
```

A failed test fails the Maven build (non-zero exit code), so this integrates cleanly into CI.

## 7. Writing a New Page Object + Test

**1. Create the page object** (extends `BasePage`, keeps locators private):

```java
public class DashboardPage extends BasePage {
    private final By welcomeBanner = By.id("welcome-banner");

    public DashboardPage(WebDriver driver) { super(driver); }

    public boolean isWelcomeBannerVisible() {
        return actions.isDisplayed(welcomeBanner);
    }
}
```

**2. Create the test** (extends `BaseTest`, uses `getDriver()`):

```java
public class DashboardTest extends BaseTest {
    @Test(description = "Verify dashboard loads after login")
    public void verifyDashboardLoads() {
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.openApplication();
        loginPage.login(getValidUsername(), getValidPassword());

        DashboardPage dashboard = new DashboardPage(getDriver());
        Assert.assertTrue(dashboard.isWelcomeBannerVisible());
    }
}
```

**3. Register the class** in `src/test/resources/testng.xml` under a `<test>` block.

## 8. Parallel Execution

`DriverFactory` already isolates each thread's WebDriver via `ThreadLocal`, and
`ExtentReportManager`/screenshot filenames are thread-safe, so enabling parallel execution only
requires editing `testng.xml`:

```xml
<suite name="AutomationSuite" parallel="methods" thread-count="4">
```

## 9. Screenshots

Captured automatically by `TestListener` for every passed, failed, and skipped test (toggle
with `screenshot.enabled`). Organized as:

```
test-output/<yyyy-MM-dd>/execution_<HHmmss>/
├── passed/
├── failed/
└── skipped/
```

## 10. Logging

Log4j2 writes to the console and to `test-output/execution.log` (daily/20MB rolling, 10 files
kept). Configure levels/appenders in `src/main/resources/log4j2.xml`. Passwords and tokens are
never logged.

## 11. HTML Report

`AutomationReport.html` is generated per execution under:

```
reports/<yyyy-MM-dd>/execution_<HHmmss>/AutomationReport.html
reports/<yyyy-MM-dd>/execution_<HHmmss>/execution-summary.txt
```

It includes execution metadata, browser/OS/Java version, pass/fail/skip counts, per-test steps,
failure stack traces, and attached screenshots.

## 12. Execution Flow

1. Maven invokes the TestNG suite (`testng.xml`).
2. `TestListener.onStart` initializes `ExtentReportManager`.
3. `BaseTest.setUp` calls `DriverFactory.initDriver(...)` before each test method.
4. The test creates a page object and drives it through `ElementActions`/`WaitUtils`.
5. TestNG assertions pass/fail the test.
6. `TestListener` records the outcome, captures a screenshot, and logs an ExtentReports entry.
7. `BaseTest.tearDown` calls `DriverFactory.quitDriver()` - this always runs, even after a
   failed assertion or setup error, via `@AfterMethod(alwaysRun = true)`.
8. `TestListener.onFinish` flushes the HTML report and writes `execution-summary.txt`.

## 13. Troubleshooting

| Symptom                                   | Likely cause / fix                                                   |
|--------------------------------------------|------------------------------------------------------------------------|
| `config.properties not found`              | Ensure `src/main/resources` is on the classpath (run via Maven).       |
| Browser does not launch                    | Confirm the browser is installed; Selenium Manager needs internet access the first time to fetch a driver. |
| `SessionNotCreatedException` mentioning a ChromeDriver/Chrome version mismatch | Selenium Manager could not resolve a ChromeDriver build matching your installed Chrome version (common right after a Chrome auto-update, before a matching driver is published). Run with `-Dbrowser=edge` or `-Dbrowser=firefox` until a matching ChromeDriver is available, or set `-Dwebdriver.chrome.driver=<path>` to a manually downloaded matching build. |
| Tests hang on CI                           | Add `-Dheadless=true`.                                                  |
| Report/screenshots missing                 | Check `report.enabled` / `screenshot.enabled` in `config.properties`.  |
| Stale element / timing failures            | Prefer the `ElementActions`/`WaitUtils` methods over raw Selenium calls - avoid `Thread.sleep()`. |

## 14. Known Limitations

- Test data is file-based (properties); no Excel/DB-backed data provider is included yet, per
  the initial scope - extend `BaseTest.getTestData()` if that's needed later.
- Log4j2's rolling file path is static (`test-output/execution.log`), not nested per execution
  ID, since Log4j2 configuration is resolved before `ExecutionManager` generates the ID at
  runtime; screenshots and the HTML report are nested per execution ID.
