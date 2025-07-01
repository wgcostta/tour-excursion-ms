package com.tourapp.service;

import com.tourapp.dto.response.JwtResponse;
import com.tourapp.dto.response.UserInfoResponse;
import com.tourapp.entity.UserEntity;

import java.util.Map;

public interface UserUseCase {
    UserInfoResponse getUserInfo(String username);

    Map<String, Object> checkSubscription(String username);

    void updateSubscription(String email, String plan, int months);

    UserEntity processGoogleToken(String googleToken);

    JwtResponse buildJwtResponse(UserEntity user, String accessToken, String token);
}
