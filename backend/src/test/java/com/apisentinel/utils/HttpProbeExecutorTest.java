package com.apisentinel.utils;

import com.apisentinel.dto.request.ProbeRequest;
import com.apisentinel.enums.HttpMethod;
import com.apisentinel.enums.ProbeOutcome;
import com.apisentinel.security.IpAddressPolicy;
import com.apisentinel.security.SsrfProperties;
import com.apisentinel.security.UrlSafetyValidator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HttpProbeExecutorTest {

    private HttpServer server;
    private String baseUrl;
    private HttpProbeExecutor executor;

    private HttpProbeExecutor newExecutor(boolean allowPrivate, int maxRedirects) {
        SsrfProperties ssrf = new SsrfProperties(allowPrivate, maxRedirects);
        return new HttpProbeExecutor(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                new UrlSafetyValidator(new IpAddressPolicy(ssrf)),
                ssrf);
    }

    private static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().add("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/ok", ex -> {
            byte[] response = "{\"status\":\"UP\",\"stock\":10}".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, response.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(response);
            }
        });

        server.createContext("/slow", ex -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
            byte[] response = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, response.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(response);
            }
        });

        server.createContext("/redir", ex -> redirect(ex, "/ok"));
        server.createContext("/loop", ex -> redirect(ex, "/loop"));
        server.createContext("/to-metadata", ex -> redirect(ex, "http://169.254.169.254/latest"));

        server.start();

        baseUrl = "http://localhost:" + server.getAddress().getPort();
        executor = newExecutor(true, 3);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("200 OK par response_received aur body aati hai")
    void ok() {
        ProbeResult r = executor.execute(new ProbeRequest(baseUrl + "/ok", HttpMethod.GET, 2000));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.RESPONSE_RECEIVED);
        assertThat(r.httpStatus()).isEqualTo(200);
        assertThat(r.body()).contains("\"stock\":10");
    }

    @Test
    @DisplayName("timeout par TIMEOUT outcome")
    void timeout() {
        ProbeResult r = executor.execute(new ProbeRequest(baseUrl + "/slow", HttpMethod.GET, 200));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.TIMEOUT);
    }

    @Test
    @DisplayName("band port par CONNECTION_FAILED")
    void connectionRefused() {
        ProbeResult r = executor.execute(new ProbeRequest("http://localhost:59999/ok", HttpMethod.GET, 2000));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.CONNECTION_FAILED);
    }

    @Test
    @DisplayName("invalid scheme par INVALID_URL")
    void invalidScheme() {
        ProbeResult r = executor.execute(new ProbeRequest("ftp://localhost/ok", HttpMethod.GET, 2000));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.INVALID_URL);
    }

    @Test
    @DisplayName("redirect follow hota hai, relative Location bhi")
    void followsRelativeRedirect() {
        ProbeResult r = executor.execute(new ProbeRequest(baseUrl + "/redir", HttpMethod.GET, 2000));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.RESPONSE_RECEIVED);
        assertThat(r.httpStatus()).isEqualTo(200);
        assertThat(r.body()).contains("\"stock\":10");
    }

    @Test
    @DisplayName("redirect metadata IP par ja raha ho toh BLOCKED_TARGET")
    void redirectToMetadataIsBlocked() {
        ProbeResult r = executor.execute(new ProbeRequest(baseUrl + "/to-metadata", HttpMethod.GET, 2000));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.BLOCKED_TARGET);
        assertThat(r.errorMessage()).contains("Redirect");
    }

    @Test
    @DisplayName("redirect loop: hop limit ke baad aakhri 302 wapas")
    void redirectLoopStopsAtLimit() {
        ProbeResult r = executor.execute(new ProbeRequest(baseUrl + "/loop", HttpMethod.GET, 2000));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.RESPONSE_RECEIVED);
        assertThat(r.httpStatus()).isEqualTo(302);
    }

    @Test
    @DisplayName("maxRedirects=0: redirect follow nahi hota")
    void redirectsDisabled() {
        ProbeResult r = newExecutor(true, 0)
                .execute(new ProbeRequest(baseUrl + "/redir", HttpMethod.GET, 2000));

        assertThat(r.httpStatus()).isEqualTo(302);
    }

    @Test
    @DisplayName("strict mode mein localhost target hi blocked")
    void strictModeBlocksLocalhost() {
        ProbeResult r = newExecutor(false, 3)
                .execute(new ProbeRequest(baseUrl + "/ok", HttpMethod.GET, 2000));

        assertThat(r.outcome()).isEqualTo(ProbeOutcome.BLOCKED_TARGET);
    }
}

