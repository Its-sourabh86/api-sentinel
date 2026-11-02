package com.apisentinel;

import com.apisentinel.config.JwtProperties;
import com.apisentinel.entity.User;
import com.apisentinel.enums.Role;
import com.apisentinel.security.AuthenticatedUser;
import com.apisentinel.security.JwtAuthenticationFilter;
import com.apisentinel.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private final JwtService jwtService =
            new JwtService(new JwtProperties(SECRET, 60), Clock.systemUTC());
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private String validToken(Role role) {
        User user = new User("a@x.com", "hash", role);
        ReflectionTestUtils.setField(user, "id", 7L);
        return jwtService.issue(user).value();
    }

    /** Filter chalao, aur chain aage badhi ya nahi wo lautao. */
    private boolean run(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain.getRequest() != null;
    }

    @Test
    @DisplayName("sahi token: user authenticated aur role authority mile")
    void validToken_authenticates() throws Exception {
        assertThat(run("Bearer " + validToken(Role.ADMIN))).isTrue();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(((AuthenticatedUser) auth.getPrincipal()).id()).isEqualTo(7L);
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("header nahi: authentication nahi, lekin request aage jaaye")
    void noHeader() throws Exception {
        assertThat(run(null)).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("kachra token: authentication nahi, request aage jaaye (401 entry point dega)")
    void garbageToken() throws Exception {
        assertThat(run("Bearer not.a.jwt")).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("tampered token reject")
    void tamperedToken() throws Exception {
        String[] parts = validToken(Role.USER).split("\\.");
        String tampered = parts[0] + "." + "X" + parts[1].substring(1) + "." + parts[2];

        assertThat(run("Bearer " + tampered)).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer ke alawa scheme (Basic) ignore")
    void otherScheme() throws Exception {
        assertThat(run("Basic dXNlcjpwYXNz")).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
