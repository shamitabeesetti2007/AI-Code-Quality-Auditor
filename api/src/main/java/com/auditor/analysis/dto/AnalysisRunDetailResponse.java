package com.auditor.analysis.dto;

import com.auditor.analysis.AnalysisRun;

import java.math.BigDecimal;
import java.util.List;

public record AnalysisRunDetailResponse(
        AnalysisRunSummaryResponse run,
        BigDecimal score,
        String grade,
        List<AnalysisMetricResponse> metrics,
        List<AnalysisFindingResponse> findings) {

    public static AnalysisRunDetailResponse from(
            AnalysisRun run,
            List<AnalysisMetricResponse> metrics,
            List<AnalysisFindingResponse> findings) {
        return new AnalysisRunDetailResponse(
                AnalysisRunSummaryResponse.from(run),
                run.getScore(),
                run.getGrade(),
                metrics,
                findings);
    }
}
