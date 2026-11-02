package com.auditor.analysis.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record AnalysisRunHistoryResponse(
        List<AnalysisRunHistoryItemResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public AnalysisRunHistoryResponse {
        items = List.copyOf(items);
    }

    public static AnalysisRunHistoryResponse from(
            Page<AnalysisRunHistoryItemResponse> result) {
        return new AnalysisRunHistoryResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
