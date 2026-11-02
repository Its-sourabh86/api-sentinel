package com.apisentinel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.apisentinel.security.UnsafeUrlException.Reason;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

@Component
public class UrlSafetyValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlSafetyValidator.class);

    private final IpAddressPolicy ipPolicy;

    public UrlSafetyValidator(IpAddressPolicy ipPolicy) {
        this.ipPolicy = ipPolicy;
    }

    public URI validate(String url) {
        if (url == null || url.isBlank()) {
            throw new UnsafeUrlException(Reason.INVALID_URL, "url is required");
        }
        try {
            return validate(new URI(url.trim()));
        } catch (URISyntaxException e) {
            throw new UnsafeUrlException(Reason.INVALID_URL, "url is not a valid URL");
        }
    }

    public URI validate(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new UnsafeUrlException(Reason.INVALID_URL, "url must use http or https");
        }
        if (uri.getUserInfo() != null) {
            throw new UnsafeUrlException(Reason.INVALID_URL, "url must not contain credentials");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException(Reason.INVALID_URL, "url must contain a valid host");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new UnsafeUrlException(Reason.UNRESOLVABLE, "host could not be resolved");
        }

        // Ek bhi address bad hai toh poora host reject
        for (InetAddress address : addresses) {
            if (ipPolicy.isBlocked(address)) {
                log.warn("SSRF guard blocked target host={}", host);
                throw new UnsafeUrlException(Reason.BLOCKED_TARGET,
                        "url points to a disallowed address");
            }
        }
        return uri;
    }
}
