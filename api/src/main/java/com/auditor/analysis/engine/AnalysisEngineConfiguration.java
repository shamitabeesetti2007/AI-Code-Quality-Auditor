
package com.auditor.analysis.engine;

import com.auditor.analysis.AnalysisScoringPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties({
        AnalysisScoringPolicy.class,
        MaintainabilityProperties.class
})
public class AnalysisEngineConfiguration {

    @Bean
    public MaintainabilityAnalyzer maintainabilityAnalyzer(
            MaintainabilityProperties properties) {

        return new MaintainabilityAnalyzer(List.of(
                new LongMethodRule(properties),
                new LongParameterListRule(properties),
                new DeepNestingRule(properties),
                new MagicNumberRule(),
                new EmptyCatchRule(),
                new IgnoredExceptionRule(),
                new PrintStackTraceRule(),
                new SystemOutRule(),
                new GodClassRule(properties)
        ));
    }

    @Bean
    public AnalyzerRegistry analyzerRegistry(
            MaintainabilityAnalyzer maintainabilityAnalyzer,
            @Value("${auditor.analysis.pmd.enabled:false}") boolean pmdEnabled) {

        List<Analyzer> analyzers = new ArrayList<>(List.of(
                new ComplexityAnalyzer(),
                maintainabilityAnalyzer
        ));

        if (pmdEnabled) {
            analyzers.add(new PmdAnalyzerAdapter());
        }

        return new AnalyzerRegistry(
                new JavaSourceParser(),
                analyzers
        );
    }
}
