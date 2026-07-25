package com.backend.observerr.account;

import com.backend.observerr.account.dto.AccountResponse;
import com.backend.observerr.account.dto.ChangePasswordRequest;
import com.backend.observerr.account.dto.PasswordChangeResponse;
import com.backend.observerr.account.dto.UpdateAccountRequest;
import com.backend.observerr.auth.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accountService.getAccount(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<AccountResponse> updateAccount(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(user, request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<PasswordChangeResponse> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ChangePasswordRequest request) {
        return ResponseEntity.ok(accountService.changePassword(user, request));
    }
}
