package com.auditor.analysis;

import com.auditor.analysis.dto.AnalysisRunDetailResponse;
import com.auditor.analysis.dto.AnalysisRunSummaryResponse;
import com.auditor.common.AuthenticatedUserId;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class AnalysisRunController {

    private final AnalysisRunService analysisRunService;

    public AnalysisRunController(AnalysisRunService analysisRunService) {
        this.analysisRunService = analysisRunService;
    }

    @PostMapping("/api/v1/submissions/{submissionId}/analysis-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisRunSummaryResponse create(
            Authentication authentication,
            @PathVariable UUID submissionId) {
        return analysisRunService.createQueuedRun(
                submissionId, AuthenticatedUserId.from(authentication));
    }

    @GetMapping("/api/v1/submissions/{submissionId}/analysis-runs")
    public List<AnalysisRunSummaryResponse> list(
            Authentication authentication,
            @PathVariable UUID submissionId) {
        return analysisRunService.listRuns(
                submissionId, AuthenticatedUserId.from(authentication));
    }

    @GetMapping("/api/v1/analysis-runs/{runId}")
    public AnalysisRunDetailResponse get(
            Authentication authentication,
            @PathVariable UUID runId) {
        return analysisRunService.getRun(
                runId, AuthenticatedUserId.from(authentication));
    }
}
