package com.auditor.analysis.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auditor.analysis.maintainability")
public class MaintainabilityProperties {

    private int longMethodStatements = 40;
    private int maxParameters = 5;
    private int maxNestingDepth = 4;
    private int godClassComplexity = 50;

    public int getLongMethodStatements() {
        return longMethodStatements;
    }

    public void setLongMethodStatements(int value) {
        this.longMethodStatements = value;
    }

    public int getMaxParameters() {
        return maxParameters;
    }

    public void setMaxParameters(int value) {
        this.maxParameters = value;
    }

    public int getMaxNestingDepth() {
        return maxNestingDepth;
    }

    public void setMaxNestingDepth(int value) {
        this.maxNestingDepth = value;
    }

    public int getGodClassComplexity() {
        return godClassComplexity;
    }

    public void setGodClassComplexity(int value) {
        this.godClassComplexity = value;
    }
}