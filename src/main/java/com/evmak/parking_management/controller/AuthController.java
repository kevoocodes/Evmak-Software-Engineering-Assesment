package com.evmak.parking_management.controller;

import com.evmak.parking_management.entity.User;
import com.evmak.parking_management.repository.UserRepository;
import com.evmak.parking_management.security.CustomUserDetailsService;
import com.evmak.parking_management.security.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT authentication and user management")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    public static class LoginRequest {
        public String email;
        public String password;

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        @Valid
        public String firstName;
        public String lastName;
        public String email;
        public String phoneNumber;
        public String password;
        public User.UserRole role = User.UserRole.USER;

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public User.UserRole getRole() { return role; }
        public void setRole(User.UserRole role) { this.role = role; }
    }

    public static class AuthResponse {
        public final boolean success;
        public final String message;
        public final String token;
        public final String tokenType = "Bearer";
        public final Long expiresIn;
        public final UserInfo user;

        public AuthResponse(boolean success, String message, String token, Long expiresIn, UserInfo user) {
            this.success = success;
            this.message = message;
            this.token = token;
            this.expiresIn = expiresIn;
            this.user = user;
        }

        public static AuthResponse success(String token, Long expiresIn, UserInfo user) {
            return new AuthResponse(true, "Authentication successful", token, expiresIn, user);
        }

        public static AuthResponse failure(String message) {
            return new AuthResponse(false, message, null, null, null);
        }
    }

    public static class UserInfo {
        public Long id;
        public String firstName;
        public String lastName;
        public String email;
        public String phoneNumber;
        public String role;

        public UserInfo() {} // Default constructor for JSON deserialization

        public UserInfo(User user) {
            this.id = user.getId();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
            this.email = user.getEmail();
            this.phoneNumber = user.getPhoneNumber();
            this.role = user.getRole().toString();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            // Load user details
            final UserDetails userDetails = userDetailsService.loadUserByUsername(loginRequest.getEmail());
            
            // Get user from database
            Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.failure("User not found"));
            }
            
            User user = userOpt.get();
            
            // Generate JWT token
            final String token = jwtTokenUtil.generateToken(
                userDetails.getUsername(), 
                user.getRole().toString(), 
                user.getId()
            );

            return ResponseEntity.ok(AuthResponse.success(
                token, 
                jwtTokenUtil.getExpirationTime(), 
                new UserInfo(user)
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(AuthResponse.failure("Invalid credentials"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.failure("Authentication failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest registerRequest) {
        try {
            // Check if user already exists
            if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body(AuthResponse.failure("Email already registered"));
            }

            if (userRepository.findByPhoneNumber(registerRequest.getPhoneNumber()).isPresent()) {
                return ResponseEntity.badRequest().body(AuthResponse.failure("Phone number already registered"));
            }

            // Create new user
            User user = new User();
            user.setUsername(registerRequest.getEmail()); // Use email as username
            user.setFirstName(registerRequest.getFirstName());
            user.setLastName(registerRequest.getLastName());
            user.setEmail(registerRequest.getEmail());
            user.setPhoneNumber(registerRequest.getPhoneNumber());
            user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole(registerRequest.getRole());
            user.setIsActive(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            // Save user
            user = userRepository.save(user);

            // Generate JWT token
            final String token = jwtTokenUtil.generateToken(
                user.getEmail(), 
                user.getRole().toString(), 
                user.getId()
            );

            return ResponseEntity.ok(AuthResponse.success(
                token, 
                jwtTokenUtil.getExpirationTime(), 
                new UserInfo(user)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.failure("Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Refresh an existing JWT token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(AuthResponse.failure("Invalid token format"));
            }

            String token = authHeader.substring(7);
            
            if (!jwtTokenUtil.validateToken(token)) {
                return ResponseEntity.badRequest().body(AuthResponse.failure("Invalid or expired token"));
            }

            String username = jwtTokenUtil.getUsernameFromToken(token);
            Optional<User> userOpt = userRepository.findByEmail(username);
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(AuthResponse.failure("User not found"));
            }

            User user = userOpt.get();
            
            // Generate new token
            String newToken = jwtTokenUtil.generateToken(
                user.getEmail(), 
                user.getRole().toString(), 
                user.getId()
            );

            return ResponseEntity.ok(AuthResponse.success(
                newToken, 
                jwtTokenUtil.getExpirationTime(), 
                new UserInfo(user)
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.failure("Token refresh failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseEntity<Object> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body("Invalid token format");
            }

            String token = authHeader.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            
            Optional<User> userOpt = userRepository.findByEmail(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(new UserInfo(userOpt.get()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get user info: " + e.getMessage());
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Validate if JWT token is still valid")
    public ResponseEntity<Object> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body(new Object() {
                    public final boolean valid = false;
                    public final String message = "Invalid token format";
                });
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtTokenUtil.validateToken(token);

            return ResponseEntity.ok(new Object() {
                public final boolean valid = isValid;
                public final String message = isValid ? "Token is valid" : "Token is invalid or expired";
                public final Long expiresIn = isValid ? jwtTokenUtil.getExpirationTime() : 0L;
            });

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new Object() {
                public final boolean valid = false;
                public final String message = "Token validation failed: " + e.getMessage();
            });
        }
    }
}