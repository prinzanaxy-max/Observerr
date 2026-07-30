package com.backend.observerr.lecturer.dashboard;

import com.backend.observerr.integrity.model.ExamSession;
import com.backend.observerr.integrity.model.IntegrityEvent;
import com.backend.observerr.integrity.repository.IntegrityEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LecturerSessionInsights {

    private final IntegrityEventRepository integrityEventRepository;

    public int resolveIntegrityScore(ExamSession session) {
        if (session.getFinalScore() != null) {
            return session.getFinalScore();
        }
        List<IntegrityEvent> events = integrityEventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(session.getId());
        if (!events.isEmpty()) {
            return events.get(events.size() - 1).getScoreAfter();
        }
        return Math.max(0, session.getStartingScore() - session.getTotalDeductions());
    }

    public IntegrityEvent latestEvent(UUID sessionId) {
        List<IntegrityEvent> events = integrityEventRepository.findBySessionIdOrderByOccurredAtAscIdAsc(sessionId);
        return events.isEmpty() ? null : events.get(events.size() - 1);
    }
}
