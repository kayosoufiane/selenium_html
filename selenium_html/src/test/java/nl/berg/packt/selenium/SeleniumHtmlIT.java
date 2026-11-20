package nl.berg.packt.selenium;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Integration test executing the Selenium IDE XHTML test suite via Selenium 4 WebDriver.
 */
class SeleniumHtmlIT {

    private static WebDriver driver;
    private static String startUrl;
    private static Duration timeout;
    private static final List<SeleneseRunner.TestResult> RESULTS =
            Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    static void setUp() {
        startUrl = System.getProperty("selenium.startUrl", "http://localhost:8080/");
        String browser = System.getProperty("selenium.browser", "firefox");
        boolean headless = Boolean.parseBoolean(System.getProperty("selenium.headless", "true"));
        long timeoutSeconds = Long.parseLong(System.getProperty("selenium.timeoutSeconds", "10"));
        timeout = Duration.ofSeconds(timeoutSeconds);
        driver = SeleneseRunner.createWebDriver(browser, headless);
    }

    @AfterAll
    static void tearDown() throws IOException {
        try {
            if (!RESULTS.isEmpty()) {
                SeleneseRunner.writeHtmlReport(Path.of("target/results/selenium-report.html"), RESULTS);
            }
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }

    @TestFactory
    Stream<DynamicTest> executeSeleniumHtmlSuite() {
        Path suiteFile = Path.of(System.getProperty("selenium.suite", "src/test/resources/selenium/TestSuite.xhtml"));
        List<Path> testFiles = SeleneseRunner.parseSuite(suiteFile);

        return testFiles.stream().map(testFile -> {
            SeleneseRunner.SeleneseTestCase testCase = SeleneseRunner.parseTestCase(testFile);
            return DynamicTest.dynamicTest(testCase.name(), () -> {
                long startNanos = System.nanoTime();
                try {
                    SeleneseRunner.executeTestCase(driver, testCase, startUrl, timeout);
                    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                    RESULTS.add(new SeleneseRunner.TestResult(testCase.name(), true, "OK", elapsedMs));
                } catch (Throwable t) {
                    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
                    RESULTS.add(new SeleneseRunner.TestResult(testCase.name(), false, t.getMessage(), elapsedMs));
                    throw t;
                }
            });
        });
    }
}
