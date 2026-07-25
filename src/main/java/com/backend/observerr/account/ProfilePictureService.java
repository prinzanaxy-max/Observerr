package com.backend.observerr.account;

import com.backend.observerr.account.dto.ProfilePictureResponse;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exception.FieldValidationException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ProfilePictureService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final UserRepository userRepository;
    private final Cloudinary cloudinary;
    private final String uploadFolder;
    private final long maxFileSizeBytes;

    public ProfilePictureService(
            UserRepository userRepository,
            ObjectProvider<Cloudinary> cloudinaryProvider,
            @Value("${cloudinary.folder:observerr/profile-pictures}") String uploadFolder,
            @Value("${account.profile-picture.max-size-bytes:5242880}") long maxFileSizeBytes) {
        this.userRepository = userRepository;
        this.cloudinary = cloudinaryProvider.getIfAvailable();
        this.uploadFolder = uploadFolder;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Transactional
    public ProfilePictureResponse uploadProfilePicture(User user, MultipartFile file) {
        requireCloudinaryConfigured();
        validateFile(file);

        try {
            deleteExistingPicture(user);

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", uploadFolder,
                            "public_id", "user-" + user.getId(),
                            "overwrite", true,
                            "resource_type", "image",
                            "transformation", "c_fill,g_face,h_400,w_400"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            String storedPublicId = (String) uploadResult.get("public_id");

            user.setProfilePictureUrl(secureUrl);
            user.setProfilePicturePublicId(storedPublicId);
            userRepository.save(user);

            log.info("Profile picture uploaded userId={} publicId={} timestamp={}",
                    user.getId(), user.getProfilePicturePublicId(), Instant.now());

            return ProfilePictureResponse.builder()
                    .success(true)
                    .message("Profile picture updated")
                    .profilePictureUrl(secureUrl)
                    .build();
        } catch (IOException ex) {
            log.error("Profile picture upload failed userId={}: {}", user.getId(), ex.getMessage(), ex);
            throw new FieldValidationException("file", "Failed to upload profile picture");
        }
    }

    @Transactional
    public ProfilePictureResponse deleteProfilePicture(User user) {
        requireCloudinaryConfigured();
        deleteExistingPicture(user);

        user.setProfilePictureUrl(null);
        user.setProfilePicturePublicId(null);
        userRepository.save(user);

        log.info("Profile picture removed userId={} timestamp={}", user.getId(), Instant.now());

        return ProfilePictureResponse.builder()
                .success(true)
                .message("Profile picture removed")
                .profilePictureUrl(null)
                .build();
    }

    private void deleteExistingPicture(User user) {
        if (cloudinary == null || user.getProfilePicturePublicId() == null) {
            return;
        }
        try {
            cloudinary.uploader().destroy(user.getProfilePicturePublicId(), ObjectUtils.emptyMap());
        } catch (IOException ex) {
            log.warn("Failed to delete old profile picture userId={} publicId={}: {}",
                    user.getId(), user.getProfilePicturePublicId(), ex.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FieldValidationException("file", "Profile picture file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new FieldValidationException("file", "Profile picture must be 5 MB or smaller");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new FieldValidationException("file", "Only JPEG, PNG, WebP, and GIF images are allowed");
        }
    }

    private void requireCloudinaryConfigured() {
        if (cloudinary == null) {
            throw new FieldValidationException("file", "Profile picture upload is not configured on the server");
        }
    }
}
