package com.auditor.analysis;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

public interface AnalysisFindingRepository
        extends Repository<AnalysisFinding, UUID> {

    List<AnalysisFinding> findAllByRun_RunIdAndRun_Owner_UserId(
            UUID runId,
            UUID ownerUserId);

    List<AnalysisFinding>
            findAllByRun_RunIdAndRun_Owner_UserIdOrderByRuleIdAscFingerprintAscFindingIdAsc(
                    UUID runId,
                    UUID ownerUserId);

    List<AnalysisFinding> findAllByRun_RunIdInAndRun_Owner_UserIdOrderByRun_RunIdAscRuleIdAscFingerprintAscFindingIdAsc(
            List<UUID> runIds, UUID ownerUserId);

    <S extends AnalysisFinding> S save(S finding);
}
