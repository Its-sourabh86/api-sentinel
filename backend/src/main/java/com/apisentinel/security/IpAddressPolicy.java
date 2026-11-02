package com.apisentinel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;

@Component
public class IpAddressPolicy {

    private static final Logger log = LoggerFactory.getLogger(IpAddressPolicy.class);

    /** Kisi bhi mode mein blocked: link-local (cloud metadata), unspecified, multicast, reserved. */
    private static final List<Cidr> ALWAYS_BLOCKED = Stream.of(
            "0.0.0.0/8", "169.254.0.0/16", "224.0.0.0/4", "240.0.0.0/4",
            "100.100.100.200/32",                       // Alibaba Cloud metadata
            "::/128", "fe80::/10", "ff00::/8",
            "fd00:ec2::254/128"                         // AWS IPv6 metadata
    ).map(Cidr::of).toList();

    /** Blocked jab tak allow-private-targets na ho. */
    private static final List<Cidr> PRIVATE = Stream.of(
            "127.0.0.0/8", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16",
            "100.64.0.0/10", "192.0.0.0/24", "198.18.0.0/15",
            "::1/128", "fc00::/7", "fec0::/10"
    ).map(Cidr::of).toList();

    private final boolean allowPrivateTargets;

    public IpAddressPolicy(SsrfProperties props) {
        this.allowPrivateTargets = props.allowPrivateTargets();
        if (allowPrivateTargets) {
            log.warn("SSRF guard: private and loopback targets are ALLOWED (dev mode). "
                    + "Never enable this in production.");
        }
    }

    public boolean isBlocked(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLinkLocalAddress()
                || address.isMulticastAddress() || matchesAny(ALWAYS_BLOCKED, address)) {
            return true;
        }
        if (allowPrivateTargets) {
            return false;
        }
        return address.isLoopbackAddress() || address.isSiteLocalAddress()
                || matchesAny(PRIVATE, address);
    }

    private static boolean matchesAny(List<Cidr> ranges, InetAddress address) {
        return ranges.stream().anyMatch(range -> range.contains(address));
    }

    /** "10.0.0.0/8" jaisa ek IP range. IPv4 aur IPv6 dono par kaam karta hai. */
    private record Cidr(byte[] network, int prefixBits) {

        static Cidr of(String text) {
            String[] parts = text.split("/");
            try {
                return new Cidr(InetAddress.getByName(parts[0]).getAddress(),
                        Integer.parseInt(parts[1]));
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Bad CIDR in SSRF policy: " + text, e);
            }
        }

        boolean contains(InetAddress candidate) {
            byte[] bytes = candidate.getAddress();
            if (bytes.length != network.length) {
                return false; // IPv4 ko IPv6 range se compare nahi karte
            }
            int fullBytes = prefixBits / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (bytes[i] != network[i]) return false;
            }
            int remainingBits = prefixBits % 8;
            if (remainingBits == 0) return true;

            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (bytes[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
