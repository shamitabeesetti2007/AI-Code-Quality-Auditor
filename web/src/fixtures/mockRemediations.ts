/**
 * Mock Remediation & Verification Fixtures
 * Grounded in Project Bible: Section 5 (Component C: AI Remediation) and Section 11 (API).
 */

import type { Remediation } from '../types/remediation';
import { MOCK_FINDING_SEC_001 } from './mockFindings';
import { SAMPLE_REMEDIATED_LOGIN_DAO } from './sampleJavaSnippets';

/**
 * Verified Clean Remediation for SEC-001 (SQL Injection in LoginDao.java)
 * Strategy: GUIDED (augmented with CWE-89 key-lookup guidance passage)
 * Outcome: VERIFIED_FIX (Target resolved, 0 new findings, signatures preserved)
 */
export const MOCK_REMEDIATION_SEC_001: Remediation = {
  remediationId: 'rem-5f3d-001',
  findingId: MOCK_FINDING_SEC_001.findingId,
  originalSubmissionId: 'sub-9b1e-001',
  patchedSubmissionId: 'sub-7f2a-004',
  patchedRunId: 'run-88ae-004',
  generationId: 'gen-99cf-002',
  strategy: 'GUIDED',
  outcome: 'VERIFIED_FIX',
  targetResolved: true,
  introducedCount: 0,
  testsPassed: true, // Sandboxed JUnit test passed (Bible Section 3)
  editFootprint: 0.08, // 8% of method lines changed
  verification: {
    steps: [
      { name: 'SCHEMA_VALID', passed: true, detail: 'JSON payload and structure match schema' },
      { name: 'PARSES', passed: true, detail: 'JavaParser built AST with 0 syntax diagnostics' },
      {
        name: 'SIGNATURES_PRESERVED',
        passed: true,
        detail: 'Public method authenticate(Connection, String, String) preserved',
      },
      {
        name: 'FOOTPRINT_WITHIN_LIMIT',
        passed: true,
        detail: 'Edit footprint (8.0%) is below maximum threshold (30.0%)',
      },
      {
        name: 'TARGET_RESOLVED',
        passed: true,
        detail: 'Target finding SEC-001 fingerprint resolved in patched AST',
      },
      {
        name: 'NO_NEW_FINDINGS',
        passed: true,
        detail: 'Zero new findings with severity >= MEDIUM introduced',
      },
      {
        name: 'TESTS_PASS',
        passed: true,
        detail: 'All 4 sandbox unit tests for authentication succeeded',
      },
    ],
  },
  diff: `@@ -10,4 +10,7 @@
-        Statement st = conn.createStatement();
-        String query = "SELECT * FROM accounts WHERE username='" + user + "' AND password='" + pass + "'";
-        ResultSet rs = st.executeQuery(query);
-        return rs.next();
+        String sql = "SELECT * FROM accounts WHERE username = ? AND password = ?";
+        try (PreparedStatement ps = conn.prepareStatement(sql)) {
+            ps.setString(1, user);
+            ps.setString(2, pass);
+            try (ResultSet rs = ps.executeQuery()) {
+                return rs.next();
+            }
+        }`,
  patchedCode: SAMPLE_REMEDIATED_LOGIN_DAO,
  explanationSummary:
    'Replaced dynamic Statement concatenation with parameterized PreparedStatement to prevent SQL injection.',
};

/**
 * Illustrative Regression Outcome Fixture (for testing UI states)
 * Outcome: REGRESSION (Target resolved, but introduced a new high-severity finding)
 */
export const MOCK_REMEDIATION_REGRESSION: Remediation = {
  remediationId: 'rem-7a2e-002',
  findingId: 'fnd-89a1-099',
  originalSubmissionId: 'sub-9b1e-001',
  patchedSubmissionId: 'sub-99ee-006',
  patchedRunId: 'run-77bb-006',
  generationId: 'gen-11aa-003',
  strategy: 'BARE',
  outcome: 'REGRESSION',
  targetResolved: true,
  introducedCount: 1,
  testsPassed: true,
  editFootprint: 0.15,
  verification: {
    steps: [
      { name: 'SCHEMA_VALID', passed: true },
      { name: 'PARSES', passed: true },
      { name: 'SIGNATURES_PRESERVED', passed: true },
      { name: 'FOOTPRINT_WITHIN_LIMIT', passed: true },
      { name: 'TARGET_RESOLVED', passed: true },
      {
        name: 'NO_NEW_FINDINGS',
        passed: false,
        detail: 'Introduced 1 new HIGH severity finding: SEC-002 (Hardcoded token)',
      },
      { name: 'TESTS_PASS', passed: true },
    ],
  },
  explanationSummary:
    'Fix resolved SQL injection but hardcoded a fallback administrative token in method body.',
};

/**
 * Illustrative Broken Outcome Fixture (for testing UI states)
 * Outcome: BROKEN (Patch failed to parse due to missing syntax token)
 */
export const MOCK_REMEDIATION_BROKEN: Remediation = {
  remediationId: 'rem-3b1c-003',
  findingId: 'fnd-89a1-099',
  originalSubmissionId: 'sub-9b1e-001',
  patchedSubmissionId: null,
  patchedRunId: null,
  generationId: 'gen-22bb-004',
  strategy: 'BARE',
  outcome: 'BROKEN',
  targetResolved: false,
  introducedCount: 0,
  testsPassed: false,
  editFootprint: 0.05,
  verification: {
    steps: [
      { name: 'SCHEMA_VALID', passed: true },
      {
        name: 'PARSES',
        passed: false,
        detail: "JavaParser syntax error: ';' expected at line 14",
      },
      { name: 'SIGNATURES_PRESERVED', passed: false },
      { name: 'FOOTPRINT_WITHIN_LIMIT', passed: true },
      { name: 'TARGET_RESOLVED', passed: false },
      { name: 'NO_NEW_FINDINGS', passed: false },
      { name: 'TESTS_PASS', passed: false },
    ],
  },
  explanationSummary: 'LLM returned malformed Java snippet missing semicolon.',
};

export const MOCK_REMEDIATION_PARTIAL: Remediation = {
  remediationId: 'rem-4c2d-004',
  findingId: MOCK_FINDING_SEC_001.findingId,
  originalSubmissionId: 'sub-9b1e-001',
  patchedSubmissionId: 'sub-6e1f-005',
  patchedRunId: 'run-55aa-005',
  generationId: 'gen-33cc-005',
  strategy: 'GUIDED',
  outcome: 'PARTIAL',
  targetResolved: true,
  introducedCount: 1, // Only LOW/INFO smells introduced
  testsPassed: true,
  editFootprint: 0.12,
  verification: {
    steps: [
      { name: 'SCHEMA_VALID', passed: true },
      { name: 'PARSES', passed: true },
      { name: 'SIGNATURES_PRESERVED', passed: true },
      { name: 'FOOTPRINT_WITHIN_LIMIT', passed: true },
      { name: 'TARGET_RESOLVED', passed: true, detail: 'SQL injection resolved with PreparedStatement' },
      {
        name: 'NO_NEW_FINDINGS',
        passed: true,
        detail: 'Zero findings >= MEDIUM. Note: 1 LOW maintainability smell introduced (unused import)',
      },
      { name: 'TESTS_PASS', passed: true },
    ],
  },
  explanationSummary:
    'Target vulnerability resolved, but introduced an unused java.sql.CallableStatement import.',
};

export const MOCK_REMEDIATION_NO_EFFECT: Remediation = {
  remediationId: 'rem-8e9a-005',
  findingId: MOCK_FINDING_SEC_001.findingId,
  originalSubmissionId: 'sub-9b1e-001',
  patchedSubmissionId: 'sub-1a2b-007',
  patchedRunId: 'run-33dd-007',
  generationId: 'gen-44dd-006',
  strategy: 'BARE',
  outcome: 'NO_EFFECT',
  targetResolved: false,
  introducedCount: 0,
  testsPassed: false,
  editFootprint: 0.04,
  verification: {
    steps: [
      { name: 'SCHEMA_VALID', passed: true },
      { name: 'PARSES', passed: true },
      { name: 'SIGNATURES_PRESERVED', passed: true },
      { name: 'FOOTPRINT_WITHIN_LIMIT', passed: true },
      {
        name: 'TARGET_RESOLVED',
        passed: false,
        detail: 'Target finding SEC-001 remains unresolved: dynamic SQL concatenation still present',
      },
      { name: 'NO_NEW_FINDINGS', passed: true },
      { name: 'TESTS_PASS', passed: false },
    ],
  },
  explanationSummary:
    'Patch refactored variable names but still passed concatenated string to Statement.executeQuery().',
};

export const MOCK_REMEDIATIONS: Remediation[] = [
  MOCK_REMEDIATION_SEC_001,
  MOCK_REMEDIATION_PARTIAL,
  MOCK_REMEDIATION_NO_EFFECT,
  MOCK_REMEDIATION_REGRESSION,
  MOCK_REMEDIATION_BROKEN,
];
