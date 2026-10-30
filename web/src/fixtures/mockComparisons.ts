/**
 * Mock Comparison Fixtures
 * Grounded in Project Bible: Section 4 (Finding Identity & Matching), Section 9 (SQL fn_compare_runs), Section 11 (API).
 */

import type { RunComparison } from '../types/comparison';
import { MOCK_RUN_LOGIN_DAO, MOCK_RUN_LOGIN_DAO_REMEDIATED } from './mockAnalysisRuns';
import { MOCK_FINDING_SEC_001, MOCK_FINDING_MAINT_001 } from './mockFindings';

/**
 * Version Comparison between LoginDao baseline and its verified remediation
 * Shows:
 * - Resolved: SEC-001 (SQL injection eliminated)
 * - Introduced: 0
 * - Persisting: MAINT-001 (Minor empty catch smell)
 * - Score Delta: +29.5 points (68.5 'C' -> 98.0 'A')
 */
export const MOCK_COMPARISON_LOGIN_DAO: RunComparison = {
  baseRunId: MOCK_RUN_LOGIN_DAO.runId,
  targetRunId: MOCK_RUN_LOGIN_DAO_REMEDIATED.runId,
  resolved: [MOCK_FINDING_SEC_001],
  introduced: [],
  persisting: [MOCK_FINDING_MAINT_001],
  scoreDelta: 29.5,
  dimensionDeltas: [
    {
      dimension: 'SECURITY',
      densityDelta: -33.3, // Security findings eliminated
      scoreDelta: 60.0,
    },
    {
      dimension: 'COMPLEXITY',
      densityDelta: 0.0,
      scoreDelta: 0.0,
    },
    {
      dimension: 'DUPLICATION',
      densityDelta: 0.0,
      scoreDelta: 0.0,
    },
    {
      dimension: 'MAINTAINABILITY',
      densityDelta: -6.7,
      scoreDelta: 11.0,
    },
  ],
};

export const MOCK_COMPARISONS: RunComparison[] = [MOCK_COMPARISON_LOGIN_DAO];
