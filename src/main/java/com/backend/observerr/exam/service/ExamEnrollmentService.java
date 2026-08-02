package com.backend.observerr.exam.service;

import com.backend.observerr.auth.model.Role;
import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exam.model.Exam;
import com.backend.observerr.exam.model.ExamEnrollment;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamEnrollmentService {

    private final UserRepository userRepository;
    private final ExamEnrollmentRepository examEnrollmentRepository;
    private final ExamRepository examRepository;

    @Transactional
    public List<String> replaceEnrollments(Long examId, Long lecturerId, List<String> institutionalIds) {
        Exam exam = examRepository.findByIdAndLecturerId(examId, lecturerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        Set<String> requestedIds = new LinkedHashSet<>();
        institutionalIds.stream()
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .forEach(requestedIds::add);

        List<User> students = requestedIds.isEmpty()
                ? List.of()
                : userRepository.findByInstitutionalIdIn(List.copyOf(requestedIds));
        if (students.size() != requestedIds.size()
                || students.stream().anyMatch(user -> user.getRole() != Role.STUDENT)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Every enrollment must identify an existing student");
        }

        examEnrollmentRepository.deleteByExamId(exam.getId());
        examEnrollmentRepository.saveAll(students.stream()
                .map(student -> ExamEnrollment.builder()
                        .examId(exam.getId())
                        .studentId(student.getId())
                        .build())
                .toList());
        exam.setEnrolledCount(students.size());
        examRepository.save(exam);
        return students.stream().map(User::getInstitutionalId).sorted().toList();
    }

    @Transactional
    public void syncEnrolledCount(Long examId) {
        examRepository.findById(examId).ifPresent(exam -> {
            int count = (int) examEnrollmentRepository.countByExamId(examId);
            exam.setEnrolledCount(count);
            examRepository.save(exam);
        });
    }
}
