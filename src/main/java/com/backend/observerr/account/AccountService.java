package com.backend.observerr.account;

import com.backend.observerr.account.dto.AccountResponse;
import com.backend.observerr.account.dto.ChangePasswordRequest;
import com.backend.observerr.account.dto.PasswordChangeResponse;
import com.backend.observerr.account.dto.UpdateAccountRequest;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exception.FieldValidationException;
import com.backend.observerr.exception.RateLimitExceededException;
import com.backend.observerr.security.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitService rateLimitService;

    @Value("${account.password-change.max-attempts:5}")
    private int passwordChangeMaxAttempts;

    @Value("${account.password-change.window-seconds:900}")
    private long passwordChangeWindowSeconds;

    @Transactional(readOnly = true)
    public AccountResponse getAccount(User user) {
        return toAccountResponse(user);
    }

    @Transactional
    public AccountResponse updateAccount(User user, UpdateAccountRequest request) {
        List<String> changedFields = new ArrayList<>();

        if (!Objects.equals(request.getFirstName(), user.getFirstName())) {
            user.setFirstName(request.getFirstName());
            changedFields.add("firstName");
        }
        if (!Objects.equals(request.getLastName(), user.getLastName())) {
            user.setLastName(request.getLastName());
            changedFields.add("lastName");
        }

        if (!changedFields.isEmpty()) {
            userRepository.save(user);
            log.info("Account updated userId={} fields={} timestamp={}",
                    user.getId(), changedFields, Instant.now());
        }

        return toAccountResponse(user);
    }

    @Transactional
    public PasswordChangeResponse changePassword(User user, ChangePasswordRequest request) {
        String rateLimitKey = "password-change:" + user.getId();
        if (!rateLimitService.tryConsume(rateLimitKey, passwordChangeMaxAttempts, passwordChangeWindowSeconds)) {
            throw new RateLimitExceededException();
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Passwords do not match");
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new FieldValidationException("newPassword", "New password must be different from current password");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new FieldValidationException("currentPassword", "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("Password changed userId={} timestamp={} sessionsInvalidated=true",
                user.getId(), Instant.now());

        return PasswordChangeResponse.builder()
                .success(true)
                .message("Password updated successfully")
                .build();
    }

    private AccountResponse toAccountResponse(User user) {
        return AccountResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .institutionalId(user.getInstitutionalId())
                .email(user.getEmail())
                .build();
    }
}
