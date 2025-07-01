package com.tourapp.service;

import com.tourapp.dto.response.JwtResponse;
import com.tourapp.dto.response.TokenRefreshResponse;

public interface AuthenticationUseCase {
    JwtResponse authenticateWithGoogle(String token);

    TokenRefreshResponse refreshToken(String refreshToken);
}
