/**
 * Mock Findings Fixtures
 * Grounded in Project Bible: Section 4 (Rules Catalogue: SEC-001, COMP-001, MAINT-001, DUP-001, SYN-001).
 */

import type { Finding } from '../types/finding';

export const MOCK_FINDING_SEC_001: Finding = {
  findingId: 'fnd-89a1-001',
  runId: 'run-c7a2-001',
  ruleId: 'rule-sec-001',
  ruleCode: 'SEC-001',
  dimension: 'SECURITY',
  cweId: 89, // CWE-89: SQL Injection
  severity: 'HIGH',
  startLine: 12,
  endLine: 12,
  message: 'SQL query constructed via string concatenation with untrusted parameter into Statement.executeQuery()',
  snippet: 'ResultSet rs = st.executeQuery(query);',
  fingerprint: 'a89f4b321c7d0e9821a4f00b21e89c678a10b45d290e8a71b23456cde9870123',
  confidence: 'HIGH',
  startColumn: 24,
  endColumn: 46,
  scoreDeduction: 15.0,
};

export const MOCK_FINDING_COMP_001: Finding = {
  findingId: 'fnd-45b2-002',
  runId: 'run-88ae-002',
  ruleId: 'rule-comp-001',
  ruleCode: 'COMP-001',
  dimension: 'COMPLEXITY',
  cweId: null,
  severity: 'MEDIUM',
  startLine: 7,
  endLine: 28,
  message: 'Method processDiscount() cyclomatic complexity of 14 exceeds threshold (10)',
  snippet: 'public double processDiscount(List<String> items, ...',
  fingerprint: 'b45c7890123456def890123456789abcde0123456789abcdef0123456789abcd',
  confidence: 'HIGH',
  startColumn: 5,
  endColumn: 35,
  scoreDeduction: 8.0,
};

export const MOCK_FINDING_MAINT_001: Finding = {
  findingId: 'fnd-12c3-003',
  runId: 'run-c7a2-001',
  ruleId: 'rule-maint-001',
  ruleCode: 'MAINT-001',
  dimension: 'MAINTAINABILITY',
  cweId: null,
  severity: 'LOW',
  startLine: 16,
  endLine: 18,
  message: 'Empty catch block: SQLException is caught without logging, rethrowing, or error recovery',
  snippet: '} catch (SQLException ignored) { }',
  fingerprint: 'c7890123456789abcdef0123456789abcdef0123456789abcdef0123456789ab',
  confidence: 'HIGH',
  startColumn: 9,
  endColumn: 39,
  scoreDeduction: 3.0,
};

export const MOCK_FINDING_SEC_003: Finding = {
  findingId: 'fnd-99d4-004',
  runId: 'run-d41c-003',
  ruleId: 'rule-sec-003',
  ruleCode: 'SEC-003',
  dimension: 'SECURITY',
  cweId: 327, // CWE-327: Use of a Broken or Risky Cryptographic Algorithm
  severity: 'MEDIUM',
  startLine: 14,
  endLine: 14,
  message: "Use of broken cryptographic hash algorithm: MessageDigest.getInstance('MD5')",
  snippet: 'MessageDigest md = MessageDigest.getInstance("MD5");',
  fingerprint: 'd0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcde',
  confidence: 'HIGH',
  startColumn: 32,
  endColumn: 59,
  scoreDeduction: 6.0,
};

export const MOCK_FINDING_SYN_001: Finding = {
  findingId: 'fnd-00e5-005',
  runId: 'run-e32a-005',
  ruleId: 'rule-syn-001',
  ruleCode: 'SYN-001',
  dimension: 'MAINTAINABILITY',
  cweId: null,
  severity: 'HIGH',
  startLine: 5,
  endLine: 5,
  message: "JavaParser syntax error: ';' expected at token 'unclosedMethod'",
  snippet: 'System.out.println("Missing closing brace and semicolon"',
  fingerprint: 'e123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef',
  confidence: 'HIGH',
  startColumn: 64,
  endColumn: 65,
  scoreDeduction: 0,
};

export const MOCK_FINDINGS: Finding[] = [
  MOCK_FINDING_SEC_001,
  MOCK_FINDING_COMP_001,
  MOCK_FINDING_MAINT_001,
  MOCK_FINDING_SEC_003,
  MOCK_FINDING_SYN_001,
];
