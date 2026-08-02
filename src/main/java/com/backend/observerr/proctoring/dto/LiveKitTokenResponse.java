package com.backend.observerr.proctoring.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiveKitTokenResponse {

    private final String url;
    private final String token;
    private final String roomName;
    private final String participantIdentity;
    private final String expiresAt;
}
