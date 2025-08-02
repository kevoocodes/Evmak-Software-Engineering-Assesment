package com.evmak.parking_management.integration;

import com.evmak.parking_management.ParkingManagementApplication;
import com.evmak.parking_management.controller.AuthController;
import com.evmak.parking_management.entity.User;
import com.evmak.parking_management.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ParkingManagementApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        // Clean database before each test
        userRepository.deleteAll();
    }

    @Test
    void testUserRegistration_Success() throws Exception {
        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@test.com");
        request.setPhoneNumber("+255700123456");
        request.setPassword("TestPassword123");
        request.setRole(User.UserRole.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("john.doe@test.com"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void testUserRegistration_DuplicateEmail() throws Exception {
        // Create a user first
        User existingUser = new User();
        existingUser.setUsername("existing@test.com");
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        existingUser.setEmail("existing@test.com");
        existingUser.setPhoneNumber("+255700111222");
        existingUser.setPasswordHash(passwordEncoder.encode("password"));
        existingUser.setRole(User.UserRole.USER);
        existingUser.setIsActive(true);
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(existingUser);

        AuthController.RegisterRequest request = new AuthController.RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("existing@test.com");
        request.setPhoneNumber("+255700123456");
        request.setPassword("TestPassword123");
        request.setRole(User.UserRole.USER);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void testUserLogin_Success() throws Exception {
        // Create a user first
        User user = new User();
        user.setUsername("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPhoneNumber("+255700123456");
        user.setPasswordHash(passwordEncoder.encode("TestPassword123"));
        user.setRole(User.UserRole.USER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("TestPassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    void testUserLogin_InvalidCredentials() throws Exception {
        // Create a user first
        User user = new User();
        user.setUsername("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPhoneNumber("+255700123456");
        user.setPasswordHash(passwordEncoder.encode("CorrectPassword"));
        user.setRole(User.UserRole.USER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void testProtectedEndpoint_WithValidToken() throws Exception {
        // Register a user and get token
        AuthController.RegisterRequest registerRequest = new AuthController.RegisterRequest();
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPhoneNumber("+255700123456");
        registerRequest.setPassword("TestPassword123");
        registerRequest.setRole(User.UserRole.USER);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        AuthController.AuthResponse authResponse = objectMapper.readValue(responseContent, AuthController.AuthResponse.class);
        String token = authResponse.token;

        // Test protected endpoint
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void testProtectedEndpoint_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpoint_WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}