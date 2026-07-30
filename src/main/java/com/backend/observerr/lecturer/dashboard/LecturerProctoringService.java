package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.dto.LecturerExamDto;
import com.backend.observerr.exam.service.LecturerExamService;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.lecturer.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LecturerProctoringService {

    private final LecturerExamService lecturerExamService;
    private final ExamSessionRepository examSessionRepository;
    private final LecturerLiveMonitoringService liveMonitoringService;

    @Transactional(readOnly = true)
    public ProctoringExamListResponse listLiveExams(User lecturer) {
        List<LecturerExamDto> liveExams = lecturerExamService.listExams(lecturer, "LIVE", null).getExams();

        List<ProctoringExamSummaryDto> summaries = liveExams.stream()
                .map(exam -> {
                    int activeFeeds = (int) examSessionRepository.findByExamIdAndLecturerId(exam.getId(), lecturer.getId())
                            .stream()
                            .filter(session -> session.getStatus() == ExamSessionStatus.IN_PROGRESS)
                            .count();
                    return ProctoringExamSummaryDto.builder()
                            .examId(exam.getId())
                            .title(exam.getTitle())
                            .courseLabel(exam.getCourseLabel())
                            .activeFeeds(activeFeeds)
                            .totalStudents(exam.getEnrolledCount())
                            .build();
                })
                .toList();

        return ProctoringExamListResponse.builder().exams(summaries).build();
    }

    @Transactional(readOnly = true)
    public ProctoringFeedsResponse getFeeds(User lecturer, Long examId) {
        LiveExamSessionsResponse live = liveMonitoringService.getLiveSessions(lecturer, examId);

        List<ProctoringFeedDto> feeds = live.getStudents().stream()
                .filter(student -> student.getLatestSessionId() != null)
                .map(student -> ProctoringFeedDto.builder()
                        .sessionId(student.getLatestSessionId())
                        .studentId(student.getStudentId())
                        .studentName(student.getName())
                        .initials(student.getInitials())
                        .integrityScore(student.getIntegrityScore())
                        .riskLevel(student.getRiskLevel())
                        .liveStatusLabel(student.getLiveStatusLabel())
                        .lastEvent(student.getLastEvent())
                        .snapshotUrl(null)
                        .build())
                .toList();

        if (feeds.isEmpty() && live.getStats().getTotal() == 0) {
            lecturerExamService.getExam(lecturer, examId);
        }

        return ProctoringFeedsResponse.builder()
                .examId(examId)
                .feeds(feeds)
                .build();
    }
}
