package com.backend.observerr.lecturer.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ProctoringFeedsResponse {

    private final Long examId;
    private final String roomName;
    private final List<ProctoringFeedDto> feeds;
}
