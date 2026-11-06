package com.auditor.analysis;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.auditor.analysis.contract.AnalysisEngineResult;
import java.util.UUID;

/** Coordinates result persistence and records terminal failure after a rollback. */
@Service
public class AnalyzerResultPersistenceService {

    private final AnalyzerResultPersistenceTransaction transaction;
    private final AnalysisRunService runService;

    public AnalyzerResultPersistenceService(
            AnalyzerResultPersistenceTransaction transaction,
            AnalysisRunService runService) {
        this.transaction = transaction;
        this.runService = runService;
    }

    public void persist(UUID runId, UUID ownerId, AnalysisEngineResult result) {
    try {
        transaction.persist(runId, ownerId, result);
    } catch (ResponseStatusException exception) {
        throw exception;
    } catch (RuntimeException exception) {
        runService.failAfterPersistenceError(runId, ownerId);
        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Analysis result could not be persisted.");
    }
}
}
