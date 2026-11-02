package com.apisentinel.service;

import com.apisentinel.config.NotificationProperties;
import com.apisentinel.enums.FailureCode;
import com.apisentinel.utils.IncidentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSenderTest {

    private static final String SECRET = "test-secret";

    private HttpServer server;
    private final Deque<Integer> statuses = new ArrayDeque<>();
    private final AtomicInteger calls = new AtomicInteger();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicReference<String> receivedSignature = new AtomicReference<>();

    private final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/hook", ex -> {
            calls.incrementAndGet();
            receivedBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            receivedSignature.set(ex.getRequestHeaders().getFirst("X-Sentinel-Signature"));
            int status = statuses.isEmpty() ? 200 : statuses.poll();
            ex.sendResponseHeaders(status, -1);
            ex.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private WebhookSender sender(int maxAttempts) {
        String url = "http://localhost:" + server.getAddress().getPort() + "/hook";
        // Backoff 10 ms, taaki test tez chale
        return new WebhookSender(
                new NotificationProperties(true, url, SECRET, 2000, maxAttempts, 10), mapper);
    }

    private IncidentEvent event() {
        return new IncidentEvent(IncidentEvent.Type.OPENED, 7L, 1L, "Inventory API",
                FailureCode.JSON_ASSERTION_FAILED, "stock 0", 3,
                Instant.parse("2026-10-16T10:00:00Z"), null);
    }

    private String expectedSignature(String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("2xx par delivered, body JSON aur signature sahi")
    void deliversSignedJson() throws Exception {
        boolean ok = sender(3).send(event());

        assertThat(ok).isTrue();
        assertThat(calls.get()).isEqualTo(1);
        assertThat(receivedBody.get()).contains("\"type\":\"OPENED\"").contains("\"incidentId\":7");
        assertThat(receivedSignature.get()).isEqualTo(expectedSignature(receivedBody.get()));
    }

    @Test
    @DisplayName("5xx par retry hota hai aur aakhir mein success")
    void retriesServerErrors() {
        statuses.add(500);
        statuses.add(500);

        assertThat(sender(3).send(event())).isTrue();
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("4xx par retry nahi")
    void doesNotRetryClientErrors() {
        statuses.add(400);

        assertThat(sender(3).send(event())).isFalse();
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("hamesha 5xx: maxAttempts ke baad false")
    void givesUpAfterMaxAttempts() {
        statuses.add(500);
        statuses.add(500);
        statuses.add(500);

        assertThat(sender(3).send(event())).isFalse();
        assertThat(calls.get()).isEqualTo(3);
    }
}
