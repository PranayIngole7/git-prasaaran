package com.pranay.gitprasaaran.api.activity;

import org.springframework.data.domain.Page;

import java.util.List;

public record ActivityPageResponse(
        List<ActivityEventResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ActivityPageResponse from(Page<ActivityEventResponse> events) {
        return new ActivityPageResponse(
                events.getContent(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages()
        );
    }
}
