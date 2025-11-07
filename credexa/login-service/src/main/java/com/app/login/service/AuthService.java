package com.app.login.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.common.util.JwtUtil;
import com.app.login.client.CustomerServiceClient;
import com.app.login.dto.AdminCreateCustomerRequest;
import com.app.login.dto.AdminCreateCustomerResponse;
import com.app.login.dto.LoginRequest;
import com.app.login.dto.LoginResponse;
import com.app.login.dto.RegisterRequest;
import com.app.login.dto.TokenValidationResponse;
import com.app.login.entity.AuditLog;
import com.app.login.entity.Role;
import com.app.login.entity.User;
import com.app.login.entity.UserSession;
import com.app.login.event.LoginEvent;
import com.app.login.event.LoginEventPublisher;
import com.app.login.repository.AuditLogRepository;
import com.app.login.repository.RoleRepository;
import com.app.login.repository.UserRepository;
import com.app.login.repository.UserSessionRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for handling authentication and authorization
 */
@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository sessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomerServiceClient customerServiceClient;

    @Autowired(required = false)
    private LoginEventPublisher eventPublisher;

    @Autowired
    @Lazy
    private AuthService self; // Self-injection for transactional methods

    @Value("${jwt.expiration:3600000}")
    private Long jwtExpiration;

    @Autowired
    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                      UserSessionRepository sessionRepository, AuditLogRepository auditLogRepository,
                      PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                      CustomerServiceClient customerServiceClient) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.customerServiceClient = customerServiceClient;
    }

    /**
     * Register a new user - creates BOTH user account and customer profile
     * User creation happens in a transaction, then customer profile is created after commit
     */
    public User register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        // Create user account in a transaction (will be committed before customer profile creation)
        User savedUser = createUserAccount(request);
        
        log.info("User account created and committed: {} (userId: {}). Now creating customer profile...",
                savedUser.getUsername(), savedUser.getId());

        // Auto-create customer profile in customer-service (AFTER user transaction is committed)
        try {
            // Generate JWT token for inter-service communication
            List<String> roles = savedUser.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());
            String tempToken = jwtUtil.generateToken(savedUser.getUsername(), roles);

            CustomerServiceClient.CreateCustomerProfileRequest customerRequest =
                CustomerServiceClient.CreateCustomerProfileRequest.builder()
                    .username(savedUser.getUsername())
                    .fullName(request.getFullName())
                    .mobileNumber(request.getMobileNumber())
                    .email(request.getEmail())
                    .panNumber(request.getPanNumber())
                    .aadharNumber(request.getAadharNumber())
                    .dateOfBirth(request.getDateOfBirth())
                    .gender(request.getGender())
                    .classification(request.getClassification())
                    .addressLine1(request.getAddressLine1())
                    .addressLine2(request.getAddressLine2())
                    .city(request.getCity())
                    .state(request.getState())
                    .pincode(request.getPincode())
                    .country(request.getCountry())
                    .accountNumber(request.getAccountNumber())
                    .ifscCode(request.getIfscCode())
                    .preferredLanguage(request.getPreferredLanguage())
                    .preferredCurrency(request.getPreferredCurrency())
                    .emailNotifications(request.getEmailNotifications())
                    .smsNotifications(request.getSmsNotifications())
                    .build();

            customerServiceClient.createCustomerProfile(customerRequest, tempToken);
            log.info("✓ Customer profile created successfully for user: {}", savedUser.getUsername());

        } catch (Exception e) {
            log.error("✗ Failed to create customer profile for user: {}. User account exists but profile missing.",
                    savedUser.getUsername(), e);
            // Don't rollback user account - just log the error
            // Admin can manually create profile later if needed
        }

        return savedUser;
    }

    /**
     * Create user account in database (transactional)
     * This is separated so the transaction commits before we call customer-service
     */
    @Transactional
    private User createUserAccount(RegisterRequest request) {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getMobileNumber() != null &&
            userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalArgumentException("Mobile number already exists");
        }

        // Create user account
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hashing
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .preferredLanguage(request.getPreferredLanguage())
                .preferredCurrency(request.getPreferredCurrency())
                .active(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .createdBy(request.getUsername())
                .roles(new HashSet<>())
                .build();

        // Assign default role
        Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.getRoles().add(customerRole);

        User savedUser = userRepository.save(user);

        // Log the registration
        logAuditEvent(request.getUsername(), AuditLog.EventType.USER_REGISTERED,
                     true, "User registered successfully", null);

        return savedUser;
    }

    /**
     * Authenticate user and generate JWT token
     */
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Login attempt for: {}", request.getUsernameOrEmailOrMobile());

        String identifier = request.getUsernameOrEmailOrMobile();

        // Find user by username, email, or mobile
        User user = userRepository.findByUsernameOrEmailOrMobileNumber(identifier, identifier, identifier)
                .orElseThrow(() -> {
                    logAuditEvent(identifier, AuditLog.EventType.LOGIN_FAILURE, 
                                 false, "User not found", httpRequest);
                    return new UsernameNotFoundException("Invalid credentials");
                });

        // Check if account is locked
        if (user.isAccountLocked()) {
            logAuditEvent(user.getUsername(), AuditLog.EventType.LOGIN_FAILURE, 
                         false, "Account is locked", httpRequest);
            throw new BadCredentialsException("Account is locked. Please contact administrator.");
        }

        // Check if account is active
        if (!user.isActive()) {
            logAuditEvent(user.getUsername(), AuditLog.EventType.LOGIN_FAILURE, 
                         false, "Account is inactive", httpRequest);
            throw new BadCredentialsException("Account is inactive");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            self.handleFailedLogin(user, httpRequest); // Use self to trigger transaction proxy
            throw new BadCredentialsException("Invalid credentials");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        String token = jwtUtil.generateToken(user.getUsername(), roles);

        // Create session
        createUserSession(user, token, httpRequest);

        // Log successful login
        logAuditEvent(user.getUsername(), AuditLog.EventType.LOGIN_SUCCESS, 
                     true, "Login successful", httpRequest);

        // Publish login event to Kafka
        publishLoginEvent(user, "LOGIN_SUCCESS", httpRequest);

        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .preferredLanguage(user.getPreferredLanguage())
                .preferredCurrency(user.getPreferredCurrency())
                .loginTime(LocalDateTime.now())
                .expiresIn(jwtExpiration)
                .build();
    }

    /**
     * Logout user
     */
    @Transactional
    public void logout(String token) {
        String username = jwtUtil.extractUsername(token);
        
        sessionRepository.findBySessionToken(token).ifPresent(session -> {
            session.setActive(false);
            session.setLogoutTime(LocalDateTime.now());
            sessionRepository.save(session);
        });

        logAuditEvent(username, AuditLog.EventType.LOGOUT, true, "User logged out", null);
        
        // Publish logout event
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            publishLoginEvent(user, "LOGOUT", null);
        }
        
        log.info("User logged out: {}", username);
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        log.info("Retrieving user information for username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    /**
     * Validate JWT token
     */
    public TokenValidationResponse validateToken(String token) {
        try {
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.extractUsername(token);
                List<String> roles = jwtUtil.extractRoles(token);
                
                User user = userRepository.findByUsername(username)
                        .orElse(null);

                return TokenValidationResponse.builder()
                        .valid(true)
                        .username(username)
                        .userId(user != null ? user.getId() : null)
                        .roles(roles.toArray(new String[0]))
                        .message("Token is valid")
                        .build();
            }
        } catch (Exception e) {
            log.error("Token validation failed", e);
        }

        return TokenValidationResponse.builder()
                .valid(false)
                .message("Invalid or expired token")
                .build();
    }

    /**
     * Handle failed login attempts
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void handleFailedLogin(User user, HttpServletRequest httpRequest) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        // Lock account after 5 failed attempts
        if (user.getFailedLoginAttempts() >= 5) {
            user.setAccountLocked(true);
            logAuditEvent(user.getUsername(), AuditLog.EventType.ACCOUNT_LOCKED,
                         true, "Account locked due to multiple failed login attempts", httpRequest);
            log.warn("Account locked due to failed attempts: {}", user.getUsername());
        }

        userRepository.save(user);
        logAuditEvent(user.getUsername(), AuditLog.EventType.LOGIN_FAILURE,
                     false, "Invalid password", httpRequest);
    }

    /**
     * Unlock user account (Admin only)
     */
    @Transactional
    public void unlockAccount(String username, HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.isAccountLocked()) {
            throw new IllegalStateException("Account is not locked");
        }

        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        logAuditEvent(username, AuditLog.EventType.ACCOUNT_UNLOCKED,
                     true, "Account unlocked by administrator", httpRequest);
        log.info("Account unlocked: {}", username);
    }

    /**
     * Create user session
     */
    private void createUserSession(User user, String token, HttpServletRequest httpRequest) {
        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(token)
                .loginTime(LocalDateTime.now())
                .lastActivity(LocalDateTime.now())
                .active(true)
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .build();

        sessionRepository.save(session);
    }

    /**
     * Log audit event
     */
    private void logAuditEvent(String username, AuditLog.EventType eventType, 
                               boolean success, String message, HttpServletRequest httpRequest) {
        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .eventType(eventType)
                .success(success)
                .message(message)
                .eventTime(LocalDateTime.now())
                .build();

        if (httpRequest != null) {
            auditLog.setIpAddress(getClientIp(httpRequest));
            auditLog.setUserAgent(httpRequest.getHeader("User-Agent"));
        }

        auditLogRepository.save(auditLog);
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Publish login event to Kafka (if Kafka is enabled)
     */
    private void publishLoginEvent(User user, String eventType, HttpServletRequest request) {
        // Skip if Kafka is not configured
        if (eventPublisher == null) {
            log.debug("Kafka event publisher not available, skipping event publication");
            return;
        }

        try {
            LoginEvent event = LoginEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .userId(user.getId())
                    .eventType(eventType)
                    .eventTime(LocalDateTime.now())
                    .ipAddress(request != null ? getClientIp(request) : null)
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .build();

            eventPublisher.publishLoginEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish login event", e);
            // Don't fail the operation if Kafka is unavailable
        }
    }

    /**
     * Admin method to create user account only (no customer profile)
     * Called from customer-service when admin creates a customer profile
     */
    @Transactional
    public com.app.login.dto.CreateUserResponse adminCreateUserAccount(
            com.app.login.dto.CreateUserRequest request,
            String adminUsername) {
        log.info("Admin {} creating user account for username: {}", adminUsername, request.getUsername());

        // Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getMobileNumber() != null &&
            userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalArgumentException("Mobile number already exists");
        }

        // Generate temporary password if not provided
        String tempPassword = request.getTemporaryPassword();
        if (tempPassword == null || tempPassword.isBlank()) {
            tempPassword = generateTemporaryPassword();
            log.info("Generated temporary password for user: {}", request.getUsername());
        }

        // Create user account
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(tempPassword))
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .preferredLanguage(request.getPreferredLanguage())
                .preferredCurrency(request.getPreferredCurrency())
                .active(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .createdBy(adminUsername)
                .roles(new HashSet<>())
                .build();

        // Assign CUSTOMER role
        Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found"));
        user.getRoles().add(customerRole);

        User savedUser = userRepository.save(user);

        // Log the creation
        logAuditEvent(request.getUsername(), AuditLog.EventType.USER_REGISTERED,
                     true, "User account created by admin: " + adminUsername, null);

        log.info("User account created successfully for: {}", savedUser.getUsername());

        return com.app.login.dto.CreateUserResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .mobileNumber(savedUser.getMobileNumber())
                .temporaryPassword(tempPassword)
                .accountActive(savedUser.isActive())
                .message("User account created successfully. Temporary password: " + tempPassword)
                .build();
    }

    /**
     * Admin method to create customer with login account
     * Creates both user account and customer profile in one transaction
     */
    /**
     * Admin creates customer with login account - PUBLIC METHOD (NON-TRANSACTIONAL)
     * This method orchestrates the two-phase process:
     * 1. Create user account (transactional) - commits to DB
     * 2. Create customer profile (non-transactional) - inter-service call
     * 
     * CRITICAL: User must be committed to database BEFORE customer-service is called,
     * because customer-service will call back to login-service to fetch the userId.
     */
    public AdminCreateCustomerResponse adminCreateCustomerWithAccount(
            AdminCreateCustomerRequest request,
            String adminUsername) {
        log.info("=== ADMIN CUSTOMER CREATION INITIATED by {} for username: {} ===", adminUsername, request.getUsername());

        // Generate temporary password if not provided
        String tempPassword = request.getTemporaryPassword();
        if (tempPassword == null || tempPassword.isBlank()) {
            tempPassword = generateTemporaryPassword();
            log.info("Generated temporary password for user: {}", request.getUsername());
        }

        // PHASE 1: Create user account in transaction (will commit when method returns)
        log.info("PHASE 1: Creating user account for: {}", request.getUsername());
        User savedUser = createUserAccountForAdmin(request, adminUsername, tempPassword);
        log.info("✓ User account committed to database with ID: {}", savedUser.getId());

        // PHASE 2: Create customer profile (after user is committed to DB)
        log.info("PHASE 2: Creating customer profile for user: {}", savedUser.getUsername());
        
        // Generate JWT token for inter-service communication
        List<String> roles = savedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        String tempToken = jwtUtil.generateToken(savedUser.getUsername(), roles);

        CustomerServiceClient.CustomerProfileResponse customerProfile = null;
        try {
            CustomerServiceClient.CreateCustomerProfileRequest customerRequest =
                CustomerServiceClient.CreateCustomerProfileRequest.builder()
                    .username(savedUser.getUsername())
                    .fullName(request.getFullName())
                    .mobileNumber(request.getMobileNumber())
                    .email(request.getEmail())
                    .panNumber(request.getPanNumber())
                    .aadharNumber(request.getAadharNumber())
                    .dateOfBirth(request.getDateOfBirth())
                    .gender(request.getGender())
                    .classification(request.getClassification())
                    .addressLine1(request.getAddressLine1())
                    .addressLine2(request.getAddressLine2())
                    .city(request.getCity())
                    .state(request.getState())
                    .pincode(request.getPincode())
                    .country(request.getCountry())
                    .accountNumber(request.getAccountNumber())
                    .ifscCode(request.getIfscCode())
                    .preferredLanguage(request.getPreferredLanguage())
                    .preferredCurrency(request.getPreferredCurrency())
                    .emailNotifications(request.getEmailNotifications())
                    .smsNotifications(request.getSmsNotifications())
                    .build();

            customerProfile = customerServiceClient.createCustomerProfile(customerRequest, tempToken);
            log.info("✓ Customer profile created successfully with ID: {}", customerProfile.getId());
            log.info("=== ADMIN CUSTOMER CREATION COMPLETED SUCCESSFULLY ===");

        } catch (Exception e) {
            log.error("✗ Failed to create customer profile for user: {}", savedUser.getUsername(), e);
            log.warn("User account exists but customer profile creation failed. User ID: {}", savedUser.getId());
            // Note: User account is already committed - cannot rollback
            // Admin will need to either retry profile creation or manually create profile
            throw new RuntimeException("Failed to create customer profile. User account creation rolled back: " + e.getMessage());
        }

        // Build response
        return AdminCreateCustomerResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .mobileNumber(savedUser.getMobileNumber())
                .temporaryPassword(tempPassword)
                .accountActive(savedUser.isActive())
                .customerId(customerProfile.getId())
                .fullName(customerProfile.getFullName())
                .classification(customerProfile.getClassification())
                .kycStatus(customerProfile.getKycStatus())
                .message("Customer created successfully with login credentials. " +
                        "Temporary password: " + tempPassword + " (User must change on first login)")
                .build();
    }

    /**
     * Create user account for admin - PRIVATE TRANSACTIONAL METHOD
     * This method runs in its own transaction and commits immediately when it returns.
     * This ensures the user exists in the database before customer-service tries to fetch it.
     */
    @Transactional
    private User createUserAccountForAdmin(
            AdminCreateCustomerRequest request, 
            String adminUsername,
            String tempPassword) {
        
        log.info("Creating user account in transaction for: {}", request.getUsername());

        // Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (request.getMobileNumber() != null &&
            userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalArgumentException("Mobile number already exists");
        }

        // Create user account
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(tempPassword))
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .preferredLanguage(request.getPreferredLanguage())
                .preferredCurrency(request.getPreferredCurrency())
                .active(true)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .createdBy(adminUsername)
                .roles(new HashSet<>())
                .build();

        // Assign CUSTOMER role
        Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found"));
        user.getRoles().add(customerRole);

        User savedUser = userRepository.save(user);

        // Log the creation
        logAuditEvent(request.getUsername(), AuditLog.EventType.USER_REGISTERED,
                     true, "User account created by admin: " + adminUsername, null);

        log.info("User saved to database with ID: {} (transaction will commit when method returns)", savedUser.getId());
        return savedUser;
    }

    /**
     * Generate a temporary password
     */
    private String generateTemporaryPassword() {
        // Generate a random 12-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
