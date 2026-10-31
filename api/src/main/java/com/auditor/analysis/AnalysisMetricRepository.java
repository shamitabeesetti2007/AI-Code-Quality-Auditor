package com.auditor.analysis;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface AnalysisMetricRepository
        extends Repository<AnalysisMetric, UUID> {

    List<AnalysisMetric> findAllByRun_RunIdAndRun_Owner_UserId(
            UUID runId,
            UUID ownerUserId);

    <S extends AnalysisMetric> S save(S metric);
}
