package nl.berg.packt.selenium;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Lightweight embedded HTTP server serving a local test page for Selenium HTML suites.
 */
public final class LocalTestServer implements AutoCloseable {

    private static final String DEFAULT_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <title>Selenium Local Test Page</title>
            </head>
            <body>
              <h1>It works !</h1>
              <p>Local test page served by selenium_html at <code>http://localhost:8080/</code>.</p>
            </body>
            </html>
            """;

    private final HttpServer server;

    private LocalTestServer(HttpServer server) {
        this.server = server;
    }

    /**
     * Starts an embedded HTTP server on the given port (use 0 for an ephemeral port).
     */
    public static LocalTestServer start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        byte[] body = DEFAULT_HTML.getBytes(StandardCharsets.UTF_8);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return new LocalTestServer(server);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public String getBaseUrl() {
        return "http://localhost:" + getPort() + "/";
    }

    /**
     * Checks whether a TCP service is already listening at the target URL host and port.
     */
    public static boolean isReachable(String url, Duration timeout) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), (int) timeout.toMillis());
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /**
     * Launches the local server on port 8080 (or the port passed as first argument / -Dserver.port).
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : Integer.parseInt(System.getProperty("server.port", "8080"));
        try (LocalTestServer localServer = start(port)) {
            System.out.println("Local test server running at " + localServer.getBaseUrl());
            System.out.println("Press Ctrl+C to stop.");
            CountDownLatch latch = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
            latch.await();
        }
    }
}
