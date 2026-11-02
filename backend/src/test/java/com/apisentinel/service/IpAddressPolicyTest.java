package com.apisentinel.service;

import com.apisentinel.security.IpAddressPolicy;
import com.apisentinel.security.SsrfProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class IpAddressPolicyTest {

    private final IpAddressPolicy strict = new IpAddressPolicy(new SsrfProperties(false, 0));
    private final IpAddressPolicy dev = new IpAddressPolicy(new SsrfProperties(true, 0));

    private static InetAddress ip(String literal) throws UnknownHostException {
        return InetAddress.getByName(literal); // literal hai, DNS nahi chalta
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.1.2.3", "172.16.0.1", "172.31.255.255",
            "192.168.1.1", "100.64.0.1", "::1", "fd12:3456::1",
            "::ffff:127.0.0.1", "::ffff:10.0.0.1"})
    @DisplayName("strict mode: private, loopback aur IPv4-mapped blocked")
    void strictBlocksPrivate(String literal) throws Exception {
        assertThat(strict.isBlocked(ip(literal))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.1.2.3", "192.168.1.1", "::1"})
    @DisplayName("dev mode: private allowed")
    void devAllowsPrivate(String literal) throws Exception {
        assertThat(dev.isBlocked(ip(literal))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"169.254.169.254", "169.254.170.2", "0.0.0.0", "224.0.0.1",
            "240.0.0.1", "255.255.255.255", "fe80::1", "ff02::1", "::",
            "fd00:ec2::254", "100.100.100.200"})
    @DisplayName("metadata, link-local, multicast: dev mode mein bhi blocked")
    void alwaysBlocked(String literal) throws Exception {
        assertThat(strict.isBlocked(ip(literal))).isTrue();
        assertThat(dev.isBlocked(ip(literal))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "93.184.216.34", "172.32.0.1",
            "172.15.255.255", "100.63.255.255", "100.128.0.1", "2606:4700:4700::1111"})
    @DisplayName("public IPs (aur range ke bilkul bahar wale) allowed")
    void publicAllowed(String literal) throws Exception {
        assertThat(strict.isBlocked(ip(literal))).isFalse();
    }
}