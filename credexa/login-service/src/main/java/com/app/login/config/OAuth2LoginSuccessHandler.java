package com.app.login.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.app.common.util.JwtUtil;
import com.app.login.entity.Role;
import com.app.login.entity.Role.RoleName;
import com.app.login.entity.User;
import com.app.login.repository.RoleRepository;
import com.app.login.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for successful OAuth2 login
 * Creates user account if not exists and generates JWT token
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2AuthenticationToken oAuth2Token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oAuth2Token.getPrincipal();
        
        // Extract user information from OAuth2 provider
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String provider = oAuth2Token.getAuthorizedClientRegistrationId(); // "google"
        
        log.info("OAuth2 login successful for email: {} from provider: {}", email, provider);
        
        // Find or create user
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> createNewUser(email, name, provider));
        
        // Generate JWT token with roles
        String role = user.getRoles().isEmpty() ? "ROLE_CUSTOMER" : 
                      user.getRoles().iterator().next().getName().name();
        String token = jwtUtil.generateToken(user.getUsername(), 
            user.getRoles().stream().map(r -> r.getName().name()).toList());
        
        // Redirect to frontend with token and user info
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:5173/oauth2/redirect")
                .queryParam("token", token)
                .queryParam("userId", user.getId())
                .queryParam("username", user.getUsername())
                .queryParam("role", role)
                .queryParam("email", user.getEmail())
                .build().toUriString();
        
        log.info("Redirecting to: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
    
    /**
     * Create new user from OAuth2 information
     */
    private User createNewUser(String email, String name, String provider) {
        log.info("Creating new user from OAuth2: email={}, name={}, provider={}", email, name, provider);
        
        User newUser = new User();
        newUser.setEmail(email);
        
        // Generate username from email (before @ symbol)
        String username = email.split("@")[0];
        // Check if username already exists and make it unique if needed
        String finalUsername = username;
        int counter = 1;
        while (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = username + counter++;
        }
        newUser.setUsername(finalUsername);
        
        // Set a random password (won't be used for OAuth2 login)
        newUser.setPassword("OAUTH2_USER_NO_PASSWORD");
        newUser.setActive(true);
        newUser.setAccountLocked(false);
        newUser.setFailedLoginAttempts(0);
        
        // Assign CUSTOMER role by default
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
            .orElseThrow(() -> new RuntimeException("ROLE_CUSTOMER not found"));
        newUser.getRoles().add(customerRole);
        
        User savedUser = userRepository.save(newUser);
        log.info("Created new user with ID: {} and username: {}", savedUser.getId(), savedUser.getUsername());
        
        return savedUser;
    }
}
