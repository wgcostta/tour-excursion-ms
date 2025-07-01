package com.tourapp.service;

import com.tourapp.dto.response.JwtResponse;
import com.tourapp.dto.response.TokenRefreshResponse;
import com.tourapp.entity.RefreshTokenEntity;
import com.tourapp.entity.UserEntity;
import com.tourapp.util.JwtUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService implements AuthenticationUseCase{

    private final UserUseCase userService;
    private final RefreshTokenUseCase refreshTokenService;
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthenticationService(UserService userService, RefreshTokenUseCase refreshTokenService,
                                 JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public JwtResponse authenticateWithGoogle(String googleToken) {
        // Process Google token and get user
        UserEntity user = userService.processGoogleToken(googleToken);

        // Load UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Generate tokens
        String accessToken = jwtUtils.generateJwtToken(userDetails);
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user.getEmail(), userDetails);

        // Build and return response
        return userService.buildJwtResponse(user, accessToken, refreshToken.getToken());
    }

    public TokenRefreshResponse refreshToken(String refreshToken) {
        // Verify and get refresh token
        RefreshTokenEntity tokenEntity = refreshTokenService.findAndValidateToken(refreshToken);

        // Get user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(tokenEntity.getUserEmail());

        // Generate new access token
        String newAccessToken = jwtUtils.generateJwtToken(userDetails);

        // Return response
        return new TokenRefreshResponse(newAccessToken, tokenEntity.getToken());
    }
}


