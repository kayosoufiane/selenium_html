# Selenium HTML Example (Modernized)

A modernized Java & Maven example project for executing Selenium IDE HTML (`.xhtml`) test suites using **Selenium 4 WebDriver**, **JUnit 6**, and **SLF4J / Logback**, ready for local execution and modern Jenkins CI pipelines.

> **Branch note:** Remote branch `origin/kayosoufiane-patch-1` was already merged into `master` via pull request #2.

## Modernized Stack

- **Java:** 17+ (`maven.compiler.release=17`)
- **Build Tool:** Apache Maven 3.9.16 via included Maven Wrapper (`mvnw` / `mvnw.cmd`, wrapper `3.3.4`)
- **Selenium:** `org.seleniumhq.selenium:selenium-java` `4.53.0` (uses Selenium Manager for automatic driver management and native headless mode — no Xvfb or manual WebDriver binaries required)
- **Testing Framework:** `org.junit.jupiter:junit-jupiter` `6.1.3`
- **Logging:** `ch.qos.logback:logback-classic` `1.6.8` (`src/test/resources/logback-test.xml`)
- **Maven Plugins:**
  - `maven-compiler-plugin` `3.16.0`
  - `maven-surefire-plugin` `3.6.1` (unit tests `*Test.java`)
  - `maven-failsafe-plugin` `3.6.1` (integration tests `*IT.java`)

## Project Structure

```text
selenium_html/
├── mvnw / mvnw.cmd
├── pom.xml
└── src/
    ├── main/java/nl/berg/packt/selenium/
    │   └── App.java
    └── test/
        ├── java/nl/berg/packt/selenium/
        │   ├── AppTest.java
        │   ├── SeleneseRunner.java
        │   ├── SeleneseRunnerTest.java
        │   └── SeleniumHtmlIT.java
        └── resources/
            ├── logback-test.xml
            └── selenium/
                ├── TestSuite.xhtml
                └── MyTest.xhtml
```

## Prerequisites

- **JDK 17** or newer (e.g. JDK 17 or JDK 21)
- A supported browser installed on the host or CI agent (**Firefox**, **Chrome**, or **Edge**) — Selenium Manager downloads the matching WebDriver automatically on first run

## Running Tests

### 1. Run Unit Tests

From the `selenium_html` directory:

**Linux / macOS:**
```bash
cd selenium_html
./mvnw test
```

**Windows (PowerShell):**
```powershell
cd selenium_html
.\mvnw.cmd test
```

### 2. Run Selenium HTML Integration Tests

Start your target web application (by default `http://localhost:8080/`), then run Maven `verify`:

```bash
cd selenium_html
./mvnw verify \
  -Dselenium.startUrl=http://localhost:8080/ \
  -Dselenium.browser=firefox \
  -Dselenium.headless=true
```

On Windows PowerShell (quote `-D` arguments containing dots):

```powershell
cd selenium_html
.\mvnw.cmd verify `
  "-Dselenium.startUrl=http://localhost:8080/" `
  "-Dselenium.browser=firefox" `
  "-Dselenium.headless=true"
```

#### Configurable System Properties

| Property | Default | Description |
| --- | --- | --- |
| `selenium.startUrl` | `http://localhost:8080/` | Base URL of the tested web application |
| `selenium.browser` | `firefox` | Target browser (`firefox`, `chrome`, or `edge`) |
| `selenium.headless` | `true` | Run browser in headless mode (`true` / `false`) |
| `selenium.suite` | `src/test/resources/selenium/TestSuite.xhtml` | Path to the Selenium IDE suite file |
| `selenium.timeoutSeconds` | `10` | Explicit WebDriverWait timeout in seconds |

#### Supported Selenese Commands

`SeleneseRunner` executes `.xhtml` test cases referenced by `<table id="suiteTable">` in `TestSuite.xhtml`:
- `open`
- `verifyTextPresent` / `assertTextPresent`
- `verifyTitle` / `assertTitle`
- `click` / `clickAndWait`
- `type`
- `waitForElementPresent`

#### Reports

- JUnit XML reports: `selenium_html/target/surefire-reports/` and `selenium_html/target/failsafe-reports/`
- HTML summary report: `selenium_html/target/results/selenium-report.html`

## Jenkins Pipeline Example

Modern Jenkins setups no longer need `Xvfb`, `selenium-maven-plugin`, or Log4j 1.x configuration flags. Use a declarative `Jenkinsfile` with JDK 17+ and a browser available on the agent:

```groovy
pipeline {
    agent any

    tools {
        jdk 'JDK-17'
    }

    stages {
        stage('Unit Tests') {
            steps {
                dir('selenium_html') {
                    sh './mvnw test'
                }
            }
        }
        stage('Selenium Integration Tests') {
            steps {
                dir('selenium_html') {
                    sh './mvnw verify -Dselenium.startUrl=http://localhost:8080/ -Dselenium.browser=firefox -Dselenium.headless=true'
                }
            }
        }
    }

    post {
        always {
            junit 'selenium_html/target/*-reports/*.xml'
            archiveArtifacts artifacts: 'selenium_html/target/results/selenium-report.html', allowEmptyArchive: true
        }
    }
}
```
