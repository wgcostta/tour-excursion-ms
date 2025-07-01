package com.tourapp.service;

import com.tourapp.dto.GoogleUserInfo;
import com.tourapp.dto.response.JwtResponse;
import com.tourapp.dto.response.UserInfoResponse;
import com.tourapp.entity.RoleEntity;
import com.tourapp.entity.UserEntity;
import com.tourapp.repository.RoleRepository;
import com.tourapp.repository.UserRepository;
import com.tourapp.config.security.GoogleTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService implements UserUseCase{

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GoogleTokenVerifier googleTokenVerifier;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public UserEntity processGoogleToken(String googleToken) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verifyWithGoogle(googleToken);
        if (googleUserInfo == null) {
            throw new RuntimeException("Invalid or expired token");
        }

        return findOrCreateUser(googleUserInfo);
    }

    private UserEntity findOrCreateUser(GoogleUserInfo googleUserInfo) {
        UserEntity user = userRepository.findByEmail(googleUserInfo.getEmail())
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(googleUserInfo.getEmail());
                    newUser.setFullName(googleUserInfo.getName());
                    newUser.setGoogleId(googleUserInfo.getSub());
                    newUser.setProfilePicture(googleUserInfo.getPicture());
                    newUser.setActive(true);
                    newUser.setCreatedAt(LocalDateTime.now());

                    // Assign USER role by default
                    RoleEntity userRole = roleRepository.findByName(RoleEntity.ROLE_USER)
                            .orElseGet(() -> {
                                RoleEntity newRole = new RoleEntity(RoleEntity.ROLE_USER);
                                return roleRepository.save(newRole);
                            });

                    newUser.getRoles().add(userRole);
                    return userRepository.save(newUser);
                });

        // Update user info if needed
        updateUserInfo(user, googleUserInfo);

        return user;
    }

    private void updateUserInfo(UserEntity user, GoogleUserInfo googleUserInfo) {
        boolean updated = false;

        if (!googleUserInfo.getSub().equals(user.getGoogleId())) {
            user.setGoogleId(googleUserInfo.getSub());
            updated = true;
        }

        if (!googleUserInfo.getName().equals(user.getFullName())) {
            user.setFullName(googleUserInfo.getName());
            updated = true;
        }

        if (!googleUserInfo.getPicture().equals(user.getProfilePicture())) {
            user.setProfilePicture(googleUserInfo.getPicture());
            updated = true;
        }

        user.setLastLogin(LocalDateTime.now());
        updated = true;

        if (updated) {
            userRepository.save(user);
        }
    }

    public JwtResponse buildJwtResponse(UserEntity user, String accessToken, String refreshToken) {
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        boolean hasActiveSubscription = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now());

        return new JwtResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roles,
                user.getProfilePicture(),
                user.getSubscriptionPlan(),
                hasActiveSubscription
        );
    }

    public UserInfoResponse getUserInfo(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setProfilePicture(user.getProfilePicture());
        response.setCreatedAt(user.getCreatedAt());
        response.setLastLogin(user.getLastLogin());
        response.setRoles(user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList()));
        response.setSubscriptionPlan(user.getSubscriptionPlan());
        response.setSubscriptionExpiry(user.getSubscriptionExpiry());
        response.setHasActiveSubscription(
                user.getSubscriptionExpiry() != null &&
                        user.getSubscriptionExpiry().isAfter(LocalDateTime.now())
        );

        return response;
    }

    public Map<String, Object> checkSubscription(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        boolean hasActiveSubscription = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now());

        Map<String, Object> result = new HashMap<>();
        result.put("hasActiveSubscription", hasActiveSubscription);
        result.put("subscriptionPlan", user.getSubscriptionPlan() != null ? user.getSubscriptionPlan() : "none");
        result.put("subscriptionExpiry", user.getSubscriptionExpiry() != null ?
                user.getSubscriptionExpiry().toString() : "");

        return result;
    }

    @Transactional
    public void updateSubscription(String email, String plan, int months) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        user.setSubscriptionPlan(plan);
        user.setSubscriptionExpiry(LocalDateTime.now().plusMonths(months));

        // Add PREMIUM role if not exists
        RoleEntity premiumRole = roleRepository.findByName(RoleEntity.ROLE_PREMIUM)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity(RoleEntity.ROLE_PREMIUM);
                    return roleRepository.save(newRole);
                });

        if (user.getRoles().stream().noneMatch(role -> role.getName().equals(RoleEntity.ROLE_PREMIUM))) {
            user.getRoles().add(premiumRole);
        }

        userRepository.save(user);
    }
}