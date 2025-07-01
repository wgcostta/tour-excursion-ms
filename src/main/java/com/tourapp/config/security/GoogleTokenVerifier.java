package com.tourapp.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourapp.dto.GoogleUserInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Component
public class GoogleTokenVerifier {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GoogleTokenVerifier(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public GoogleUserInfo verify(String idTokenString) {
        try {
            // Decode token to extract payload
            String[] parts = idTokenString.split("\\.");
            if (parts.length != 3) {
                logger.error("Invalid token: incorrect format");
                return null;
            }

            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decodedBytes);

            Map<String, Object> tokenInfo = objectMapper.readValue(payloadJson, Map.class);

            // Basic token verification
            long expirationTime = ((Number) tokenInfo.get("exp")).longValue();
            if (System.currentTimeMillis() / 1000 > expirationTime) {
                logger.error("Token expired");
                return null;
            }

            // Return user information
            return new GoogleUserInfo(
                    (String) tokenInfo.get("sub"),
                    (String) tokenInfo.get("email"),
                    (String) tokenInfo.get("name"),
                    (String) tokenInfo.get("picture")
            );
        } catch (Exception e) {
            logger.error("Error verifying Google token", e);
            return null;
        }
    }

    // Alternative method using Google's tokeninfo endpoint
    public GoogleUserInfo verifyWithGoogle(String idTokenString) {
        try {
            Request request = new Request.Builder()
                    .url("https://oauth2.googleapis.com/tokeninfo?id_token=" + idTokenString)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Error verifying token with Google: {}", response.code());
                    return null;
                }

                String responseBody = response.body().string();
                Map<String, Object> tokenInfo = objectMapper.readValue(responseBody, Map.class);

                return new GoogleUserInfo(
                        (String) tokenInfo.get("sub"),
                        (String) tokenInfo.get("email"),
                        (String) tokenInfo.get("name"),
                        (String) tokenInfo.get("picture")
                );
            }
        } catch (IOException e) {
            logger.error("Error verifying token with Google", e);
            return null;
        }
    }
}