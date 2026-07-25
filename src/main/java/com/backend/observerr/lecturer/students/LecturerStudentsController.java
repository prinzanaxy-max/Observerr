package com.backend.observerr.lecturer.students;

import com.backend.observerr.auth.model.User;
import com.backend.observerr.lecturer.students.dto.LecturerStudentsPageDto;
import com.backend.observerr.lecturer.students.dto.ProctoringSessionDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lecturer/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class LecturerStudentsController {

    private final LecturerStudentsService lecturerStudentsService;

    @GetMapping
    public ResponseEntity<LecturerStudentsPageDto> getStudents(
            @AuthenticationPrincipal User lecturer,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String course) {
        return ResponseEntity.ok(lecturerStudentsService.getStudents(lecturer, page, size, search, course));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ProctoringSessionDetailDto> getSessionDetail(
            @AuthenticationPrincipal User lecturer,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(lecturerStudentsService.getSessionDetail(lecturer, sessionId));
    }
}
