package com.company.invoice.api.controller;

import com.company.invoice.api.dto.request.LoginRequest;
import com.company.invoice.api.dto.request.RegisterRequest;
import com.company.invoice.api.dto.response.AuthResponse;
import com.company.invoice.api.dto.response.UserResponse;
import com.company.invoice.api.security.JwtTokenProvider;
import com.company.invoice.api.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and authorization endpoints")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "423", description = "Account locked")
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            log.info("🔐 Login attempt for: {}", loginRequest.getUsernameOrEmail());
            
            AuthResponse authResponse = authenticationService.login(loginRequest);
            
            log.info("✅ Login successful for: {}", loginRequest.getUsernameOrEmail());
            return ResponseEntity.ok(authResponse);
            
        } catch (BadCredentialsException e) {
            log.warn("❌ Login failed for: {} - {}", loginRequest.getUsernameOrEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid credentials", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Login error for: {} - {}", loginRequest.getUsernameOrEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Login failed", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Registration successful"),
        @ApiResponse(responseCode = "400", description = "Invalid registration data"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            log.info("📝 Registration attempt for: {}", registerRequest.getUsername());
            
            AuthResponse authResponse = authenticationService.register(registerRequest);
            
            log.info("✅ Registration successful for: {}", registerRequest.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (IllegalArgumentException e) {
            log.warn("❌ Registration failed for: {} - {}", registerRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Registration failed", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Registration error for: {} - {}", registerRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Registration failed", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Refresh JWT token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> refreshRequest) {
        try {
            String refreshToken = refreshRequest.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refresh token is required"));
            }
            
            log.info("🔄 Token refresh attempt");
            
            AuthResponse authResponse = authenticationService.refreshToken(refreshToken);
            
            log.info("✅ Token refreshed successfully");
            return ResponseEntity.ok(authResponse);
            
        } catch (BadCredentialsException e) {
            log.warn("❌ Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid refresh token", "message", e.getMessage()));
                
        } catch (Exception e) {
            log.error("🚨 Token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Token refresh failed", "message", "An unexpected error occurred"));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user (client should discard tokens)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserDetails userDetails) {
        // In a stateless JWT system, logout is typically handled client-side by discarding tokens
        // For enhanced security, you could implement a token blacklist here
        
        if (userDetails != null) {
            log.info("👋 User logout: {}", userDetails.getUsername());
        } else {
            log.info("👋 Anonymous logout");
        }
        
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User information retrieved"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
            }
            
            UserResponse userResponse = authenticationService.getCurrentUser(userDetails.getUsername());
            return ResponseEntity.ok(userResponse);
            
        } catch (Exception e) {
            log.error("🚨 Error getting current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get user information"));
        }
    }

    @PostMapping("/admin/create-golden-user")
    @Operation(summary = "Create golden admin user", description = "Create default admin user with BCrypt password")
    public ResponseEntity<?> createGoldenUser() {
        try {
            log.info("🏆 Creating golden admin user...");
            
            var goldenUser = authenticationService.createGoldenAdminUser();
            
            return ResponseEntity.ok(Map.of(
                "message", "Golden admin user created successfully",
                "username", goldenUser.getUsername(),
                "email", goldenUser.getEmail(),
                "role", goldenUser.getRole().name(),
                "note", "Password is 'password' (BCrypt hashed)"
            ));
            
        } catch (Exception e) {
            log.error("🚨 Error creating golden user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create golden user", "message", e.getMessage()));
        }
    }

    @PostMapping("/admin/create-default-users")
    @Operation(summary = "Create default users", description = "Create default users with different roles (ADMIN, APPROVER, USER)")
    public ResponseEntity<?> createDefaultUsers() {
        try {
            log.info("🚀 Creating default users for all roles...");
            
            authenticationService.createDefaultUsers();
            
            return ResponseEntity.ok(Map.of(
                "message", "Default users created successfully",
                "users", Map.of(
                    "admin", Map.of("username", "admin", "email", "admin@invoice.system", "role", "ADMIN", "password", "password"),
                    "approver", Map.of("username", "approver", "email", "approver@invoice.system", "role", "APPROVER", "password", "password"),
                    "user", Map.of("username", "user", "email", "user@invoice.system", "role", "USER", "password", "password")
                ),
                "note", "All passwords are 'password' (BCrypt hashed)"
            ));
            
        } catch (Exception e) {
            log.error("🚨 Error creating default users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create default users", "message", e.getMessage()));
        }
    }
}
