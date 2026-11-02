package com.apisentinel.utils;


import com.apisentinel.dto.request.ProbeRequest;
import com.apisentinel.enums.HttpMethod;
import com.apisentinel.enums.ProbeOutcome;
import com.apisentinel.security.UnsafeUrlException;
import com.apisentinel.security.UrlSafetyValidator;
import com.apisentinel.security.SsrfProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class HttpProbeExecutor {

    private static final Logger log = LoggerFactory.getLogger(HttpProbeExecutor.class);
    private static final int MAX_BODY_BYTES = 1_048_576; // 1 MB

    private final HttpClient httpClient;
    private final UrlSafetyValidator urlSafetyValidator;
    private final SsrfProperties ssrfProperties;

    public HttpProbeExecutor(HttpClient httpClient,
                             UrlSafetyValidator urlSafetyValidator,
                             SsrfProperties ssrfProperties) {
        this.httpClient = httpClient;
        this.urlSafetyValidator = urlSafetyValidator;
        this.ssrfProperties = ssrfProperties;
    }

    public ProbeResult execute(ProbeRequest request) {
        long start = System.nanoTime();

        URI current;
        try {
            current = urlSafetyValidator.validate(request.url());
        } catch (UnsafeUrlException e) {
            return unsafe(e, start, false);
        }

        HttpMethod method = request.method();
        int redirects = 0;

        try {
            while (true) {
                // Poore check (redirects mila ke) ka ek hi time budget
                long remainingMs = request.timeoutMs() - elapsedMs(start);
                if (remainingMs <= 0) {
                    return timeoutResult(request, start);
                }

                HttpResponse<InputStream> response = httpClient.send(
                        buildRequest(current, method, remainingMs),
                        HttpResponse.BodyHandlers.ofInputStream());

                int status = response.statusCode();
                String location = response.headers().firstValue("Location").orElse(null);

                if (isRedirect(status) && location != null
                        && redirects < ssrfProperties.maxRedirects()) {
                    response.body().close();
                    try {
                        // Relative Location ("/ok") bhi sahi resolve hota hai, phir HAR hop validate
                        current = urlSafetyValidator.validate(current.resolve(location));
                    } catch (IllegalArgumentException e) {
                        return ProbeResult.failure(ProbeOutcome.INVALID_URL, elapsedMs(start),
                                "Redirect location is not a valid URL");
                    } catch (UnsafeUrlException e) {
                        return unsafe(e, start, true);
                    }
                    method = methodAfterRedirect(status, method);
                    redirects++;
                    continue;
                }
                return readBody(response, start);
            }

        } catch (HttpTimeoutException e) {
            return timeoutResult(request, start);
        } catch (ConnectException e) {
            log.debug("Connection failed: {}", e.getClass().getSimpleName());
            return ProbeResult.failure(ProbeOutcome.CONNECTION_FAILED, elapsedMs(start),
                    "Could not connect to target");
        } catch (IOException e) {
            log.warn("Probe I/O error: {}", e.getClass().getSimpleName());
            return ProbeResult.failure(ProbeOutcome.ERROR, elapsedMs(start),
                    "I/O error: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // interrupt flag wapas set karo
            return ProbeResult.failure(ProbeOutcome.ERROR, elapsedMs(start), "Probe interrupted");
        }
    }

    private ProbeResult readBody(HttpResponse<InputStream> response, long start) throws IOException {
        try (InputStream in = response.body()) {
            byte[] bytes = in.readNBytes(MAX_BODY_BYTES + 1);
            long duration = elapsedMs(start);

            if (bytes.length > MAX_BODY_BYTES) {
                return ProbeResult.failure(ProbeOutcome.RESPONSE_TOO_LARGE, duration,
                        "Response body exceeds " + MAX_BODY_BYTES + " bytes");
            }
            return ProbeResult.responseReceived(response.statusCode(), duration,
                    new String(bytes, StandardCharsets.UTF_8));
        }
    }

    private HttpRequest buildRequest(URI uri, HttpMethod method, long timeoutMs) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(timeoutMs))
                .header("User-Agent", "API-Sentinel/1.0")
                .method(method.name(), HttpRequest.BodyPublishers.noBody())
                .build();
    }

    private ProbeResult unsafe(UnsafeUrlException e, long start, boolean duringRedirect) {
        long duration = elapsedMs(start);
        return switch (e.reason()) {
            case INVALID_URL -> ProbeResult.failure(ProbeOutcome.INVALID_URL, duration,
                    duringRedirect ? "Redirect location is not a valid URL" : "Invalid URL");
            case BLOCKED_TARGET -> ProbeResult.failure(ProbeOutcome.BLOCKED_TARGET, duration,
                    duringRedirect ? "Redirect to a disallowed address was blocked"
                                   : "Target address is not allowed");
            case UNRESOLVABLE -> ProbeResult.failure(ProbeOutcome.CONNECTION_FAILED, duration,
                    "Could not resolve host");
        };
    }

    private ProbeResult timeoutResult(ProbeRequest request, long start) {
        return ProbeResult.failure(ProbeOutcome.TIMEOUT, elapsedMs(start),
                "No response within " + request.timeoutMs() + " ms");
    }

    private boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    /** 307/308 method preserve karte hain, baaki redirects GET ban jaate hain (browser jaisa). */
    private HttpMethod methodAfterRedirect(int status, HttpMethod current) {
        boolean preserves = status == 307 || status == 308 || current == HttpMethod.HEAD;
        return preserves ? current : HttpMethod.GET;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}