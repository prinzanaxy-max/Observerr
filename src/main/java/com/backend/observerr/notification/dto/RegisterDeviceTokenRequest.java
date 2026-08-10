package com.backend.observerr.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "Push endpoint is required")
    private String endpoint;

    @NotNull(message = "Push keys are required")
    @Valid
    private Keys keys;

    @Getter
    @Setter
    public static class Keys {
        @NotBlank(message = "p256dh key is required")
        private String p256dh;

        @NotBlank(message = "auth key is required")
        private String auth;
    }
}
