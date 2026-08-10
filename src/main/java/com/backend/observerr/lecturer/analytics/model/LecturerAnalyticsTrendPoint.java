package com.backend.observerr.lecturer.analytics.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecturer_analytics_trend_points")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerAnalyticsTrendPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "overview_id", nullable = false)
    private LecturerAnalyticsOverview overview;

    @Column(nullable = false, length = 10)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "monitored_sessions", nullable = false)
    private int monitoredSessions;

    @Column(name = "flagged_events", nullable = false)
    private int flaggedEvents;

    @Column(nullable = false)
    private boolean alert;
}
