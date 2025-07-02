package br.com.tourapp.tourapp.service;


import br.com.tourapp.tourapp.entity.RefreshTokenEntity;
import br.com.tourapp.tourapp.exception.TokenRefreshException;
import br.com.tourapp.tourapp.repository.RefreshTokenRepository;
import br.com.tourapp.tourapp.util.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class RefreshTokenService implements RefreshTokenUseCase{

    @Value("${app.jwt.refresh-expiration:604800000}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtUtils jwtUtils) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtils = jwtUtils;
    }

    public RefreshTokenEntity createRefreshToken(String userEmail, UserDetails userDetails) {
        String token = jwtUtils.generateRefreshToken(userDetails);

        RefreshTokenEntity refreshToken = new RefreshTokenEntity(
                token,
                userEmail,
                Instant.now().plusMillis(refreshTokenDurationMs)
        );

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshTokenEntity> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshTokenEntity verifyExpiration(RefreshTokenEntity token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException(token.getToken(),
                    "Refresh token expired. Please login again");
        }

        return token;
    }

    public RefreshTokenEntity findAndValidateToken(String token) {
        RefreshTokenEntity refreshToken = findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token,
                        "Refresh token not found in database!"));

        return verifyExpiration(refreshToken);
    }

    @Transactional
    public void deleteByUserEmail(String userEmail) {
        refreshTokenRepository.deleteByUserEmail(userEmail);
    }
}