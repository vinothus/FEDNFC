package com.company.invoice.api.controller;

import com.company.invoice.api.dto.request.CreateUserRequest;
import com.company.invoice.api.dto.request.UpdateUserRequest;
import com.company.invoice.api.dto.response.UserResponse;
import com.company.invoice.data.entity.User;
import com.company.invoice.data.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for User Management operations
 * Provides CRUD operations for managing user accounts
 * Restricted to ADMIN role only
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Admin operations for managing user accounts")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users with pagination and sorting
     */
    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve paginated list of all users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active) {
        
        log.info("📋 Fetching users - page: {}, size: {}, sortBy: {}, search: {}", page, size, sortBy, search);
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<User> users;
            
            // Apply filters
            if (search != null && !search.trim().isEmpty()) {
                users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                    search, search, search, search, pageable);
            } else if (role != null && !role.trim().isEmpty()) {
                try {
                    User.Role userRole = User.Role.valueOf(role.toUpperCase());
                    users = userRepository.findByRole(userRole, pageable);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid role filter: {}", role);
                    users = userRepository.findAll(pageable);
                }
            } else if (active != null) {
                users = userRepository.findByIsActive(active, pageable);
            } else {
                users = userRepository.findAll(pageable);
            }
            
            Page<UserResponse> response = users.map(UserResponse::fromUser);
            
            log.info("✅ Retrieved {} users (total: {})", response.getNumberOfElements(), response.getTotalElements());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        log.info("🔍 Fetching user with ID: {}", id);
        
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            log.warn("❌ User not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        log.info("✅ Found user: {}", user.get().getUsername());
        return ResponseEntity.ok(UserResponse.fromUser(user.get()));
    }

    /**
     * Create new user
     */
    @PostMapping
    @Operation(summary = "Create new user", description = "Create a new user account")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("➕ Creating new user: {}", request.getUsername());
        
        try {
            // Check if username or email already exists
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("❌ Username already exists: {}", request.getUsername());
                return ResponseEntity.badRequest().build();
            }
            
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("❌ Email already exists: {}", request.getEmail());
                return ResponseEntity.badRequest().build();
            }
            
            // Parse and validate role
            User.Role userRole;
            try {
                userRole = User.Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role: {}", request.getRole());
                return ResponseEntity.badRequest().build();
            }
            
            // Create new user
            User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(userRole)
                .isActive(true)
                .isLocked(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();
            
            User savedUser = userRepository.save(user);
            
            log.info("✅ User created successfully: {}", savedUser.getUsername());
            return ResponseEntity.ok(UserResponse.fromUser(savedUser));
            
        } catch (Exception e) {
            log.error("❌ Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update existing user
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Update an existing user account")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        log.info("✏️ Updating user with ID: {}", id);
        
        try {
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                log.warn("❌ User not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            User user = optionalUser.get();
            
            // Check if new username/email conflicts with existing users (excluding current user)
            if (!user.getUsername().equals(request.getUsername()) && 
                userRepository.existsByUsername(request.getUsername())) {
                log.warn("❌ Username already exists: {}", request.getUsername());
                return ResponseEntity.badRequest().build();
            }
            
            if (!user.getEmail().equals(request.getEmail()) && 
                userRepository.existsByEmail(request.getEmail())) {
                log.warn("❌ Email already exists: {}", request.getEmail());
                return ResponseEntity.badRequest().build();
            }
            
            // Parse and validate role
            User.Role userRole;
            try {
                userRole = User.Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role: {}", request.getRole());
                return ResponseEntity.badRequest().build();
            }
            
            // Update user fields
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setRole(userRole);
            user.setIsActive(request.getIsActive());
            user.setUpdatedAt(LocalDateTime.now());
            
            // Update password if provided
            if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            }
            
            User savedUser = userRepository.save(user);
            
            log.info("✅ User updated successfully: {}", savedUser.getUsername());
            return ResponseEntity.ok(UserResponse.fromUser(savedUser));
            
        } catch (Exception e) {
            log.error("❌ Error updating user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete user (soft delete by deactivating)
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deactivate a user account")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("🗑️ Deleting user with ID: {}", id);
        
        try {
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                log.warn("❌ User not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            User user = optionalUser.get();
            
            // Soft delete by deactivating
            user.setIsActive(false);
            user.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(user);
            
            log.info("✅ User deactivated successfully: {}", user.getUsername());
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("❌ Error deleting user: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Toggle user lock status
     */
    @PostMapping("/{id}/toggle-lock")
    @Operation(summary = "Toggle user lock", description = "Lock or unlock a user account")
    public ResponseEntity<UserResponse> toggleUserLock(@PathVariable Long id) {
        log.info("🔒 Toggling lock status for user ID: {}", id);
        
        try {
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                log.warn("❌ User not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            User user = optionalUser.get();
            user.setIsLocked(!user.getIsLocked());
            
            // Reset login attempts when unlocking
            if (!user.getIsLocked()) {
                user.setFailedLoginAttempts(0);
            }
            
            user.setUpdatedAt(LocalDateTime.now());
            
            User savedUser = userRepository.save(user);
            
            log.info("✅ User lock status toggled: {} -> {}", 
                savedUser.getUsername(), savedUser.getIsLocked() ? "LOCKED" : "UNLOCKED");
            return ResponseEntity.ok(UserResponse.fromUser(savedUser));
            
        } catch (Exception e) {
            log.error("❌ Error toggling user lock: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Reset user password
     */
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset user password", description = "Reset a user's password to a temporary value")
    public ResponseEntity<Void> resetUserPassword(@PathVariable Long id, @RequestParam String newPassword) {
        log.info("🔑 Resetting password for user ID: {}", id);
        
        try {
            Optional<User> optionalUser = userRepository.findById(id);
            if (optionalUser.isEmpty()) {
                log.warn("❌ User not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            User user = optionalUser.get();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(user);
            
            log.info("✅ Password reset successfully for user: {}", user.getUsername());
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("❌ Error resetting password: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Get aggregate statistics about users")
    public ResponseEntity<UserStatsResponse> getUserStats() {
        log.info("📊 Fetching user statistics");
        
        try {
            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActive(true);
            long lockedUsers = userRepository.countByIsLocked(true);
            long adminUsers = userRepository.countByRole(User.Role.ADMIN);
            
            UserStatsResponse stats = UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(totalUsers - activeUsers)
                .lockedUsers(lockedUsers)
                .adminUsers(adminUsers)
                .approverUsers(userRepository.countByRole(User.Role.APPROVER))
                .regularUsers(userRepository.countByRole(User.Role.USER))
                .build();
            
            log.info("✅ User statistics: total={}, active={}, locked={}", totalUsers, activeUsers, lockedUsers);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("❌ Error fetching user statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * User statistics response DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class UserStatsResponse {
        private long totalUsers;
        private long activeUsers;
        private long inactiveUsers;
        private long lockedUsers;
        private long adminUsers;
        private long approverUsers;
        private long regularUsers;
    }
}
