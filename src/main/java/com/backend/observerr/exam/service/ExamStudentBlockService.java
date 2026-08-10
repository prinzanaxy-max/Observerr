package com.backend.observerr.exam.service;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.exam.model.ExamStudentBlock;
import com.backend.observerr.exam.repository.ExamEnrollmentRepository;
import com.backend.observerr.exam.repository.ExamRepository;
import com.backend.observerr.exam.repository.ExamStudentBlockRepository;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ExamStudentBlockService {
    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository enrollmentRepository;
    private final ExamStudentBlockRepository blockRepository;
    private final ExamSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public ExamStudentBlock block(User lecturer, Long examId, String identifier, String reason) {
        requireOwnedExam(lecturer, examId);
        User student = resolveStudent(identifier);
        if (!enrollmentRepository.existsByExamIdAndStudentId(examId, student.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student is not enrolled in this exam");
        }
        ExamStudentBlock block = blockRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseGet(() -> ExamStudentBlock.builder()
                        .examId(examId).studentId(student.getId()).build());
        block.setBlockedBy(lecturer.getId());
        block.setReason(reason);
        block.setBlockedAt(Instant.now());
        block.setUnblockedAt(null);

        sessionRepository.findByExamId(examId).stream()
                .filter(s -> s.getStudentId().equals(student.getId()))
                .filter(s -> s.getStatus() == ExamSessionStatus.IN_PROGRESS)
                .forEach(s -> {
                    s.setStatus(ExamSessionStatus.COMPLETED);
                    s.setEndedAt(Instant.now());
                    s.setFinalScore((short) Math.max(0, s.getStartingScore() - s.getTotalDeductions()));
                    s.setRequiresReview(true);
                });
        return blockRepository.save(block);
    }

    @Transactional
    public ExamStudentBlock unblock(User lecturer, Long examId, String identifier) {
        requireOwnedExam(lecturer, examId);
        User student = resolveStudent(identifier);
        ExamStudentBlock block = blockRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
        block.setUnblockedAt(Instant.now());
        return block;
    }

    public void requireNotBlocked(Long examId, Long studentId) {
        if (blockRepository.existsByExamIdAndStudentIdAndUnblockedAtIsNull(examId, studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student is blocked from this exam");
        }
    }

    private void requireOwnedExam(User lecturer, Long examId) {
        examRepository.findByIdAndLecturerId(examId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private User resolveStudent(String identifier) {
        try {
            return userRepository.findById(Long.valueOf(identifier))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        } catch (NumberFormatException ignored) {
            return userRepository.findByInstitutionalId(identifier)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        }
    }
}
