package com.backend.observerr.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProfilePictureResponse {

    private final boolean success;
    private final String message;
    private final String profilePictureUrl;
}
