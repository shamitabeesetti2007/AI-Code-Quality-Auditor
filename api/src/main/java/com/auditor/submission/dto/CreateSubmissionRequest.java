package com.auditor.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateSubmissionRequest {

    @NotBlank
    @Size(max = 120)
    private String title;

    @NotBlank
    private String language;

    @NotBlank
    private String originType;

    @NotBlank
    private String sourceCode;

    private boolean autoAnalyze = true;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getOriginType() {
        return originType;
    }

    public void setOriginType(String originType) {
        this.originType = originType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public boolean isAutoAnalyze() {
        return autoAnalyze;
    }

    public void setAutoAnalyze(boolean autoAnalyze) {
        this.autoAnalyze = autoAnalyze;
    }
}
