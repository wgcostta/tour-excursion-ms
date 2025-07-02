package br.com.tourapp.tourapp.service;

import br.com.tourapp.tourapp.dto.response.JwtResponse;
import br.com.tourapp.tourapp.dto.response.TokenRefreshResponse;

public interface AuthenticationUseCase {
    JwtResponse authenticateWithGoogle(String token);

    TokenRefreshResponse refreshToken(String refreshToken);
}
