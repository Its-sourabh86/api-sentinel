package com.apisentinel.service;

import com.apisentinel.dto.request.LoginRequest;
import com.apisentinel.dto.request.RegisterRequest;
import com.apisentinel.dto.response.LoginResponse;
import com.apisentinel.dto.response.UserResponse;
import com.apisentinel.entity.User;
import com.apisentinel.enums.Role;
import com.apisentinel.exception.AuthenticationFailedException;
import com.apisentinel.exception.ConflictException;
import com.apisentinel.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    // Cost 4: test tez chale. Production mein default (10) use hota hai.
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AuthService service() {
        return new AuthService(userRepository, encoder, jwtService);
    }

    @Test
    @DisplayName("register: email lowercase, role USER, password hashed (plain nahi)")
    void register_hashesPasswordAndForcesUserRole() {
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service().register(new RegisterRequest("  A@X.com ", "secret123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("a@x.com");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", saved.getPasswordHash())).isTrue();
        assertThat(response.email()).isEqualTo("a@x.com");
    }

    @Test
    @DisplayName("register: email pehle se hai toh Conflict aur save kabhi nahi")
    void register_duplicate_conflict() {
        when(userRepository.existsByEmail("a@x.com")).thenReturn(true);

        assertThatThrownBy(() -> service().register(new RegisterRequest("a@x.com", "secret123")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("register: race mein DB unique index ka error bhi Conflict ban jaye")
    void register_raceCondition_conflict() {
        when(userRepository.existsByEmail("a@x.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service().register(new RegisterRequest("a@x.com", "secret123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("login: sahi password par token mile")
    void login_success() {
        User user = new User("a@x.com", encoder.encode("secret123"), Role.USER);
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user));
        when(jwtService.issue(user)).thenReturn(new JwtService.IssuedToken("tok", 3600));

        LoginResponse response = service().login(new LoginRequest(" A@x.com ", "secret123"));

        assertThat(response.accessToken()).isEqualTo("tok");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    @DisplayName("login: galat password par 401 exception aur token kabhi nahi")
    void login_wrongPassword() {
        User user = new User("a@x.com", encoder.encode("secret123"), Role.USER);
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service().login(new LoginRequest("a@x.com", "wrong-pass")))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(jwtService, never()).issue(any());
    }

    @Test
    @DisplayName("login: unknown email aur galat password ka message bilkul same")
    void login_unknownEmail_sameMessage() {
        User user = new User("a@x.com", encoder.encode("secret123"), Role.USER);
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("nobody@x.com")).thenReturn(Optional.empty());

        Throwable wrongPassword = catchThrowable(
                () -> service().login(new LoginRequest("a@x.com", "wrong-pass")));
        Throwable unknownEmail = catchThrowable(
                () -> service().login(new LoginRequest("nobody@x.com", "whatever12")));

        assertThat(unknownEmail).isInstanceOf(AuthenticationFailedException.class);
        assertThat(unknownEmail.getMessage()).isEqualTo(wrongPassword.getMessage());
    }
}