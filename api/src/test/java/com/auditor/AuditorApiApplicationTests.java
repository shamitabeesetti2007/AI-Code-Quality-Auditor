package com.auditor;

import com.auditor.analysis.EngineCodeAnalyzerAdapter;
import com.auditor.analysis.AnalysisScoringPolicy;
import com.auditor.analysis.contract.CodeAnalyzer;
import com.auditor.analysis.engine.AnalyzerRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuditorApiApplicationTests {

	@Autowired
	private CodeAnalyzer codeAnalyzer;

	@Autowired
	private AnalyzerRegistry analyzerRegistry;

	@Autowired
	private AnalysisScoringPolicy scoringPolicy;

	@Test
	void contextLoads() {
		Assertions.assertInstanceOf(EngineCodeAnalyzerAdapter.class, codeAnalyzer);
		Assertions.assertNotNull(analyzerRegistry);
		Assertions.assertEquals("complexity-v1", scoringPolicy.getVersion());
		Assertions.assertEquals(
				new java.math.BigDecimal("15"),
				scoringPolicy.getSeverityDeductions().get("HIGH"));
	}

}
