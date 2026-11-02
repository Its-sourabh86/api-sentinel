package com.apisentinel.service;

import com.apisentinel.dto.request.LoginRequest;
import com.apisentinel.dto.response.LoginResponse;
import com.apisentinel.dto.request.RegisterRequest;
import com.apisentinel.dto.response.UserResponse;
import com.apisentinel.entity.User;
import com.apisentinel.enums.Role;
import com.apisentinel.exception.AuthenticationFailedException;
import com.apisentinel.exception.ConflictException;
import com.apisentinel.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Email na mile tab bhi BCrypt chalane ke liye (timing attack se bachne ko)
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("email is already registered");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), Role.USER);
        try {
            User saved = userRepository.saveAndFlush(user);
            log.info("User registered userId={}", saved.getId());
            return UserResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("email is already registered");
        }
    }

    // Jaan-boojh kar @Transactional nahi: BCrypt slow hai, DB connection phasa na rahe
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> found = userRepository.findByEmail(email);

        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash); // sirf time barabar karne ke liye
            log.warn("Login failed: unknown account");
            throw new AuthenticationFailedException();
        }

        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login failed userId={}", user.getId());
            throw new AuthenticationFailedException();
        }

        JwtService.IssuedToken token = jwtService.issue(user);
        log.info("Login succeeded userId={}", user.getId());
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
    }
}