package com.apisentinel.dto.response;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
}
