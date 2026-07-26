package com.backend.observerr.lecturer.analytics.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "lecturer_analytics_overviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_lecturer_analytics_period",
                columnNames = {"lecturer_id", "period"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerAnalyticsOverview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lecturer_id", nullable = false)
    private Long lecturerId;

    @Column(nullable = false, length = 10)
    private String period;

    @Column(name = "total_exams_monitored", nullable = false)
    private int totalExamsMonitored;

    @Column(name = "exams_change_percent", precision = 5, scale = 2)
    private BigDecimal examsChangePercent;

    @Column(name = "exams_change_direction", nullable = false, length = 10)
    private String examsChangeDirection;

    @Column(name = "exams_change_label", nullable = false, length = 50)
    private String examsChangeLabel;

    @Column(name = "total_flagged_events", nullable = false)
    private int totalFlaggedEvents;

    @Column(name = "flags_change_percent", precision = 5, scale = 2)
    private BigDecimal flagsChangePercent;

    @Column(name = "flags_change_direction", nullable = false, length = 10)
    private String flagsChangeDirection;

    @Column(name = "flags_change_label", nullable = false, length = 50)
    private String flagsChangeLabel;

    @Column(name = "avg_integrity_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal avgIntegrityScore;

    @Column(name = "integrity_change_percent", precision = 5, scale = 2)
    private BigDecimal integrityChangePercent;

    @Column(name = "integrity_change_direction", nullable = false, length = 10)
    private String integrityChangeDirection;

    @Column(name = "integrity_change_label", nullable = false, length = 50)
    private String integrityChangeLabel;

    @Column(name = "most_common_flag_label", nullable = false, length = 100)
    private String mostCommonFlagLabel;

    @Column(name = "most_common_flag_share_percent", nullable = false)
    private int mostCommonFlagSharePercent;

    @Column(name = "most_common_flag_icon", nullable = false, length = 50)
    private String mostCommonFlagIcon;

    @Column(name = "trend_granularity", nullable = false, length = 10)
    private String trendGranularity;

    @Column(name = "trend_subtitle", nullable = false)
    private String trendSubtitle;

    @Builder.Default
    @OneToMany(mappedBy = "overview", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<LecturerAnalyticsTrendPoint> trendPoints = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "overview", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<LecturerAnalyticsBehavior> behaviors = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
