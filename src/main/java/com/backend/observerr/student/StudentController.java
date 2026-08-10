package com.backend.observerr.student;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @GetMapping("/hello")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello Student! Your exam is ready.");
    }
}
