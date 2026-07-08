package com.courierapp.dto.auth;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean mfaRequired,
        String mfaPendingToken
) {
    public static TokenResponse bearer(String access, String refresh, long expiresIn) {
        return new TokenResponse(access, refresh, "Bearer", expiresIn, false, null);
    }

    public static TokenResponse mfaRequired(String mfaPendingToken) {
        return new TokenResponse(null, null, null, 0, true, mfaPendingToken);
    }
}
