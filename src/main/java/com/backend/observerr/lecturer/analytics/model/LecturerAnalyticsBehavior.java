package com.backend.observerr.lecturer.analytics.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lecturer_analytics_behaviors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerAnalyticsBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "overview_id", nullable = false)
    private LecturerAnalyticsOverview overview;

    @Column(name = "behavior_code", nullable = false, length = 50)
    private String behaviorCode;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "event_count", nullable = false)
    private int eventCount;

    @Column(nullable = false, length = 50)
    private String icon;

    @Column(nullable = false, length = 20)
    private String tone;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
