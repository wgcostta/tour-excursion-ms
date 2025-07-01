package com.tourapp.config.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourapp.dto.GoogleUserInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
            // Decodifica o token para extrair o payload
            String[] parts = idTokenString.split("\\.");
            if (parts.length != 3) {
                logger.error("Token inválido: formato incorreto");
                return null;
            }

            String payload = parts[1];
            byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
            String payloadJson = new String(decodedBytes);

            Map<String, Object> tokenInfo = objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});

            // Verificações básicas do token
            long expirationTime = ((Number) tokenInfo.get("exp")).longValue();
            if (System.currentTimeMillis() / 1000 > expirationTime) {
                logger.error("Token expirado");
                return null;
            }

            // Retorna as informações do usuário
            return new GoogleUserInfo(
                    (String) tokenInfo.get("sub"),
                    (String) tokenInfo.get("email"),
                    (String) tokenInfo.get("name"),
                    (String) tokenInfo.get("picture")
            );
        } catch (Exception e) {
            logger.error("Erro ao verificar token do Google", e);
            return null;
        }
    }

    // Método alternativo usando o endpoint de tokeninfo do Google
    public GoogleUserInfo verifyWithGoogle(String idTokenString) {
        try {
            Request request = new Request.Builder()
                    .url("https://oauth2.googleapis.com/tokeninfo?id_token=" + idTokenString)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Erro ao verificar token com o Google: {}", response.code());
                    return null;
                }

                String responseBody = response.body() != null ? response.body().string() : null;
                if (responseBody == null) return null;

                Map<String, Object> tokenInfo = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

                return new GoogleUserInfo(
                        (String) tokenInfo.get("sub"),
                        (String) tokenInfo.get("email"),
                        (String) tokenInfo.get("name"),
                        (String) tokenInfo.get("picture")
                );
            }
        } catch (Exception e) {
            logger.error("Erro ao verificar token com o Google", e);
            return null;
        }
    }
}