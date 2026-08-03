package com.backend.observerr.integrity;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical integrity deduction table.
 * <p>
 * Per-type caps prevent a single signal from zeroing a score. Example: COPY is 7 pts
 * per occurrence with a 35-pt cap → five copies land at 65% and flag review.
 */
@Component
public class IntegrityScoringPolicy {

    public static final int UNAVAILABLE_PROCTORING_SCORE_CAP = 60;
    private static final int MAX_EVENT_DURATION_MS = 6 * 60 * 60 * 1000;

    /** Cap keys → max cumulative deduction for that family. */
    public static final Map<String, Integer> MAX_DEDUCTION_BY_CAP = Map.ofEntries(
            Map.entry("COPY", 35),
            Map.entry("PASTE", 32),
            Map.entry("MULTI_FACE", 45),
            Map.entry("FACE_ABSENT", 32),
            Map.entry("DEVTOOLS", 30),
            Map.entry("TAB_SWITCH", 25),
            Map.entry("FOCUS_LOSS", 20),
            Map.entry("FULLSCREEN_EXIT", 15),
            Map.entry("PAGE_REFRESH", 18),
            Map.entry("IDLE", 15),
            Map.entry("GAZE", 15),
            Map.entry("TAB_BLUR_NO_FACE", 45),
            Map.entry("CAMERA_PERMISSION", 40),
            Map.entry("CAMERA_FROZEN", 55),
            Map.entry("PROCTORING_UNAVAILABLE", 100)
    );

    private static final Map<String, Rule> FIXED_RULES = Map.ofEntries(
            Map.entry("COPY_EVENT", new Rule("Copy detected", "MEDIUM", 7, false, "COPY")),
            Map.entry("PASTE_EVENT", new Rule("Paste detected", "HIGH", 8, false, "PASTE")),
            Map.entry("CLIPBOARD_EVENT", new Rule("Clipboard activity", "MEDIUM", 7, false, "COPY")),
            Map.entry("MULTI_FACE_DETECTED", new Rule("Second face detected in frame", "HIGH", 15, true, "MULTI_FACE")),
            Map.entry("DEVTOOLS_SHORTCUT", new Rule("DevTools / app-switch shortcut", "HIGH", 10, false, "DEVTOOLS")),
            Map.entry("TAB_SWITCH", new Rule("Tab switch", "MEDIUM", 5, false, "TAB_SWITCH")),
            Map.entry("TAB_BLUR", new Rule("Tab switch", "MEDIUM", 5, false, "TAB_SWITCH")),
            Map.entry("FOCUS_LOSS", new Rule("Window focus lost", "LOW", 4, false, "FOCUS_LOSS")),
            Map.entry("FULLSCREEN_EXIT", new Rule("Exited fullscreen", "MEDIUM", 5, false, "FULLSCREEN_EXIT")),
            Map.entry("PAGE_REFRESH", new Rule("Page refresh", "MEDIUM", 6, false, "PAGE_REFRESH")),
            Map.entry("IDLE_TIMEOUT", new Rule("Idle for 60s+", "LOW", 3, false, "IDLE")),
            Map.entry("GAZE_DEVIATION", new Rule("Gaze deviation", "LOW", 3, false, "GAZE")),
            Map.entry("FACE_ABSENT", new Rule("No face detected", "MEDIUM", 8, false, "FACE_ABSENT")),
            Map.entry("TAB_BLUR_REPEATED", new Rule("Repeated tab blur (legacy)", "INFO", 0, false, "TAB_SWITCH")),
            Map.entry("CAMERA_PERMISSION_LOST", new Rule("Webcam permission lost", "CRITICAL", 40, true, "CAMERA_PERMISSION")),
            Map.entry("PROCTORING_UNAVAILABLE", new Rule("Proctoring unavailable", "HIGH", 25, true, "PROCTORING_UNAVAILABLE")),
            Map.entry("TAB_BLUR_NO_FACE", new Rule("Tab blur with no face visible", "CRITICAL", 15, true, "TAB_BLUR_NO_FACE")),
            Map.entry("CAMERA_FEED_FROZEN", new Rule("Camera feed frozen or spoofed", "CRITICAL", 25, true, "CAMERA_FROZEN"))
    );

    private static final Set<String> INFORMATIONAL_CODES = Set.of(
            "GAZE_DEVIATION_START",
            "GAZE_DEVIATION_END",
            "FACE_LOST",
            "FACE_RESTORED",
            "FACE_PARTIAL_DETECTED",
            "FACE_PARTIAL_CLEARED",
            "TAB_FOCUS",
            "MULTI_FACE_CLEARED",
            "CALIBRATION_COMPLETE",
            "SESSION_STARTED",
            "SESSION_ENDED"
    );

    public static final List<String> COPY_CAP_CODES = List.of("COPY_EVENT", "CLIPBOARD_EVENT");
    public static final List<String> PASTE_CAP_CODES = List.of("PASTE_EVENT");
    public static final List<String> MULTI_FACE_CAP_CODES = List.of("MULTI_FACE_DETECTED");
    public static final List<String> FACE_ABSENT_CAP_CODES = List.of(
            "FACE_ABSENT", "FACE_ABSENT_SHORT", "FACE_ABSENT_BRIEF", "FACE_ABSENT_MEDIUM", "FACE_ABSENT_LONG");
    public static final List<String> DEVTOOLS_CAP_CODES = List.of("DEVTOOLS_SHORTCUT");
    public static final List<String> TAB_SWITCH_CAP_CODES = List.of("TAB_SWITCH", "TAB_BLUR", "TAB_BLUR_REPEATED");
    public static final List<String> FOCUS_LOSS_CAP_CODES = List.of("FOCUS_LOSS");
    public static final List<String> FULLSCREEN_CAP_CODES = List.of("FULLSCREEN_EXIT");
    public static final List<String> PAGE_REFRESH_CAP_CODES = List.of("PAGE_REFRESH");
    public static final List<String> IDLE_CAP_CODES = List.of("IDLE_TIMEOUT");
    public static final List<String> GAZE_CAP_CODES = List.of(
            "GAZE_DEVIATION", "GAZE_DEVIATION_BRIEF", "GAZE_DEVIATION_MODERATE", "GAZE_DEVIATION_SUSTAINED");
    public static final List<String> TAB_BLUR_NO_FACE_CAP_CODES = List.of("TAB_BLUR_NO_FACE");
    public static final List<String> CAMERA_PERMISSION_CAP_CODES = List.of("CAMERA_PERMISSION_LOST");
    public static final List<String> CAMERA_FROZEN_CAP_CODES = List.of("CAMERA_FEED_FROZEN");
    public static final List<String> PROCTORING_UNAVAILABLE_CAP_CODES = List.of("PROCTORING_UNAVAILABLE");

    public Rule resolve(String eventCode, Integer durationMs) {
        return resolve(eventCode, durationMs, null);
    }

    public Rule resolve(String eventCode, Integer durationMs, Map<String, Object> metadata) {
        if (eventCode == null || eventCode.isBlank()) {
            throw invalid("eventCode is required");
        }
        if (durationMs != null && (durationMs < 0 || durationMs > MAX_EVENT_DURATION_MS)) {
            throw invalid("Invalid event duration");
        }

        String normalized = normalizeEventCode(eventCode, metadata);

        Rule fixed = FIXED_RULES.get(normalized);
        if (fixed != null) {
            return fixed;
        }
        if (INFORMATIONAL_CODES.contains(normalized)) {
            return new Rule(normalized.replace('_', ' '), "INFO", 0, false, normalized);
        }

        int duration = requireDuration(durationMs);
        return switch (normalized) {
            case "GAZE_DEVIATION_BRIEF", "GAZE_DEVIATION_MODERATE", "GAZE_DEVIATION_SUSTAINED" -> {
                if (normalized.equals("GAZE_DEVIATION_BRIEF") && (duration < 2_000 || duration >= 4_000)) {
                    throw rejectDuration(normalized);
                }
                if (normalized.equals("GAZE_DEVIATION_MODERATE") && (duration < 4_000 || duration >= 10_000)) {
                    throw rejectDuration(normalized);
                }
                if (normalized.equals("GAZE_DEVIATION_SUSTAINED") && duration < 10_000) {
                    throw rejectDuration(normalized);
                }
                yield new Rule("Gaze deviation", "LOW", 3, false, "GAZE");
            }
            case "FACE_PARTIAL_BRIEF" -> duration <= 5_000
                    ? new Rule("Face partially out of frame", "LOW", 3, false, "FACE_ABSENT")
                    : rejectDuration(normalized);
            case "FACE_ABSENT_SHORT", "FACE_ABSENT_BRIEF" -> duration >= 2_000 && duration < 5_000
                    ? new Rule("No face detected", "MEDIUM", 8, false, "FACE_ABSENT")
                    : rejectDuration(normalized);
            case "FACE_ABSENT_MEDIUM" -> duration >= 5_000 && duration < 15_000
                    ? new Rule("No face detected", "MEDIUM", 8, false, "FACE_ABSENT")
                    : rejectDuration(normalized);
            case "FACE_ABSENT_LONG" -> duration >= 15_000
                    ? new Rule("No face detected", "MEDIUM", 8, false, "FACE_ABSENT")
                    : rejectDuration(normalized);
            default -> throw invalid("Unsupported integrity eventCode");
        };
    }

    public static String normalizeEventCode(String eventCode, Map<String, Object> metadata) {
        if ("CLIPBOARD_EVENT".equals(eventCode) && metadata != null) {
            Object action = metadata.get("action");
            if (action != null && "paste".equalsIgnoreCase(String.valueOf(action))) {
                return "PASTE_EVENT";
            }
            return "COPY_EVENT";
        }
        if ("TAB_BLUR".equals(eventCode) && metadata != null
                && "window".equalsIgnoreCase(String.valueOf(metadata.get("source")))) {
            return "FOCUS_LOSS";
        }
        return eventCode;
    }

    public static List<String> codesForCap(String capKey) {
        return switch (capKey) {
            case "COPY" -> COPY_CAP_CODES;
            case "PASTE" -> PASTE_CAP_CODES;
            case "MULTI_FACE" -> MULTI_FACE_CAP_CODES;
            case "FACE_ABSENT" -> FACE_ABSENT_CAP_CODES;
            case "DEVTOOLS" -> DEVTOOLS_CAP_CODES;
            case "TAB_SWITCH" -> TAB_SWITCH_CAP_CODES;
            case "FOCUS_LOSS" -> FOCUS_LOSS_CAP_CODES;
            case "FULLSCREEN_EXIT" -> FULLSCREEN_CAP_CODES;
            case "PAGE_REFRESH" -> PAGE_REFRESH_CAP_CODES;
            case "IDLE" -> IDLE_CAP_CODES;
            case "GAZE" -> GAZE_CAP_CODES;
            case "TAB_BLUR_NO_FACE" -> TAB_BLUR_NO_FACE_CAP_CODES;
            case "CAMERA_PERMISSION" -> CAMERA_PERMISSION_CAP_CODES;
            case "CAMERA_FROZEN" -> CAMERA_FROZEN_CAP_CODES;
            case "PROCTORING_UNAVAILABLE" -> PROCTORING_UNAVAILABLE_CAP_CODES;
            default -> List.of(capKey);
        };
    }

    public static boolean flagsReviewWhenCapped(String capKey) {
        return "COPY".equals(capKey) || "MULTI_FACE".equals(capKey);
    }

    public static int applyCap(String capKey, int requestedPoints, int alreadyDeducted) {
        int max = MAX_DEDUCTION_BY_CAP.getOrDefault(capKey, Integer.MAX_VALUE);
        int remaining = Math.max(0, max - alreadyDeducted);
        return Math.min(Math.max(0, requestedPoints), remaining);
    }

    private int requireDuration(Integer durationMs) {
        if (durationMs == null) {
            throw invalid("durationMs is required for this eventCode");
        }
        return durationMs;
    }

    private Rule rejectDuration(String eventCode) {
        throw invalid("durationMs does not match " + eventCode);
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record Rule(
            String title,
            String severity,
            int points,
            boolean requiresReview,
            String capKey
    ) {
    }
}
