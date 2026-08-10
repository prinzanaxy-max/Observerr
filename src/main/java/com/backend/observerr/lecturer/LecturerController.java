package com.backend.observerr.lecturer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lecturer")
public class LecturerController {

    @GetMapping("/hello")
    @PreAuthorize("hasRole('LECTURER')")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello Lecturer! Your dashboard is ready.");
    }
}
