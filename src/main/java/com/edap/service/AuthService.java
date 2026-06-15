package com.edap.service;

import com.edap.dto.LoginRequest;
import com.edap.dto.LoginResponse;
import com.edap.dto.RegisterRequest;
import com.edap.entity.AppUser;
import com.edap.exception.ResourceAlreadyExistsException;
import com.edap.repository.AppUserRepository;
import com.edap.security.JwtTokenProvider;
import com.edap.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user '{}'", request.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        String token = tokenProvider.generateToken(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        log.info("User '{}' authenticated successfully", request.getUsername());
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .username(principal.getUsername())
                .roles(userRepository.findByUsername(principal.getUsername())
                        .map(AppUser::getRoles).orElse(Set.of()))
                .build();
    }

    @Transactional
    public void register(RegisterRequest request) {
        log.info("Registering user '{}'", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("User", "username", request.getUsername());
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User", "email", request.getEmail());
        }

        Set<String> roles = (request.getRoles() == null || request.getRoles().isEmpty())
                ? Set.of("ROLE_VIEWER")
                : request.getRoles();

        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("User '{}' registered with roles {}", request.getUsername(), roles);
    }
}
