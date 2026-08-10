package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class LecturerDashboardResponse {

    private final LiveExamBannerDto liveExam;
    private final List<NeedsReviewItemDto> needsReview;
    private final ExamTabsDto examTabs;
    private final IntegrityTrendSummaryDto integrityTrend;
    private final List<TopFlaggedBehaviorDto> topFlaggedBehaviors;
}
