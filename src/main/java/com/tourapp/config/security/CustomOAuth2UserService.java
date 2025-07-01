package com.tourapp.config.security;

import com.tourapp.entity.RoleEntity;
import com.tourapp.entity.UserEntity;
import com.tourapp.repository.RoleRepository;
import com.tourapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CustomOAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // Extract user information from Google
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String googleId = (String) attributes.get("sub");
        String pictureUrl = (String) attributes.get("picture");

        // Check if user exists or create new one
        Optional<UserEntity> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            updateExistingUser(user, googleId, name, pictureUrl);
        } else {
            createNewUser(email, googleId, name, pictureUrl);
        }

        return oAuth2User;
    }

    private void updateExistingUser(UserEntity user, String googleId, String name, String pictureUrl) {
        user.setGoogleId(googleId);
        user.setFullName(name);
        user.setProfilePicture(pictureUrl);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Existing user updated: {}", user.getEmail());
    }

    private void createNewUser(String email, String googleId, String name, String pictureUrl) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setFullName(name);
        user.setGoogleId(googleId);
        user.setProfilePicture(pictureUrl);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        // Assign USER role by default
        RoleEntity userRole = roleRepository.findByName(RoleEntity.ROLE_USER)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName(RoleEntity.ROLE_USER);
                    return roleRepository.save(newRole);
                });

        user.getRoles().add(userRole);
        userRepository.save(user);
        logger.info("New user created: {}", email);
    }
}