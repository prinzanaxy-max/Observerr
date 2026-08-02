package com.backend.observerr.exam.service;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.auth.model.UserRepository;
import com.backend.observerr.config.CacheConfig;
import com.backend.observerr.exam.dto.*;
import com.backend.observerr.exam.model.*;
import com.backend.observerr.exam.repository.*;
import com.backend.observerr.integrity.IntegritySessionService;
import com.backend.observerr.integrity.dto.CompleteExamSessionRequest;
import com.backend.observerr.integrity.dto.CompleteExamSessionResponse;
import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.ExamSessionStatus;
import com.backend.observerr.integrity.repository.ExamSessionRepository;
import com.backend.observerr.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
public class ExamAttemptService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository questionRepository;
    private final ExamQuestionOptionRepository optionRepository;
    private final ExamAnswerRepository answerRepository;
    private final ExamResultRepository resultRepository;
    private final ExamSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final IntegritySessionService integritySessionService;
    private final NotificationService notificationService;
    private final ExamStudentBlockService blockService;

    @Transactional
    public SavedAnswerDto saveAnswer(
            User student, UUID sessionId, Long questionId, SaveAnswerRequest request) {
        ExamSession session = requireStudentSessionForUpdate(student, sessionId);
        ensureInProgress(session);
        ExamQuestion question = requireQuestionForExam(questionId, session.getExamId());
        requireOption(question.getId(), request.getSelectedOption());

        Instant now = Instant.now();
        ExamAnswer answer = answerRepository.findForUpdate(sessionId, questionId)
                .orElseGet(() -> ExamAnswer.builder()
                        .sessionId(sessionId)
                        .questionId(questionId)
                        .build());
        if (answer.isSubmitted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Answer is already submitted");
        }
        answer.setSelectedOptionKey(request.getSelectedOption());
        answer.setSavedAt(now);
        return toSavedAnswer(answerRepository.save(answer));
    }

    @Transactional(readOnly = true)
    public List<SavedAnswerDto> restoreAnswers(User student, UUID sessionId) {
        ExamSession session = sessionRepository.findByIdAndStudentId(sessionId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        blockService.requireNotBlocked(session.getExamId(), student.getId());
        return answerRepository.findBySessionIdOrderByQuestionIdAsc(sessionId)
                .stream().map(this::toSavedAnswer).toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.STUDENT_RESULTS_PAGE_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.STUDENT_RESULTS_SUMMARY_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.STUDENT_RESULT_DETAIL_CACHE, allEntries = true)
    })
    public ExamResultDto submit(User student, UUID sessionId, SubmitExamRequest request) {
        Optional<ExamResult> existing = resultRepository.findBySessionId(sessionId);
        if (existing.isPresent()) {
            requireResultStudent(existing.get(), student);
            return toResultDto(existing.get());
        }

        ExamSession session = requireStudentSessionForUpdate(student, sessionId);
        for (SubmitAnswerDto submitted : request.getAnswers()) {
            SaveAnswerRequest save = new SaveAnswerRequest();
            save.setSelectedOption(submitted.getSelectedOption());
            saveAnswer(student, sessionId, submitted.getQuestionId(), save);
        }
        completeAndGradeInternal(student, session, request.getCompletion());
        ExamResult result = resultRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Exam has no gradeable questions"));
        return toResultDto(result);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.STUDENT_RESULTS_PAGE_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.STUDENT_RESULTS_SUMMARY_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.STUDENT_RESULT_DETAIL_CACHE, allEntries = true)
    })
    public CompleteExamSessionResponse completeAndGrade(
            User student, UUID sessionId, CompleteExamSessionRequest request) {
        ExamSession session = requireStudentSessionForUpdate(student, sessionId);
        return completeAndGradeInternal(student, session, request);
    }

    @Transactional(readOnly = true)
    public List<ExamResultDto> lecturerResults(User lecturer, Long examId) {
        requireOwnedExam(lecturer, examId);
        return resultRepository.findByExamIdOrderBySubmittedAtDesc(examId)
                .stream().map(this::toResultDto).toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.STUDENT_RESULTS_PAGE_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.STUDENT_RESULTS_SUMMARY_CACHE, allEntries = true),
            @CacheEvict(value = CacheConfig.STUDENT_RESULT_DETAIL_CACHE, allEntries = true)
    })
    public List<ExamResultDto> setReleased(
            User lecturer, Long examId, List<Long> resultIds, boolean released) {
        requireOwnedExam(lecturer, examId);
        List<ExamResult> results = resultIds == null || resultIds.isEmpty()
                ? resultRepository.findByExamIdOrderBySubmittedAtDesc(examId)
                : resultRepository.findByExamIdAndIdIn(examId, resultIds);
        if (resultIds != null && !resultIds.isEmpty() && results.size() != new HashSet<>(resultIds).size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more results were not found");
        }
        Instant now = Instant.now();
        for (ExamResult result : results) {
            boolean newlyReleased = released && result.getReleaseStatus() != ExamResultStatus.RELEASED;
            result.setReleaseStatus(released ? ExamResultStatus.RELEASED : ExamResultStatus.PENDING);
            result.setReleasedAt(released ? now : null);
            if (newlyReleased) {
                notificationService.notifyResultReleased(
                        result.getStudentId(), result.getExamId(), result.getId());
            }
        }
        return resultRepository.saveAll(results).stream().map(this::toResultDto).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheConfig.STUDENT_RESULTS_PAGE_CACHE,
            key = "'exam:' + #student.id + ':' + #page + ':' + #size + ':' + #sortKey")
    public ExamResultsPageDto studentResults(User student, int page, int size, String sortKey) {
        Sort sort = switch (sortKey == null ? "MOST_RECENT" : sortKey.toUpperCase(Locale.ROOT)) {
            case "OLDEST" -> Sort.by(Sort.Direction.ASC, "submittedAt");
            case "HIGHEST_INTEGRITY" -> Sort.by(Sort.Direction.DESC, "integrityScore")
                    .and(Sort.by(Sort.Direction.DESC, "submittedAt"));
            case "LOWEST_INTEGRITY" -> Sort.by(Sort.Direction.ASC, "integrityScore")
                    .and(Sort.by(Sort.Direction.DESC, "submittedAt"));
            default -> Sort.by(Sort.Direction.DESC, "submittedAt");
        };
        Page<ExamResult> results = resultRepository.findByStudentIdAndReleaseStatus(
                student.getId(),
                ExamResultStatus.RELEASED,
                PageRequest.of(page, size, sort));
        return ExamResultsPageDto.builder()
                .content(results.getContent().stream().map(this::toResultDto).toList())
                .page(results.getNumber())
                .size(results.getSize())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .build();
    }

    public ExamResultsPageDto studentResults(User student, int page, int size) {
        return studentResults(student, page, size, "MOST_RECENT");
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.STUDENT_RESULT_DETAIL_CACHE, key = "#student.id + ':' + #resultId")
    public ExamResultDetailDto studentResultDetail(User student, Long resultId) {
        ExamResult result = resultRepository.findByIdAndStudentIdAndReleaseStatus(
                        resultId, student.getId(), ExamResultStatus.RELEASED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        return ExamResultDetailDto.builder()
                .result(toResultDto(result))
                .analysis(buildAnalysis(result))
                .build();
    }

    private CompleteExamSessionResponse completeAndGradeInternal(
            User student, ExamSession session, CompleteExamSessionRequest request) {
        CompleteExamSessionResponse response =
                integritySessionService.completeSession(student, session.getId(), request);
        if (resultRepository.findBySessionId(session.getId()).isEmpty()) {
            grade(session.getId());
        }
        return response;
    }

    private void grade(UUID sessionId) {
        ExamSession completed = sessionRepository.findById(sessionId).orElseThrow();
        List<ExamQuestion> questions =
                questionRepository.findByExamIdOrderByDisplayOrderAsc(completed.getExamId());
        if (questions.isEmpty()) {
            return;
        }
        List<ExamAnswer> answers = answerRepository.findBySessionIdOrderByQuestionIdAsc(sessionId);
        Map<Long, ExamAnswer> answerByQuestion = answers.stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, Function.identity()));
        Map<Long, AnswerChoice> correct = correctAnswers(questions);

        int score = questions.stream()
                .filter(question -> {
                    ExamAnswer answer = answerByQuestion.get(question.getId());
                    return answer != null
                            && answer.getSelectedOptionKey() == correct.get(question.getId());
                })
                .mapToInt(ExamQuestion::getPoints)
                .sum();
        int maxScore = questions.stream().mapToInt(ExamQuestion::getPoints).sum();
        Instant submittedAt = completed.getEndedAt() != null ? completed.getEndedAt() : Instant.now();
        for (ExamAnswer answer : answers) {
            answer.setSubmitted(true);
            answer.setSubmittedAt(submittedAt);
        }
        answerRepository.saveAll(answers);

        Exam exam = examRepository.findById(completed.getExamId()).orElseThrow();
        ExamResult result = resultRepository.save(ExamResult.builder()
                .examId(exam.getId())
                .sessionId(sessionId)
                .studentId(completed.getStudentId())
                .lecturerId(exam.getLecturerId())
                .academicScore(score)
                .maxScore(maxScore)
                .integrityScore(completed.getFinalScore() == null ? 0 : completed.getFinalScore())
                .requiresReview(completed.isRequiresReview())
                .releaseStatus(ExamResultStatus.PENDING)
                .submittedAt(submittedAt)
                .build());
        notificationService.notifyStudentCompleted(
                exam.getLecturerId(), completed.getStudentId(), exam.getId());
    }

    private List<QuestionAnalysisDto> buildAnalysis(ExamResult result) {
        List<ExamQuestion> questions =
                questionRepository.findByExamIdOrderByDisplayOrderAsc(result.getExamId());
        Map<Long, ExamAnswer> answers = answerRepository.findBySessionIdOrderByQuestionIdAsc(result.getSessionId())
                .stream().collect(Collectors.toMap(ExamAnswer::getQuestionId, Function.identity()));
        Map<Long, AnswerChoice> correct = correctAnswers(questions);
        return questions.stream().map(question -> {
            ExamAnswer answer = answers.get(question.getId());
            AnswerChoice selected = answer == null ? null : answer.getSelectedOptionKey();
            boolean isCorrect = selected != null && selected == correct.get(question.getId());
            return QuestionAnalysisDto.builder()
                    .questionId(question.getId())
                    .question(question.getPrompt())
                    .selectedAnswer(selected)
                    .correctAnswer(correct.get(question.getId()))
                    .correct(isCorrect)
                    .pointsEarned(isCorrect ? question.getPoints() : 0)
                    .pointsPossible(question.getPoints())
                    .build();
        }).toList();
    }

    private Map<Long, AnswerChoice> correctAnswers(List<ExamQuestion> questions) {
        Set<Long> ids = questions.stream().map(ExamQuestion::getId).collect(Collectors.toSet());
        return optionRepository.findByQuestionIdInOrderByQuestionIdAscOptionKeyAsc(ids).stream()
                .filter(ExamQuestionOption::isCorrectAnswer)
                .collect(Collectors.toMap(
                        ExamQuestionOption::getQuestionId, ExamQuestionOption::getOptionKey));
    }

    private ExamSession requireStudentSessionForUpdate(User student, UUID sessionId) {
        ExamSession session = sessionRepository.findByIdAndStudentIdForUpdate(sessionId, student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        blockService.requireNotBlocked(session.getExamId(), student.getId());
        return session;
    }

    private void ensureInProgress(ExamSession session) {
        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already completed");
        }
    }

    private ExamQuestion requireQuestionForExam(Long questionId, Long examId) {
        ExamQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!question.getExamId().equals(examId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        return question;
    }

    private void requireOption(Long questionId, AnswerChoice choice) {
        boolean exists = optionRepository.findByQuestionIdOrderByOptionKeyAsc(questionId)
                .stream().anyMatch(option -> option.getOptionKey() == choice);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid answer option");
        }
    }

    private Exam requireOwnedExam(User lecturer, Long examId) {
        return examRepository.findByIdAndLecturerId(examId, lecturer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private void requireResultStudent(ExamResult result, User student) {
        if (!result.getStudentId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found");
        }
    }

    private SavedAnswerDto toSavedAnswer(ExamAnswer answer) {
        return SavedAnswerDto.builder()
                .questionId(answer.getQuestionId())
                .selectedOption(answer.getSelectedOptionKey())
                .savedAt(answer.getSavedAt().toString())
                .submitted(answer.isSubmitted())
                .build();
    }

    private ExamResultDto toResultDto(ExamResult result) {
        Exam exam = examRepository.findById(result.getExamId()).orElseThrow();
        User student = userRepository.findById(result.getStudentId()).orElseThrow();
        String studentName = String.join(" ",
                Optional.ofNullable(student.getFirstName()).orElse(""),
                Optional.ofNullable(student.getLastName()).orElse("")).trim();
        return ExamResultDto.builder()
                .id(result.getId())
                .examId(result.getExamId())
                .sessionId(result.getSessionId().toString())
                .studentId(result.getStudentId())
                .studentName(studentName.isBlank() ? student.getInstitutionalId() : studentName)
                .examTitle(exam.getTitle())
                .courseCode(exam.getCourseCode())
                .academicScore(result.getAcademicScore())
                .maxScore(result.getMaxScore())
                .percentage(result.getMaxScore() == 0
                        ? 0
                        : Math.round(result.getAcademicScore() * 10000.0 / result.getMaxScore()) / 100.0)
                .integrityScore(result.getIntegrityScore())
                .requiresReview(result.isRequiresReview())
                .status(result.getReleaseStatus().name())
                .submittedAt(result.getSubmittedAt().toString())
                .releasedAt(result.getReleasedAt() == null ? null : result.getReleasedAt().toString())
                .build();
    }
}
