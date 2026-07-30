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

@Service
@RequiredArgsConstructor
public class ExamEnrollmentService {

    private final UserRepository userRepository;
    private final ExamEnrollmentRepository examEnrollmentRepository;
    private final ExamRepository examRepository;

    @Transactional
    public void enrollAllStudents(Exam exam) {
        if (exam == null || !exam.isPublished()) {
            return;
        }

        for (User student : userRepository.findByRole(Role.STUDENT)) {
            if (!examEnrollmentRepository.existsByExamIdAndStudentId(exam.getId(), student.getId())) {
                examEnrollmentRepository.save(ExamEnrollment.builder()
                        .examId(exam.getId())
                        .studentId(student.getId())
                        .build());
            }
        }

        syncEnrolledCount(exam.getId());
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
