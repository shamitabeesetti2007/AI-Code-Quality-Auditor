package com.auditor.analysis.engine;

import com.auditor.analysis.AnalysisScoringPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;

@Configuration
@EnableConfigurationProperties(AnalysisScoringPolicy.class)
public class AnalysisEngineConfiguration {

    @Bean
    public AnalyzerRegistry analyzerRegistry() {
        return new AnalyzerRegistry(
                new JavaSourceParser(),
                List.of(
                        new ComplexityAnalyzer(),
                        new MaintainabilityAnalyzer()
                ));
    }
}
