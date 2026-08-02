package com.backend.observerr.exam.service;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.exam.dto.*;
import com.backend.observerr.exam.model.*;
import com.backend.observerr.exam.repository.*;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamQuestionService {

    private final ExamRepository examRepository;
    private final ExamEnrollmentRepository enrollmentRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamQuestionOptionRepository optionRepository;
    private final ExamSessionRepository sessionRepository;
    private final ExamStudentBlockService blockService;

    @Transactional
    public void createQuestions(Long examId, List<ExamQuestionRequest> requests) {
        saveQuestions(examId, requests == null ? List.of() : requests);
    }

    @Transactional(readOnly = true)
    public List<LecturerQuestionDto> getLecturerQuestions(User lecturer, Long examId) {
        requireOwnedExam(lecturer, examId);
        return lecturerDtos(examId);
    }

    @Transactional
    public List<LecturerQuestionDto> replaceLecturerQuestions(
            User lecturer, Long examId, List<ExamQuestionRequest> requests) {
        Exam exam = requireOwnedExam(lecturer, examId);
        requireSafeToEdit(exam);
        if (exam.isPublished() && (requests == null || requests.isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A published exam must contain at least one question");
        }
        List<ExamQuestion> existing = questionRepository.findByExamIdOrderByDisplayOrderAsc(examId);
        if (!existing.isEmpty()) {
            optionRepository.deleteByQuestionIdIn(existing.stream().map(ExamQuestion::getId).toList());
            optionRepository.flush();
            questionRepository.deleteAll(existing);
            questionRepository.flush();
        }
        saveQuestions(examId, requests == null ? List.of() : requests);
        return lecturerDtos(examId);
    }

    @Transactional(readOnly = true)
    public List<StudentQuestionDto> getStudentQuestions(User student, Long examId) {
        blockService.requireNotBlocked(examId, student.getId());
        Exam exam = examRepository.findPublishedExamForStudent(examId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
        requireExamAvailable(exam);
        return studentDtos(examId);
    }

    @Transactional(readOnly = true)
    public List<StudentQuestionDto> getSessionQuestions(User student, UUID sessionId) {
        ExamSession session = sessionRepository.findByIdAndStudentId(sessionId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        blockService.requireNotBlocked(session.getExamId(), student.getId());
        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already completed");
        }
        return studentDtos(session.getExamId());
    }

    @Transactional(readOnly = true)
    public List<ExamQuestion> loadQuestions(Long examId) {
        return questionRepository.findByExamIdOrderByDisplayOrderAsc(examId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ExamQuestionOption>> loadOptions(Collection<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return optionRepository.findByQuestionIdInOrderByQuestionIdAscOptionKeyAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(
                        ExamQuestionOption::getQuestionId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private void saveQuestions(Long examId, List<ExamQuestionRequest> requests) {
        for (int index = 0; index < requests.size(); index++) {
            ExamQuestionRequest request = requests.get(index);
            validateQuestion(request, index);
            ExamQuestion question = questionRepository.saveAndFlush(ExamQuestion.builder()
                    .examId(examId)
                    .prompt(request.getText().trim())
                    .displayOrder(index)
                    .points(request.getPoints())
                    .build());
            for (AnswerChoice choice : AnswerChoice.values()) {
                optionRepository.save(ExamQuestionOption.builder()
                        .questionId(question.getId())
                        .optionKey(choice)
                        .optionText(request.getOptions().get(choice).trim())
                        .correctAnswer(choice == request.getCorrectAnswer())
                        .build());
            }
        }
    }

    private void validateQuestion(ExamQuestionRequest request, int index) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            badQuestion(index, "text is required");
        }
        if (request.getPoints() < 1) {
            badQuestion(index, "points must be positive");
        }
        if (request.getCorrectAnswer() == null || request.getOptions() == null
                || request.getOptions().size() != AnswerChoice.values().length) {
            badQuestion(index, "exactly options A through D and a correct answer are required");
        }
        Set<String> unique = new HashSet<>();
        for (AnswerChoice choice : AnswerChoice.values()) {
            String value = request.getOptions().get(choice);
            if (value == null || value.isBlank()) {
                badQuestion(index, "option " + choice + " is required");
            }
            if (!unique.add(value.trim().toLowerCase(Locale.ROOT))) {
                badQuestion(index, "option text must be unique");
            }
        }
    }

    private void badQuestion(int index, String message) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Question " + (index + 1) + ": " + message);
    }

    private Exam requireOwnedExam(User lecturer, Long examId) {
        return examRepository.findByIdAndLecturerId(examId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private void requireSafeToEdit(Exam exam) {
        if (sessionRepository.existsByExamId(exam.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Questions cannot be changed after an attempt has started");
        }
        if (exam.getStatus() == ExamStatus.LIVE || exam.getStatus() == ExamStatus.ENDED
                || !Instant.now().isBefore(exam.getStartTime())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Questions can only be changed before the exam starts");
        }
    }

    private void requireExamAvailable(Exam exam) {
        Instant now = Instant.now();
        Instant end = exam.getEndTime() != null
                ? exam.getEndTime()
                : exam.getStartTime().plusSeconds(exam.getDurationMinutes() * 60L);
        if (now.isBefore(exam.getStartTime()) || now.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam questions are not available");
        }
    }

    private List<LecturerQuestionDto> lecturerDtos(Long examId) {
        List<ExamQuestion> questions = questionRepository.findByExamIdOrderByDisplayOrderAsc(examId);
        Map<Long, List<ExamQuestionOption>> options = loadOptions(
                questions.stream().map(ExamQuestion::getId).toList());
        return questions.stream().map(question -> LecturerQuestionDto.builder()
                .id(question.getId())
                .text(question.getPrompt())
                .order(question.getDisplayOrder())
                .points(question.getPoints())
                .options(toOptionDtos(options.getOrDefault(question.getId(), List.of())))
                .correctAnswer(options.getOrDefault(question.getId(), List.of()).stream()
                        .filter(ExamQuestionOption::isCorrectAnswer)
                        .map(ExamQuestionOption::getOptionKey)
                        .findFirst()
                        .orElseThrow())
                .build()).toList();
    }

    private List<StudentQuestionDto> studentDtos(Long examId) {
        List<ExamQuestion> questions = questionRepository.findByExamIdOrderByDisplayOrderAsc(examId);
        Map<Long, List<ExamQuestionOption>> options = loadOptions(
                questions.stream().map(ExamQuestion::getId).toList());
        return questions.stream().map(question -> StudentQuestionDto.builder()
                .id(question.getId())
                .text(question.getPrompt())
                .order(question.getDisplayOrder())
                .points(question.getPoints())
                .options(toOptionDtos(options.getOrDefault(question.getId(), List.of())))
                .build()).toList();
    }

    private List<ExamOptionDto> toOptionDtos(List<ExamQuestionOption> options) {
        return options.stream().map(option -> ExamOptionDto.builder()
                .key(option.getOptionKey())
                .text(option.getOptionText())
                .build()).toList();
    }
}
