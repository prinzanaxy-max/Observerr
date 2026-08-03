package com.backend.observerr.integrity;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Component
public class IntegrityScoringPolicy {

    public static final int UNAVAILABLE_PROCTORING_SCORE_CAP = 60;
    private static final int MAX_EVENT_DURATION_MS = 6 * 60 * 60 * 1000;

    private static final Map<String, Rule> FIXED_RULES = Map.ofEntries(
            Map.entry("TAB_BLUR", new Rule("Tab/window blur", "MEDIUM", 8, false)),
            Map.entry("TAB_BLUR_REPEATED", new Rule("Repeated tab/window blur", "HIGH", 20, false)),
            Map.entry("CLIPBOARD_EVENT", new Rule("Copy/paste detected", "HIGH", 20, false)),
            Map.entry("MULTI_FACE_DETECTED", new Rule("Second face detected in frame", "CRITICAL", 40, true)),
            Map.entry("DEVTOOLS_SHORTCUT", new Rule("DevTools shortcut attempt", "HIGH", 35, false)),
            Map.entry("CAMERA_PERMISSION_LOST", new Rule("Webcam permission lost", "CRITICAL", 40, true)),
            Map.entry("PROCTORING_UNAVAILABLE", new Rule("Proctoring unavailable", "HIGH", 25, true)),
            Map.entry("TAB_BLUR_NO_FACE", new Rule("Tab blur with no face visible", "CRITICAL", 50, true)),
            Map.entry("CAMERA_FEED_FROZEN", new Rule("Camera feed frozen or spoofed", "CRITICAL", 55, true))
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

    public Rule resolve(String eventCode, Integer durationMs) {
        if (eventCode == null || eventCode.isBlank()) {
            throw invalid("eventCode is required");
        }
        if (durationMs != null && (durationMs < 0 || durationMs > MAX_EVENT_DURATION_MS)) {
            throw invalid("Invalid event duration");
        }

        Rule fixed = FIXED_RULES.get(eventCode);
        if (fixed != null) {
            return fixed;
        }
        if (INFORMATIONAL_CODES.contains(eventCode)) {
            return new Rule(eventCode.replace('_', ' '), "INFO", 0, false);
        }

        int duration = requireDuration(durationMs);
        return switch (eventCode) {
            case "GAZE_DEVIATION_BRIEF" -> duration >= 2_000 && duration < 4_000
                    ? new Rule("Brief gaze deviation", "LOW", 5, false) : rejectDuration(eventCode);
            case "GAZE_DEVIATION_MODERATE" -> duration >= 4_000 && duration < 10_000
                    ? new Rule("Moderate gaze deviation", "MEDIUM", 12, false) : rejectDuration(eventCode);
            case "GAZE_DEVIATION_SUSTAINED" -> duration >= 10_000
                    ? new Rule("Sustained gaze deviation", "HIGH", 20, false) : rejectDuration(eventCode);
            case "FACE_PARTIAL_BRIEF" -> duration <= 5_000
                    ? new Rule("Face partially out of frame", "MEDIUM", 6, false) : rejectDuration(eventCode);
            case "FACE_ABSENT_SHORT", "FACE_ABSENT_BRIEF" -> duration >= 2_000 && duration < 5_000
                    ? new Rule("Face briefly absent", "MEDIUM", 10, false) : rejectDuration(eventCode);
            case "FACE_ABSENT_MEDIUM" -> duration >= 5_000 && duration < 15_000
                    ? new Rule("Face absent", "HIGH", 18, false) : rejectDuration(eventCode);
            case "FACE_ABSENT_LONG" -> duration >= 15_000
                    ? new Rule("Face absent for an extended period", "CRITICAL", 30, true) : rejectDuration(eventCode);
            default -> throw invalid("Unsupported integrity eventCode");
        };
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

    public record Rule(String title, String severity, int points, boolean requiresReview) {
    }
}
