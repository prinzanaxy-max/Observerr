package com.backend.observerr.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AccountResponse {

    private final String firstName;
    private final String lastName;
    private final String institutionalId;
    private final String email;
}
