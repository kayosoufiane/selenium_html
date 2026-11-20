package nl.berg.packt.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses and executes legacy Selenium IDE XHTML suites and test cases using Selenium 4 WebDriver.
 */
public final class SeleneseRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleneseRunner.class);

    public record SeleneseCommand(String command, String target, String value) {
    }

    public record SeleneseTestCase(String name, Path file, List<SeleneseCommand> commands) {
    }

    public record TestResult(String name, boolean passed, String message, long durationMs) {
    }

    private SeleneseRunner() {
    }

    /**
     * Parses a Selenium IDE suite XHTML file and returns all referenced test case paths.
     */
    public static List<Path> parseSuite(Path suiteFile) {
        if (!Files.isRegularFile(suiteFile)) {
            throw new IllegalArgumentException("Suite file not found: " + suiteFile);
        }
        Document doc = parseXml(suiteFile);
        Path suiteDir = suiteFile.toAbsolutePath().getParent();
        List<Path> testFiles = new ArrayList<>();

        NodeList links = doc.getElementsByTagName("a");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String href = link.getAttribute("href").trim();
            if (!href.isEmpty()) {
                testFiles.add(suiteDir.resolve(href).normalize());
            }
        }
        return testFiles;
    }

    /**
     * Parses a single Selenium IDE test case XHTML file into ordered commands.
     */
    public static SeleneseTestCase parseTestCase(Path testFile) {
        if (!Files.isRegularFile(testFile)) {
            throw new IllegalArgumentException("Test case file not found: " + testFile);
        }
        Document doc = parseXml(testFile);
        String title = extractTitle(doc, testFile);
        List<SeleneseCommand> commands = new ArrayList<>();

        NodeList tbodies = doc.getElementsByTagName("tbody");
        for (int i = 0; i < tbodies.getLength(); i++) {
            Element tbody = (Element) tbodies.item(i);
            NodeList rows = tbody.getElementsByTagName("tr");
            for (int r = 0; r < rows.getLength(); r++) {
                Element row = (Element) rows.item(r);
                NodeList cells = row.getElementsByTagName("td");
                if (cells.getLength() >= 2) {
                    String cmd = cells.item(0).getTextContent().trim();
                    String target = cells.item(1).getTextContent().trim();
                    String value = cells.getLength() >= 3 ? cells.item(2).getTextContent().trim() : "";
                    if (!cmd.isEmpty()) {
                        commands.add(new SeleneseCommand(cmd, target, value));
                    }
                }
            }
        }
        return new SeleneseTestCase(title, testFile, commands);
    }

    /**
     * Creates a Selenium 4 WebDriver instance managed automatically by Selenium Manager.
     */
    public static WebDriver createWebDriver(String browser, boolean headless) {
        String normalized = browser == null ? "firefox" : browser.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "firefox", "*firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                yield new FirefoxDriver(options);
            }
            case "chrome", "*chrome", "*googlechrome" -> {
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                yield new ChromeDriver(options);
            }
            case "edge", "*edge" -> {
                EdgeOptions options = new EdgeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                yield new EdgeDriver(options);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported browser '" + browser + "'. Supported values: firefox, chrome, edge.");
        };
    }

    /**
     * Executes a parsed test case against the provided base URL using WebDriver.
     */
    public static void executeTestCase(WebDriver driver, SeleneseTestCase testCase, String baseUrl, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        for (SeleneseCommand step : testCase.commands()) {
            LOGGER.info("[{}] {} | {} | {}", testCase.name(), step.command(), step.target(), step.value());
            switch (step.command()) {
                case "open" -> driver.get(resolveUrl(baseUrl, step.target()));
                case "verifyTextPresent", "assertTextPresent" -> {
                    String expected = step.target();
                    boolean found = wait.until(d -> {
                        String bodyText = d.findElement(By.tagName("body")).getText();
                        if (bodyText != null && bodyText.contains(expected)) {
                            return true;
                        }
                        String source = d.getPageSource();
                        return source != null && source.contains(expected);
                    });
                    if (!found) {
                        throw new AssertionError("Expected text not present: " + expected);
                    }
                }
                case "verifyTitle", "assertTitle" -> {
                    String expectedTitle = step.target();
                    boolean matched = wait.until(d -> expectedTitle.equals(d.getTitle()));
                    if (!matched) {
                        throw new AssertionError("Expected title '" + expectedTitle + "' but was '" + driver.getTitle() + "'");
                    }
                }
                case "click", "clickAndWait" -> {
                    By locator = parseLocator(step.target());
                    WebElement element = wait.until(d -> d.findElement(locator));
                    element.click();
                }
                case "type" -> {
                    By locator = parseLocator(step.target());
                    WebElement element = wait.until(d -> d.findElement(locator));
                    element.clear();
                    element.sendKeys(step.value());
                }
                case "waitForElementPresent" -> {
                    By locator = parseLocator(step.target());
                    wait.until(d -> d.findElement(locator));
                }
                default -> throw new UnsupportedOperationException(
                        "Unsupported Selenese command '" + step.command() + "' in " + testCase.file());
            }
        }
    }

    /**
     * Resolves a relative Selenese target path against the configured base start URL.
     */
    public static String resolveUrl(String baseUrl, String target) {
        if (target == null || target.isBlank()) {
            return baseUrl;
        }
        String trimmed = target.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        URI base = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        String relative = trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
        return base.resolve(relative).toString();
    }

    /**
     * Converts a Selenese locator string (e.g. id=, name=, css=, xpath=, link=) into a Selenium By.
     */
    public static By parseLocator(String locator) {
        if (locator == null || locator.isBlank()) {
            throw new IllegalArgumentException("Locator must not be empty");
        }
        if (locator.startsWith("id=")) {
            return By.id(locator.substring(3));
        }
        if (locator.startsWith("name=")) {
            return By.name(locator.substring(5));
        }
        if (locator.startsWith("css=")) {
            return By.cssSelector(locator.substring(4));
        }
        if (locator.startsWith("xpath=")) {
            return By.xpath(locator.substring(6));
        }
        if (locator.startsWith("//")) {
            return By.xpath(locator);
        }
        if (locator.startsWith("link=")) {
            return By.linkText(locator.substring(5));
        }
        return By.id(locator);
    }

    /**
     * Writes a simple HTML report summarizing executed Selenese test cases.
     */
    public static void writeHtmlReport(Path outputFile, List<TestResult> results) throws IOException {
        Files.createDirectories(outputFile.getParent());
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
                .append("  <meta charset=\"UTF-8\"/>\n")
                .append("  <title>Selenium HTML Test Report</title>\n")
                .append("  <style>body{font-family:sans-serif;margin:2rem;}table{border-collapse:collapse;width:100%;}")
                .append("th,td{border:1px solid #ddd;padding:8px;text-align:left;}.pass{color:#155724;}.fail{color:#721c24;}</style>\n")
                .append("</head>\n<body>\n")
                .append("<h1>Selenium HTML Test Report</h1>\n")
                .append("<p>Generated at: ").append(escapeHtml(Instant.now().toString())).append("</p>\n")
                .append("<table>\n<thead><tr><th>Test Case</th><th>Status</th><th>Duration (ms)</th><th>Details</th></tr></thead>\n<tbody>\n");
        for (TestResult result : results) {
            String statusClass = result.passed() ? "pass" : "fail";
            String statusText = result.passed() ? "PASSED" : "FAILED";
            html.append("<tr><td>").append(escapeHtml(result.name()))
                    .append("</td><td class=\"").append(statusClass).append("\">").append(statusText)
                    .append("</td><td>").append(result.durationMs())
                    .append("</td><td>").append(escapeHtml(result.message()))
                    .append("</td></tr>\n");
        }
        html.append("</tbody>\n</table>\n</body>\n</html>\n");
        Files.writeString(outputFile, html.toString(), StandardCharsets.UTF_8);
    }

    private static Document parseXml(Path xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            return builder.parse(xmlFile.toFile());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse XHTML file: " + xmlFile, e);
        }
    }

    private static String extractTitle(Document doc, Path fallbackFile) {
        NodeList titles = doc.getElementsByTagName("title");
        if (titles.getLength() > 0) {
            String text = titles.item(0).getTextContent().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return fallbackFile.getFileName().toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
