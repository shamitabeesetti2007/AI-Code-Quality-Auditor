package com.auditor.analysis.engine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AnalysisEngineConfiguration {

    @Bean
    public AnalyzerRegistry analyzerRegistry() {
        return new AnalyzerRegistry(
                new JavaSourceParser(),
                List.of(new ComplexityAnalyzer()));
    }
}
