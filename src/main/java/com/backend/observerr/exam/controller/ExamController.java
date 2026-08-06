package com.backend.observerr.exam.controller;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.dto.CreateExamRequest;
import com.backend.observerr.exam.dto.LecturerExamDto;
import com.backend.observerr.exam.dto.LecturerExamListResponse;
import com.backend.observerr.exam.dto.ExamQuestionRequest;
import com.backend.observerr.exam.dto.LecturerQuestionDto;
import com.backend.observerr.exam.dto.ExamResultDto;
import com.backend.observerr.exam.dto.ReleaseResultsRequest;
import com.backend.observerr.exam.dto.ReplaceExamEnrollmentsRequest;
import com.backend.observerr.exam.service.ExamLifecycleService;
import com.backend.observerr.exam.service.LecturerExamService;
import com.backend.observerr.exam.service.ExamQuestionService;
import com.backend.observerr.exam.service.ExamAttemptService;
import com.backend.observerr.exam.service.ExamStudentBlockService;
import com.backend.observerr.exam.service.ExamEnrollmentService;
import com.backend.observerr.exam.dto.BlockStudentRequest;
import com.backend.observerr.lecturer.dashboard.LecturerLiveMonitoringService;
import com.backend.observerr.lecturer.dashboard.dto.LiveExamSessionsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/lecturer/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamLifecycleService examLifecycleService;
    private final LecturerExamService lecturerExamService;
    private final ExamQuestionService examQuestionService;
    private final ExamAttemptService examAttemptService;
    private final LecturerLiveMonitoringService lecturerLiveMonitoringService;
    private final ExamStudentBlockService examStudentBlockService;
    private final ExamEnrollmentService examEnrollmentService;

    @GetMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LecturerExamListResponse> listExams(
            @AuthenticationPrincipal User lecturer,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(lecturerExamService.listExams(lecturer, status, search));
    }

    @GetMapping("/{examId}")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LecturerExamDto> getExam(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(lecturerExamService.getExam(lecturer, examId));
    }

    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LecturerExamDto> createExam(
            @AuthenticationPrincipal User lecturer,
            @Valid @RequestBody CreateExamRequest request) {
        LecturerExamDto created = lecturerExamService.createExam(lecturer, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{examId}/publish")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LecturerExamDto> publishExam(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(lecturerExamService.publishExam(lecturer, examId));
    }

    @PutMapping("/{examId}/enrollments")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Map<String, Object>> replaceEnrollments(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId,
            @Valid @RequestBody ReplaceExamEnrollmentsRequest request) {
        var institutionalIds = examEnrollmentService.replaceEnrollments(
                examId, lecturer.getId(), request.getStudentInstitutionalIds());
        return ResponseEntity.ok(Map.of(
                "examId", examId,
                "studentInstitutionalIds", institutionalIds,
                "enrolledCount", institutionalIds.size()));
    }

    @GetMapping("/{examId}/questions")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<List<LecturerQuestionDto>> getQuestions(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(examQuestionService.getLecturerQuestions(lecturer, examId));
    }

    @PutMapping("/{examId}/questions")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<List<LecturerQuestionDto>> replaceQuestions(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId,
            @Valid @RequestBody List<@Valid ExamQuestionRequest> questions) {
        return ResponseEntity.ok(examQuestionService.replaceLecturerQuestions(lecturer, examId, questions));
    }

    @GetMapping("/{examId}/results")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<List<ExamResultDto>> getResults(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(examAttemptService.lecturerResults(lecturer, examId));
    }

    @PostMapping("/{examId}/results/release")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<List<ExamResultDto>> releaseResults(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId,
            @RequestBody(required = false) ReleaseResultsRequest request) {
        List<Long> ids = request == null ? List.of() : request.getResultIds();
        return ResponseEntity.ok(examAttemptService.setReleased(lecturer, examId, ids, true));
    }

    @PostMapping("/{examId}/results/unrelease")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<List<ExamResultDto>> unreleaseResults(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId,
            @RequestBody(required = false) ReleaseResultsRequest request) {
        List<Long> ids = request == null ? List.of() : request.getResultIds();
        return ResponseEntity.ok(examAttemptService.setReleased(lecturer, examId, ids, false));
    }

    @PostMapping("/{examId}/start")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Map<String, Object>> startExam(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        LecturerExamDto exam = lecturerExamService.getExam(lecturer, examId);
        if (!exam.isPublished()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "Publish the exam before starting it",
                    "examId", examId
            ));
        }
        if ("COMPLETED".equalsIgnoreCase(exam.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "success", false,
                    "message", "This exam has already ended",
                    "examId", examId
            ));
        }
        examLifecycleService.transitionExamToLive(examId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Exam start triggered",
                "examId", examId
        ));
    }

    @PostMapping("/{examId}/end")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Map<String, Object>> endExam(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        lecturerExamService.getExam(lecturer, examId);
        examLifecycleService.endExam(examId);
        return ResponseEntity.ok(Map.of(
                "examId", examId,
                "status", "COMPLETED",
                "endedAt", java.time.Instant.now().toString()
        ));
    }

    @GetMapping("/{examId}/live-sessions")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<LiveExamSessionsResponse> getLiveSessions(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId) {
        return ResponseEntity.ok(lecturerLiveMonitoringService.getLiveSessions(lecturer, examId));
    }

    @PostMapping("/{examId}/students/{studentIdentifier}/block")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Map<String, Object>> blockStudent(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId,
            @PathVariable String studentIdentifier,
            @Valid @RequestBody(required = false) BlockStudentRequest request) {
        var block = examStudentBlockService.block(
                lecturer, examId, studentIdentifier, request == null ? null : request.getReason());
        return ResponseEntity.ok(Map.of(
                "examId", examId, "studentId", block.getStudentId(), "blocked", true));
    }

    @PostMapping("/{examId}/students/{studentIdentifier}/unblock")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Map<String, Object>> unblockStudent(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId,
            @PathVariable String studentIdentifier) {
        var block = examStudentBlockService.unblock(lecturer, examId, studentIdentifier);
        return ResponseEntity.ok(Map.of(
                "examId", examId, "studentId", block.getStudentId(), "blocked", false));
    }

    @DeleteMapping("/{examId}/students/{studentIdentifier}/block")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<Void> deleteStudentBlock(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long examId,
            @PathVariable String studentIdentifier) {
        examStudentBlockService.unblock(lecturer, examId, studentIdentifier);
        return ResponseEntity.noContent().build();
    }
}
