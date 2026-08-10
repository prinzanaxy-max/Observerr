package com.backend.observerr.lecturer.analytics.repository;

import com.backend.observerr.lecturer.analytics.model.LecturerAnalyticsOverview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LecturerAnalyticsOverviewRepository extends JpaRepository<LecturerAnalyticsOverview, Long> {

    Optional<LecturerAnalyticsOverview> findByLecturerIdAndPeriod(Long lecturerId, String period);
}
