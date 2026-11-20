package nl.berg.packt.selenium;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LocalTestServer}.
 */
class LocalTestServerTest {

    @Test
    void servesExpectedHtmlPageLocally() throws Exception {
        try (LocalTestServer server = LocalTestServer.start(0)) {
            String url = server.getBaseUrl() + "?from=%2F";
            assertTrue(LocalTestServer.isReachable(url, Duration.ofSeconds(1)));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("It works !"));
        }
    }

    @Test
    void reportsUnreachableWhenPortClosed() {
        assertFalse(LocalTestServer.isReachable("http://127.0.0.1:1/", Duration.ofMillis(200)));
    }
}
