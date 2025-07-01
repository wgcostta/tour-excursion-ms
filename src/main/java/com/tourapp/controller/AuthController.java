package com.tourapp.controller;


import com.tourapp.dto.request.TokenRefreshRequest;
import com.tourapp.dto.response.JwtResponse;
import com.tourapp.dto.response.TokenRefreshResponse;
import com.tourapp.dto.response.UserInfoResponse;
import com.tourapp.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final UserUseCase userService;
    private final RefreshTokenUseCase refreshTokenService;
    private final AuthenticationUseCase authenticationService;

    public AuthController(UserService userService, RefreshTokenUseCase refreshTokenService,
                          AuthenticationUseCase authenticationService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate Google token", description = "Validates a Google ID token and returns JWT")
    public ResponseEntity<JwtResponse> validateToken(@RequestParam("token") String token) {
        JwtResponse jwtResponse = authenticationService.authenticateWithGoogle(token);
        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Generate a new access token using refresh token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authenticationService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate user's refresh token")
    public ResponseEntity<Map<String, String>> logoutUser(@AuthenticationPrincipal UserDetails userDetails) {
        refreshTokenService.deleteByUserEmail(userDetails.getUsername());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Log out successful!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get user info", description = "Returns authenticated user's information")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        UserInfoResponse userInfoResponse = userService.getUserInfo(userDetails.getUsername());
        return ResponseEntity.ok(userInfoResponse);
    }

    @GetMapping("/check-subscription")
    @Operation(summary = "Check subscription", description = "Check if user has active subscription")
    public ResponseEntity<Map<String, Object>> checkSubscription(@AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> subscriptionInfo = userService.checkSubscription(userDetails.getUsername());
        return ResponseEntity.ok(subscriptionInfo);
    }

    @PostMapping("/subscription/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update subscription", description = "Update user subscription (admin only)")
    public ResponseEntity<Map<String, String>> updateSubscription(
            @RequestParam("email") String email,
            @RequestParam("plan") String plan,
            @RequestParam("months") int months) {
        userService.updateSubscription(email, plan, months);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Subscription updated successfully");
        return ResponseEntity.ok(response);
    }
}