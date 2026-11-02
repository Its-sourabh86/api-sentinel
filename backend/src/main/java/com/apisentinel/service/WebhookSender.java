package com.apisentinel.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.apisentinel.config.NotificationProperties;
import com.apisentinel.utils.IncidentEvent;
import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;

@Component
public class WebhookSender {

    private static final Logger log = LoggerFactory.getLogger(WebhookSender.class);

    private final NotificationProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI uri;

    public WebhookSender(NotificationProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.uri = props.enabled() ? URI.create(props.webhookUrl()) : null;
        // Probe wale HttpClient bean se alag, taaki beans mein confusion na ho
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(props.timeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        if (props.enabled() && isBlank(props.webhookSecret())) {
            log.warn("Webhook secret is not set, payloads will be sent unsigned");
        }
    }

    /** true = delivered (2xx). false = chhod diya (permanent error ya retries khatam). */
    public boolean send(IncidentEvent event) {
        String body;
        try {
            body = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Could not serialize incident event incidentId={}", event.incidentId(), e);
            return false;
        }

        for (int attempt = 1; attempt <= props.maxAttempts(); attempt++) {
            try {
                HttpResponse<Void> response =
                        httpClient.send(buildRequest(body), HttpResponse.BodyHandlers.discarding());
                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    log.info("Webhook delivered type={} incidentId={} attempt={}",
                            event.type(), event.incidentId(), attempt);
                    return true;
                }
                if (status >= 400 && status < 500 && status != 429) {
                    // Request hi galat hai, retry se kuch nahi badlega
                    log.warn("Webhook rejected, not retrying status={} incidentId={}",
                            status, event.incidentId());
                    return false;
                }
                log.warn("Webhook attempt failed status={} attempt={}/{} incidentId={}",
                        status, attempt, props.maxAttempts(), event.incidentId());

            } catch (IOException e) {
                log.warn("Webhook attempt failed error={} attempt={}/{} incidentId={}",
                        e.getClass().getSimpleName(), attempt, props.maxAttempts(), event.incidentId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // flag wapas set karo
                return false;
            }

            if (attempt < props.maxAttempts() && !waitBeforeRetry(attempt)) {
                return false;
            }
        }

        log.error("Webhook delivery FAILED after {} attempts type={} incidentId={}",
                props.maxAttempts(), event.type(), event.incidentId());
        return false;
    }

    private HttpRequest buildRequest(String body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(props.timeoutMs()))
                .header("Content-Type", "application/json")
                .header("User-Agent", "API-Sentinel/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (!isBlank(props.webhookSecret())) {
            builder.header("X-Sentinel-Signature", sign(body));
        }
        return builder.build();
    }

    /** Exponential backoff: base, 2x base, 4x base... false = thread interrupt hua, ruk jao. */
    private boolean waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(props.retryBackoffMs() * (1L << (attempt - 1)));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    props.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
