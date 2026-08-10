package com.backend.observerr.exam.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReleaseResultsRequest {
    private List<Long> resultIds = new ArrayList<>();
}
