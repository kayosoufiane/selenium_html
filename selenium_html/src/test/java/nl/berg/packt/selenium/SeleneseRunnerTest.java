package nl.berg.packt.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link SeleneseRunner}.
 */
class SeleneseRunnerTest {

    @Test
    void parsesSuiteAndTestCaseXhtmlFiles() {
        Path suitePath = Path.of("src/test/resources/selenium/TestSuite.xhtml");
        List<Path> tests = SeleneseRunner.parseSuite(suitePath);

        assertEquals(1, tests.size());
        assertEquals("MyTest.xhtml", tests.get(0).getFileName().toString());

        SeleneseRunner.SeleneseTestCase testCase = SeleneseRunner.parseTestCase(tests.get(0));
        assertEquals("MyTest", testCase.name());
        assertFalse(testCase.commands().isEmpty());
        assertEquals("open", testCase.commands().get(0).command());
        assertEquals("/?from=%2F", testCase.commands().get(0).target());
        assertEquals("verifyTextPresent", testCase.commands().get(1).command());
        assertEquals("It works !", testCase.commands().get(1).target());
    }

    @Test
    void resolvesRelativeAndAbsoluteUrls() {
        assertEquals("http://localhost:8080/?from=%2F",
                SeleneseRunner.resolveUrl("http://localhost:8080/", "/?from=%2F"));
        assertEquals("http://localhost:8080/login",
                SeleneseRunner.resolveUrl("http://localhost:8080", "login"));
        assertEquals("https://example.org/health",
                SeleneseRunner.resolveUrl("http://localhost:8080/", "https://example.org/health"));
    }

    @Test
    void parsesSeleneseLocators() {
        assertEquals(By.id("submit"), SeleneseRunner.parseLocator("id=submit"));
        assertEquals(By.name("username"), SeleneseRunner.parseLocator("name=username"));
        assertEquals(By.cssSelector(".btn-primary"), SeleneseRunner.parseLocator("css=.btn-primary"));
        assertEquals(By.xpath("//button[@type='submit']"), SeleneseRunner.parseLocator("xpath=//button[@type='submit']"));
        assertEquals(By.linkText("Sign in"), SeleneseRunner.parseLocator("link=Sign in"));
    }
}
