package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.lecturer.dashboard.dto.ProctoringExamListResponse;
import com.backend.observerr.lecturer.dashboard.dto.ProctoringFeedsResponse;
import com.backend.observerr.proctoring.ProctoringMediaService;
import com.backend.observerr.proctoring.dto.LiveKitTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lecturer/proctoring")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class LecturerProctoringController {

    private final LecturerProctoringService lecturerProctoringService;
    private final ProctoringMediaService proctoringMediaService;

    @GetMapping("/exams")
    public ResponseEntity<ProctoringExamListResponse> listExams(@AuthenticationPrincipal User lecturer) {
        return ResponseEntity.ok(lecturerProctoringService.listLiveExams(lecturer));
    }

    @GetMapping("/exams/{examId}/feeds")
    public ResponseEntity<ProctoringFeedsResponse> getFeeds(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(lecturerProctoringService.getFeeds(lecturer, examId));
    }

    @PostMapping("/exams/{examId}/media-token")
    public ResponseEntity<LiveKitTokenResponse> issueMediaToken(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(proctoringMediaService.issueLecturerToken(lecturer, examId));
    }
}
