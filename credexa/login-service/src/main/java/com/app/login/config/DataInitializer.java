package com.app.login.config;

import com.app.login.entity.BankConfiguration;
import com.app.login.entity.Role;
import com.app.login.entity.User;
import com.app.login.repository.BankConfigurationRepository;
import com.app.login.repository.RoleRepository;
import com.app.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Data initializer to create default roles, admin user, and bank configuration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BankConfigurationRepository bankConfigRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.create-default:true}")
    private boolean createDefaultAdmin;

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:Admin@123}")
    private String adminPassword;

    @Value("${admin.email:admin@credexa.com}")
    private String adminEmail;

    @Override
    public void run(String... args) {
        log.info("Initializing default data...");

        // Create default roles
        createDefaultRoles();

        // Create default admin user
        createDefaultAdminUser();

        // Create default bank configuration
        createDefaultBankConfiguration();

        log.info("Default data initialization completed");
    }

    private void createDefaultRoles() {
        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .description("Default role: " + roleName.name())
                        .build();
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    private void createDefaultAdminUser() {
        // Only create default admin if enabled (can be disabled in production)
        if (!createDefaultAdmin) {
            log.info("Default admin creation is disabled. Set admin.create-default=true to enable.");
            return;
        }

        if (!userRepository.existsByUsername(adminUsername)) {
            Role adminRole = roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));

            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .mobileNumber("9999999999")
                    .preferredLanguage("en")
                    .preferredCurrency("USD")
                    .active(true)
                    .accountLocked(false)
                    .failedLoginAttempts(0)
                    .createdBy("SYSTEM")
                    .roles(new HashSet<>())
                    .build();

            admin.getRoles().add(adminRole);
            userRepository.save(admin);

            log.warn("========================================");
            log.warn("⚠️  DEFAULT ADMIN USER CREATED");
            log.warn("Username: {}", adminUsername);
            log.warn("Email: {}", adminEmail);
            log.warn("⚠️  CHANGE THE PASSWORD IMMEDIATELY IN PRODUCTION!");
            log.warn("⚠️  Set admin.create-default=false to disable auto-creation");
            log.warn("========================================");
        }
    }

    private void createDefaultBankConfiguration() {
        if (bankConfigRepository.findByActiveTrue().isEmpty()) {
            BankConfiguration config = BankConfiguration.builder()
                    .bankName("Credexa Bank")
                    .logoUrl("/assets/logo.png")
                    .defaultLanguage("en")
                    .defaultCurrency("USD")
                    .currencyDecimalPlaces(2)
                    .active(true)
                    .build();
            
            bankConfigRepository.save(config);
            log.info("Created default bank configuration");
        }
    }
}
