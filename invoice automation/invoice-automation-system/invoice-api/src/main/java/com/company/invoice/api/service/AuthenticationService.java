package com.company.invoice.api.service;

import com.company.invoice.api.dto.request.LoginRequest;
import com.company.invoice.api.dto.request.RegisterRequest;
import com.company.invoice.api.dto.response.AuthResponse;
import com.company.invoice.api.dto.response.UserResponse;
import com.company.invoice.api.security.JwtTokenProvider;
import com.company.invoice.data.entity.User;
import com.company.invoice.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Authentication service for user login, registration, and token management
 */
@Service
@Slf4j
@Transactional
public class AuthenticationService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(UserRepository userRepository, 
                               PasswordEncoder passwordEncoder,
                               JwtTokenProvider jwtTokenProvider,
                               @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * User login with username/email and password
     */
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            log.info("🔐 Login attempt for user: {}", loginRequest.getUsernameOrEmail());

            // Find user by username or email
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());
            if (userOpt.isEmpty()) {
                log.warn("❌ User not found: {}", loginRequest.getUsernameOrEmail());
                throw new BadCredentialsException("Invalid username or password");
            }

            User user = userOpt.get();

            // Check if account is locked
            if (user.getIsLocked()) {
                log.warn("🔒 Account is locked: {}", user.getUsername());
                throw new BadCredentialsException("Account is locked due to multiple failed login attempts");
            }

            // Check if account is active
            if (!user.getIsActive()) {
                log.warn("🚫 Account is inactive: {}", user.getUsername());
                throw new BadCredentialsException("Account is inactive");
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    user.getUsername(),
                    loginRequest.getPassword()
                )
            );

            // Generate tokens
            String accessToken = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

            // Update login success
            user.resetFailedLoginAttempts();
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            log.info("✅ Login successful for user: {}", user.getUsername());

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getTokenExpirationTime(accessToken))
                .user(UserResponse.fromUser(user))
                .build();

        } catch (AuthenticationException e) {
            // Handle failed login
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(loginRequest.getUsernameOrEmail());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.incrementFailedLoginAttempts();
                userRepository.save(user);
                log.warn("🚨 Failed login attempt #{} for user: {}", 
                    user.getFailedLoginAttempts(), user.getUsername());
            }

            log.error("❌ Authentication failed for: {}", loginRequest.getUsernameOrEmail());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    /**
     * User registration
     */
    public AuthResponse register(RegisterRequest registerRequest) {
        log.info("📝 Registration attempt for user: {}", registerRequest.getUsername());

        // Validate username uniqueness
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Validate email uniqueness
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = User.builder()
            .username(registerRequest.getUsername())
            .email(registerRequest.getEmail())
            .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
            .role(User.Role.USER) // Default role
            .firstName(registerRequest.getFirstName())
            .lastName(registerRequest.getLastName())
            .isActive(true)
            .isLocked(false)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateTokenFromUsername(savedUser.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getUsername());

        log.info("✅ User registered successfully: {}", savedUser.getUsername());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(jwtTokenProvider.getTokenExpirationTime(accessToken))
            .user(UserResponse.fromUser(savedUser))
            .build();
    }

    /**
     * Refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            if (!jwtTokenProvider.validateToken(refreshToken) || 
                !jwtTokenProvider.isRefreshToken(refreshToken)) {
                throw new BadCredentialsException("Invalid refresh token");
            }

            // Get username from token
            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            
            // Find user
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            // Check if user is still active
            if (!user.getIsActive() || user.getIsLocked()) {
                throw new BadCredentialsException("User account is not accessible");
            }

            // Generate new tokens
            String newAccessToken = jwtTokenProvider.generateTokenFromUsername(username);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            log.info("🔄 Token refreshed for user: {}", username);

            return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getTokenExpirationTime(newAccessToken))
                .user(UserResponse.fromUser(user))
                .build();

        } catch (Exception e) {
            log.error("❌ Token refresh failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }

    /**
     * Get current authenticated user
     */
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return UserResponse.fromUser(user);
    }

    /**
     * UserDetailsService implementation for Spring Security
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        log.debug("🔍 Loaded user details for: {}", username);
        return user;
    }

    /**
     * Create default users with different roles
     */
    @Transactional
    public void createDefaultUsers() {
        log.info("🚀 Creating default users for Invoice Automation System...");
        
        // Create Admin user
        createGoldenAdminUser();
        
        // Create Approver user
        createDefaultUser("approver", "approver@invoice.system", "password", User.Role.APPROVER, "Invoice", "Approver");
        
        // Create Regular user
        createDefaultUser("user", "user@invoice.system", "password", User.Role.USER, "Regular", "User");
        
        log.info("✅ All default users created successfully!");
    }

    /**
     * Create a user with specified role
     */
    @Transactional
    public User createDefaultUser(String username, String email, String password, User.Role role, String firstName, String lastName) {
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            log.info("👤 User already exists: {} ({})", username, role);
            return existingUser.get();
        }

        // Create user
        User user = User.builder()
            .username(username)
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .role(role)
            .firstName(firstName)
            .lastName(lastName)
            .isActive(true)
            .isLocked(false)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);
        
        log.info("✅ {} user created: {} | Email: {} | Password: password", 
            role.name(), savedUser.getUsername(), savedUser.getEmail());

        return savedUser;
    }

    /**
     * Create golden admin user with BCrypt password
     */
    @Transactional
    public User createGoldenAdminUser() {
        String goldenUsername = "admin";
        String goldenPassword = "password"; // Will be BCrypt hashed

        // Check if admin user already exists
        Optional<User> existingAdmin = userRepository.findByUsername(goldenUsername);
        if (existingAdmin.isPresent()) {
            log.info("🏆 Golden admin user already exists: {}", goldenUsername);
            return existingAdmin.get();
        }

        // Create golden admin user
        User goldenAdmin = User.builder()
            .username(goldenUsername)
            .email("admin@invoice.system")
            .passwordHash(passwordEncoder.encode(goldenPassword)) // BCrypt hash of "password"
            .role(User.Role.ADMIN)
            .firstName("System")
            .lastName("Administrator")
            .isActive(true)
            .isLocked(false)
            .failedLoginAttempts(0)
            .createdAt(LocalDateTime.now())
            .build();

        User savedAdmin = userRepository.save(goldenAdmin);
        
        log.info("✅ Golden admin user created successfully!");
        log.info("🔑 Username: {} | Password: {} (BCrypt hashed)", goldenUsername, goldenPassword);
        log.info("🏆 User ID: {} | Role: {}", savedAdmin.getId(), savedAdmin.getRole());

        return savedAdmin;
    }
}
