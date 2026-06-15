package com.edap.integration;

import com.edap.dto.LoginRequest;
import com.edap.dto.RegisterRequest;
import com.edap.entity.AppUser;
import com.edap.repository.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack integration test using H2 (test profile).
 * Verifies the login / register flow end to end, including JWT issuance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController integration tests")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedUser() {
        if (userRepository.findByUsername("integtest").isEmpty()) {
            AppUser user = AppUser.builder()
                    .username("integtest")
                    .email("integtest@edap.internal")
                    .passwordHash(passwordEncoder.encode("Test@1234"))
                    .roles(Set.of("ROLE_ENGINEER", "ROLE_VIEWER"))
                    .enabled(true)
                    .build();
            userRepository.saveAndFlush(user);
        }
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns JWT")
    void login_validCredentials_returnsJwt() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("integtest");
        request.setPassword("Test@1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("integtest"))
                .andExpect(jsonPath("$.data.roles", hasItem("ROLE_ENGINEER")));
    }

    @Test
    @DisplayName("POST /api/auth/login with wrong password returns 401")
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("integtest");
        request.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login with unknown user returns 401")
    void login_unknownUser_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("Test@1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/register creates a new user")
    void register_newUser_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@edap.internal");
        request.setPassword("Newuser@123");
        request.setRoles(Set.of("ROLE_VIEWER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate username returns 409")
    void register_duplicateUsername_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("integtest");   // already exists
        request.setEmail("other@edap.internal");
        request.setPassword("Other@1234");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login after registering returns valid JWT")
    void registerThenLogin_returnsJwt() throws Exception {
        // Register
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("freshuser");
        reg.setEmail("freshuser@edap.internal");
        reg.setPassword("Fresh@1234");
        reg.setRoles(Set.of("ROLE_VIEWER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login with freshly registered account
        LoginRequest login = new LoginRequest();
        login.setUsername("freshuser");
        login.setPassword("Fresh@1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("freshuser"));
    }
}
