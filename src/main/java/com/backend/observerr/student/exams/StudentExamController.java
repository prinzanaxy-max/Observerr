package com.backend.observerr.student.exams;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.student.exams.dto.StudentExamDto;
import com.backend.observerr.student.exams.dto.StudentExamListResponse;
import com.backend.observerr.exam.dto.StudentQuestionDto;
import com.backend.observerr.exam.service.ExamQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {

    private final StudentExamService studentExamService;
    private final ExamQuestionService examQuestionService;

    @GetMapping
    public ResponseEntity<StudentExamListResponse> listExams(@AuthenticationPrincipal User student) {
        return ResponseEntity.ok(studentExamService.listExams(student));
    }

    @GetMapping("/{examId}")
    public ResponseEntity<StudentExamDto> getExam(
            @AuthenticationPrincipal User student,
            @PathVariable Long examId) {
        return ResponseEntity.ok(studentExamService.getExam(student, examId));
    }

    @GetMapping("/{examId}/questions")
    public ResponseEntity<List<StudentQuestionDto>> getQuestions(
            @AuthenticationPrincipal User student,
            @PathVariable Long examId) {
        return ResponseEntity.ok(examQuestionService.getStudentQuestions(student, examId));
    }
}
