package org.opensbpm.engine.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class Authentication {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private int expiresIn;

    @JsonProperty("refresh_expires_in")
    private int refreshExpiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("not-before-policy")
    private int notBeforePolicy;

    @JsonProperty("session_state")
    private String sessionState;

    @JsonProperty("scope")
    private String scope;

    private LocalDateTime createAt;
    public Authentication() {
        this.createAt = LocalDateTime.now();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public int getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public int getNotBeforePolicy() {
        return notBeforePolicy;
    }

    public String getSessionState() {
        return sessionState;
    }

    public String getScope() {
        return scope;
    }

    public boolean isExpired() {
        return LocalDateTime.now().plusSeconds(10)
                .isAfter(createAt.plusSeconds(getExpiresIn()));
    }

    public boolean isRefreshExpired() {
        return LocalDateTime.now().plusSeconds(10)
                .isAfter(createAt.plusSeconds(getRefreshExpiresIn()));
    }
}
