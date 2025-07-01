package com.tourapp.service;

import com.tourapp.config.security.GoogleTokenVerifier;
import com.tourapp.dto.GoogleUserInfo;
import com.tourapp.dto.response.JwtResponse;
import com.tourapp.dto.response.UserInfoResponse;
import com.tourapp.entity.RoleEntity;
import com.tourapp.entity.UserEntity;
import com.tourapp.repository.RoleRepository;
import com.tourapp.repository.UserRepository;
import com.tourapp.util.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService implements UserUseCase {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final RefreshTokenService refreshTokenService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            JwtUtils jwtUtils,
            GoogleTokenVerifier googleTokenVerifier,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtUtils = jwtUtils;
        this.googleTokenVerifier = googleTokenVerifier;
        this.refreshTokenService = refreshTokenService;
    }

    // Método para processar token do Google e retornar usuário e UserDetails
    public UserEntity processGoogleToken(String googleToken) {
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(googleToken);
        if (googleUserInfo == null) {
            throw new RuntimeException("Token inválido ou expirado");
        }

        // Criar ou atualizar usuário
        UserEntity user = findOrCreateUser(googleUserInfo);

        // Criar UserDetails
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("")
                .authorities(authorities)
                .build();

        return user;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return jwtUtils.generateJwtToken(userDetails);
    }

    // Método para construir a resposta JWT
    public JwtResponse buildJwtResponse(UserEntity user, String accessToken, String refreshToken) {
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        boolean hasActiveSubscription = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now());

        return new JwtResponse(
                accessToken,
                refreshToken,
                user.getId() != null ? user.getId() : 0,
                user.getEmail(),
                user.getFullName(),
                roles,
                user.getProfilePicture(),
                user.getSubscriptionPlan(),
                hasActiveSubscription
        );
    }

    // Método para encontrar ou criar usuário baseado nas informações do Google
    private UserEntity findOrCreateUser(GoogleUserInfo googleUserInfo) {
        UserEntity user = userRepository.findByEmail(googleUserInfo.getEmail())
                .orElseGet(() -> {
                    // Cria novo usuário
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(googleUserInfo.getEmail());
                    newUser.setFullName(googleUserInfo.getName());
                    newUser.setGoogleId(googleUserInfo.getSub());
                    newUser.setProfilePicture(googleUserInfo.getPicture());
                    newUser.setActive(true);

                    // Atribui papel UserEntity por padrão
                    RoleEntity userRole = roleRepository.findByName(RoleEntity.ROLE_USER)
                            .orElseGet(() -> {
                                RoleEntity newRole = new RoleEntity();
                                newRole.setName(RoleEntity.ROLE_USER);
                                return roleRepository.save(newRole);
                            });

                    newUser.getRoles().add(userRole);
                    return userRepository.save(newUser);
                });

        // Atualiza dados do usuário, se necessário
        updateUserInfo(user, googleUserInfo);

        return user;
    }

    @Transactional
    public JwtResponse validateToken(String googleToken) {
        // Verifica o token do Google
        GoogleUserInfo googleUserInfo = googleTokenVerifier.verify(googleToken);
        if (googleUserInfo == null) {
            throw new RuntimeException("Token inválido ou expirado");
        }

        logger.info("Token do Google validado para o usuário: {}", googleUserInfo.getEmail());

        // Verificando se o usuário já existe ou criando um novo
        UserEntity user = userRepository.findByEmail(googleUserInfo.getEmail())
                .orElseGet(() -> {
                    // Cria novo usuário
                    UserEntity newUser = new UserEntity();
                    newUser.setEmail(googleUserInfo.getEmail());
                    newUser.setFullName(googleUserInfo.getName());
                    newUser.setGoogleId(googleUserInfo.getSub());
                    newUser.setProfilePicture(googleUserInfo.getPicture());
                    newUser.setActive(true);

                    // Atribui papel UserEntity por padrão
                    RoleEntity userRole = roleRepository.findByName(RoleEntity.ROLE_USER)
                            .orElseGet(() -> {
                                RoleEntity newRole = new RoleEntity();
                                newRole.setName(RoleEntity.ROLE_USER);
                                return roleRepository.save(newRole);
                            });

                    newUser.getRoles().add(userRole);
                    return userRepository.save(newUser);
                });

        // Atualiza dados do usuário, se necessário
        updateUserInfo(user, googleUserInfo);

        // Cria um UserDetails para gerar o token JWT
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("")
                .authorities(authorities)
                .build();

        // Gera token JWT
        String token = jwtUtils.generateJwtToken(userDetails);

        // Gera refresh token
        var refreshToken = refreshTokenService.createRefreshToken(user.getEmail(), userDetails);

        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        boolean hasActiveSubscription = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now());

        return new JwtResponse(
                token,
                refreshToken.getToken(),
                user.getId() != null ? user.getId() : 0,
                user.getEmail(),
                user.getFullName(),
                roles,
                user.getProfilePicture(),
                user.getSubscriptionPlan(),
                hasActiveSubscription
        );
    }

    @Override
    public UserDetails loadUserDetailsByEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("")
                .authorities(authorities)
                .build();
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

        if (googleUserInfo.getPicture() != null &&
                !googleUserInfo.getPicture().equals(user.getProfilePicture())) {
            user.setProfilePicture(googleUserInfo.getPicture());
            updated = true;
        }

        // Atualiza o último login
        user.setLastLogin(LocalDateTime.now());
        updated = true;

        if (updated) {
            userRepository.save(user);
        }
    }

    public UserInfoResponse getUserInfo(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));

        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        boolean hasActiveSubscription = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now());

        return new UserInfoResponse(
                user.getId() != null ? user.getId() : 0,
                user.getEmail(),
                user.getFullName(),
                user.getProfilePicture(),
                user.getCreatedAt(),
                user.getLastLogin(),
                roles,
                user.getSubscriptionPlan(),
                user.getSubscriptionExpiry(),
                hasActiveSubscription
        );
    }

    public Map<String, Object> checkSubscription(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));

        boolean hasActiveSubscription = user.getSubscriptionExpiry() != null &&
                user.getSubscriptionExpiry().isAfter(LocalDateTime.now());

        return Map.of(
                "hasActiveSubscription", hasActiveSubscription,
                "subscriptionPlan", user.getSubscriptionPlan() != null ? user.getSubscriptionPlan() : "none",
                "subscriptionExpiry", user.getSubscriptionExpiry() != null ? user.getSubscriptionExpiry().toString() : ""
        );
    }

    @Transactional
    public void updateSubscription(String email, String plan, int months) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));

        user.setSubscriptionPlan(plan);
        user.setSubscriptionExpiry(LocalDateTime.now().plusMonths(months));

        // Adicionar ROLE_PREMIUM se não existir
        RoleEntity premiumRole = roleRepository.findByName(RoleEntity.ROLE_PREMIUM)
                .orElseGet(() -> {
                    RoleEntity newRole = new RoleEntity();
                    newRole.setName(RoleEntity.ROLE_PREMIUM);
                    return roleRepository.save(newRole);
                });

        boolean hasPremiumRole = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleEntity.ROLE_PREMIUM));

        if (!hasPremiumRole) {
            user.getRoles().add(premiumRole);
        }

        userRepository.save(user);
    }

    // Classe auxiliar para simular o Pair do Kotlin
    public static class Pair<A, B> {
        private final A first;
        private final B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        public A getFirst() {
            return first;
        }

        public B getSecond() {
            return second;
        }
    }
}