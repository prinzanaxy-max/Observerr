package com.backend.observerr.auth.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByInstitutionalId(String institutionalId);
    boolean existsByEmail(String email);
    boolean existsByInstitutionalId(String institutionalId);
}
