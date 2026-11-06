package com.auditor.analysis;

public class ActiveAnalysisRunConflictException extends RuntimeException {

    public ActiveAnalysisRunConflictException() {
        super("An analysis run is already active for this submission.");
    }
}
