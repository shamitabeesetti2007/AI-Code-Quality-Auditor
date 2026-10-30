/**
 * Mock Analysis Runs & Quality Scoring Fixtures
 * Grounded in Project Bible: Section 4 (Scoring Model & Pipeline) and Section 9 (DDL: analysis_run).
 */

import type { AnalysisRun, AnalysisReport } from '../types/analysis';
import { MOCK_FINDING_SEC_001, MOCK_FINDING_MAINT_001 } from './mockFindings';

/**
 * Analysis Run for LoginDao.java (Vulnerable baseline)
 * Grade capped at 'C' due to HIGH-severity SQL injection finding.
 */
export const MOCK_RUN_LOGIN_DAO: AnalysisRun = {
  runId: 'run-c7a2-001',
  submissionId: 'sub-9b1e-001',
  status: 'COMPLETED',
  engineVersion: '1.0.0-mvp',
  rulesetVersion: '2026.10',
  scoringVersion: 'v1.0-weighted-density',
  overallScore: 68.5,
  grade: 'C', // Capped by SEC-001 finding (Bible Section 4 Gate Rule)
  startedAt: '2026-10-21T14:10:00Z',
  finishedAt: '2026-10-21T14:10:01Z',
  dimensions: [
    {
      dimension: 'SECURITY',
      weightedDensity: 33.3, // (5.0 weight / 15 LOC) * 100
      subScore: 40.0,
      findingCount: 1,
    },
    {
      dimension: 'COMPLEXITY',
      weightedDensity: 0.0,
      subScore: 100.0,
      findingCount: 0,
    },
    {
      dimension: 'DUPLICATION',
      weightedDensity: 0.0,
      subScore: 100.0,
      findingCount: 0,
    },
    {
      dimension: 'MAINTAINABILITY',
      weightedDensity: 6.7, // (1.0 weight / 15 LOC) * 100
      subScore: 85.0,
      findingCount: 1,
    },
  ],
};

/**
 * Analysis Run for OrderProcessingService.java (Complexity hotspot)
 * Grade 'F' due to extreme cognitive and cyclomatic complexity density.
 */
export const MOCK_RUN_ORDER_SERVICE: AnalysisRun = {
  runId: 'run-88ae-002',
  submissionId: 'sub-5a3d-002',
  status: 'COMPLETED',
  engineVersion: '1.0.0-mvp',
  rulesetVersion: '2026.10',
  scoringVersion: 'v1.0-weighted-density',
  overallScore: 58.0,
  grade: 'F',
  startedAt: '2026-10-21T13:20:00Z',
  finishedAt: '2026-10-21T13:20:02Z',
  dimensions: [
    {
      dimension: 'SECURITY',
      weightedDensity: 0.0,
      subScore: 100.0,
      findingCount: 0,
    },
    {
      dimension: 'COMPLEXITY',
      weightedDensity: 35.0,
      subScore: 30.0,
      findingCount: 2,
    },
    {
      dimension: 'DUPLICATION',
      weightedDensity: 12.0,
      subScore: 70.0,
      findingCount: 1,
    },
    {
      dimension: 'MAINTAINABILITY',
      weightedDensity: 18.0,
      subScore: 65.0,
      findingCount: 2,
    },
  ],
};

/**
 * Analysis Run for TokenCache.java (Clean Baseline)
 * Grade 'A' across all 4 deterministic analyzers.
 */
export const MOCK_RUN_TOKEN_CACHE: AnalysisRun = {
  runId: 'run-11ff-003',
  submissionId: 'sub-2c4e-003',
  status: 'COMPLETED',
  engineVersion: '1.0.0-mvp',
  rulesetVersion: '2026.10',
  scoringVersion: 'v1.0-weighted-density',
  overallScore: 96.0,
  grade: 'A',
  startedAt: '2026-10-21T11:45:00Z',
  finishedAt: '2026-10-21T11:45:01Z',
  dimensions: [
    {
      dimension: 'SECURITY',
      weightedDensity: 0.0,
      subScore: 100.0,
      findingCount: 0,
    },
    {
      dimension: 'COMPLEXITY',
      weightedDensity: 0.0,
      subScore: 98.0,
      findingCount: 0,
    },
    {
      dimension: 'DUPLICATION',
      weightedDensity: 0.0,
      subScore: 100.0,
      findingCount: 0,
    },
    {
      dimension: 'MAINTAINABILITY',
      weightedDensity: 2.0,
      subScore: 94.0,
      findingCount: 0,
    },
  ],
};

/**
 * Analysis Run for LoginDao_Remediated.java (AI Remediated v2)
 * Grade 'A': SQL injection finding resolved, 0 regressions introduced.
 */
export const MOCK_RUN_LOGIN_DAO_REMEDIATED: AnalysisRun = {
  runId: 'run-88ae-004',
  submissionId: 'sub-7f2a-004',
  status: 'COMPLETED',
  engineVersion: '1.0.0-mvp',
  rulesetVersion: '2026.10',
  scoringVersion: 'v1.0-weighted-density',
  overallScore: 98.0,
  grade: 'A',
  startedAt: '2026-10-21T14:15:00Z',
  finishedAt: '2026-10-21T14:15:01Z',
  dimensions: [
    {
      dimension: 'SECURITY',
      weightedDensity: 0.0,
      subScore: 100.0,
      findingCount: 0,
    },
    {
      dimension: 'COMPLEXITY',
      weightedDensity: 0.0,
      subScore: 98.0,
      findingCount: 0,
    },
    {
      dimension: 'DUPLICATION',
      weightedDensity: 0.0,
      subScore: 100.0,
      findingCount: 0,
    },
    {
      dimension: 'MAINTAINABILITY',
      weightedDensity: 0.0,
      subScore: 96.0,
      findingCount: 0,
    },
  ],
};

/**
 * Analysis Run with Syntax Error (PARSE_FAILED)
 * Bible Section 4 rule: Grade is 'N/A', not 'F'.
 */
export const MOCK_RUN_PARSE_FAILED: AnalysisRun = {
  runId: 'run-e32a-005',
  submissionId: 'sub-00e1-005',
  status: 'PARSE_FAILED',
  engineVersion: '1.0.0-mvp',
  rulesetVersion: '2026.10',
  scoringVersion: 'v1.0-weighted-density',
  overallScore: null,
  grade: 'N/A',
  startedAt: '2026-10-21T10:00:00Z',
  finishedAt: '2026-10-21T10:00:00Z',
  dimensions: [],
  error: "JavaParser error: Syntax error at line 5: ';' expected",
};

/**
 * Comprehensive Audit Report Fixture for LoginDao.java
 */
export const MOCK_REPORT_LOGIN_DAO: AnalysisReport = {
  run: MOCK_RUN_LOGIN_DAO,
  scoreFormula: '100 − [ 0.35·(Density_Security) + 0.25·(Density_Complexity) + 0.20·(Density_Duplication) + 0.20·(Density_Maintainability) ]',
  findingContributions: {
    [MOCK_FINDING_SEC_001.fingerprint]: -15.0,
    [MOCK_FINDING_MAINT_001.fingerprint]: -3.0,
  },
  findings: [MOCK_FINDING_SEC_001, MOCK_FINDING_MAINT_001],
  summaryText: 'Audit complete: 1 High-severity security vulnerability (CWE-89 SQL Injection) detected. Remediate with PreparedStatement.',
};

export const MOCK_ANALYSIS_RUNS: AnalysisRun[] = [
  MOCK_RUN_LOGIN_DAO,
  MOCK_RUN_ORDER_SERVICE,
  MOCK_RUN_TOKEN_CACHE,
  MOCK_RUN_LOGIN_DAO_REMEDIATED,
  MOCK_RUN_PARSE_FAILED,
];
