package com.apisentinel.service;

import com.apisentinel.security.UnsafeUrlException.Reason;
import com.apisentinel.security.UrlSafetyValidator;
import com.apisentinel.security.IpAddressPolicy;
import com.apisentinel.security.SsrfProperties;
import com.apisentinel.security.UnsafeUrlException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlSafetyValidatorTest {

    private final UrlSafetyValidator strict =
            new UrlSafetyValidator(new IpAddressPolicy(new SsrfProperties(false, 0)));
    private final UrlSafetyValidator dev =
            new UrlSafetyValidator(new IpAddressPolicy(new SsrfProperties(true, 0)));

    /** null = URL allowed. */
    private Reason reasonFor(UrlSafetyValidator validator, String url) {
        try {
            validator.validate(url);
            return null;
        } catch (UnsafeUrlException e) {
            return e.reason();
        }
    }

    @Test
    @DisplayName("ftp, file, credentials, bina host, kachra: INVALID_URL")
    void invalidUrls() {
        assertThat(reasonFor(strict, "ftp://example.com/x")).isEqualTo(Reason.INVALID_URL);
        assertThat(reasonFor(strict, "file:///etc/passwd")).isEqualTo(Reason.INVALID_URL);
        assertThat(reasonFor(strict, "http://user:pw@example.com/")).isEqualTo(Reason.INVALID_URL);
        assertThat(reasonFor(strict, "http:///path")).isEqualTo(Reason.INVALID_URL);
        assertThat(reasonFor(strict, "not a url")).isEqualTo(Reason.INVALID_URL);
        assertThat(reasonFor(strict, "  ")).isEqualTo(Reason.INVALID_URL);
    }

    @Test
    @DisplayName("cloud metadata IP dono modes mein BLOCKED_TARGET")
    void metadataBlocked() {
        assertThat(reasonFor(strict, "http://169.254.169.254/latest/meta-data/"))
                .isEqualTo(Reason.BLOCKED_TARGET);
        assertThat(reasonFor(dev, "http://169.254.169.254/latest/meta-data/"))
                .isEqualTo(Reason.BLOCKED_TARGET);
    }

    @Test
    @DisplayName("localhost aur IPv6 loopback: strict mein blocked, dev mein allowed")
    void loopbackDependsOnMode() {
        assertThat(reasonFor(strict, "http://localhost:8080/x")).isEqualTo(Reason.BLOCKED_TARGET);
        assertThat(reasonFor(strict, "http://[::1]:8080/x")).isEqualTo(Reason.BLOCKED_TARGET);
        assertThat(reasonFor(dev, "http://localhost:8080/x")).isNull();
    }

    @Test
    @DisplayName("public IP literal allowed")
    void publicAllowed() {
        assertThat(reasonFor(strict, "https://8.8.8.8/")).isNull();
    }

    @Test
    @DisplayName("numeric tricks (127.1, decimal, hex) kisi bhi reason se reject hon")
    void numericTricksRejected() {
        // Kaunsa reason aayega wo Java ke URI parsing par depend karta hai, lekin allow kabhi nahi
        assertThat(reasonFor(strict, "http://127.1/")).isNotNull();
        assertThat(reasonFor(strict, "http://2130706433/")).isNotNull();
        assertThat(reasonFor(strict, "http://0x7f.0.0.1/")).isNotNull();
    }
}
