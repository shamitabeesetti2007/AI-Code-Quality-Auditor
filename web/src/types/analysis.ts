/**
 * Analysis Run and Quality Scoring Models
 * Grounded in Project Bible: Section 4 (Analysis Engine & Scoring), Section 9 (DDL: analysis_run, run_dimension_score), Section 11 (API).
 */

import type { Finding } from './finding';

/**
 * State machine for an analysis run.
 * Grounded in Bible Section 4, Section 9 DDL CHECK, and Section 12.5 State Diagram:
 * QUEUED -> PARSING -> ANALYZING -> SCORING -> COMPLETED (or PARTIAL, PARSE_FAILED, FAILED)
 */
export type RunStatus =
  | 'QUEUED'
  | 'PARSING'
  | 'ANALYZING'
  | 'SCORING'
  | 'COMPLETED'
  | 'PARTIAL'
  | 'PARSE_FAILED'
  | 'FAILED';

/**
 * Letter grades defined in Project Bible Section 4:
 * A >= 90, B >= 80, C >= 70, D >= 60, F < 60.
 * Gate rule: Any CRITICAL security finding caps the grade at C.
 * Syntax error rule: If parsing fails, the grade is explicitly 'N/A', not 'F'.
 */
export type QualityGrade = 'A' | 'B' | 'C' | 'D' | 'F' | 'N/A';

/**
 * The 4 deterministic analyzer quality dimensions written by the team.
 * Grounded in Project Bible Section 4 & Section 9.
 */
export type DimensionType =
  | 'SECURITY'
  | 'COMPLEXITY'
  | 'DUPLICATION'
  | 'MAINTAINABILITY';

/**
 * Dimension Scoring Metadata & Weights
 * Defined in Project Bible Section 4: Overall Q = sum(w_d * S_d)
 */
export interface DimensionConfig {
  dimension: DimensionType;
  displayName: string;
  weight: number; // e.g. 0.35, 0.25, 0.20, 0.20
  ruleRange: string;
  description: string;
}

export const DIMENSION_CONFIGS: Record<DimensionType, DimensionConfig> = {
  SECURITY: {
    dimension: 'SECURITY',
    displayName: 'Security',
    weight: 0.35,
    ruleRange: 'SEC-001..008',
    description: 'Pattern rules for SQL injection, hardcoded secrets, weak crypto, and deserialization',
  },
  COMPLEXITY: {
    dimension: 'COMPLEXITY',
    displayName: 'Complexity',
    weight: 0.25,
    ruleRange: 'COMP-001..003',
    description: 'Cyclomatic and cognitive complexity metrics across methods and nesting hierarchies',
  },
  DUPLICATION: {
    dimension: 'DUPLICATION',
    displayName: 'Duplication',
    weight: 0.20,
    ruleRange: 'DUP-001..002',
    description: 'Token-based Type-1 (identical) and Type-2 (renamed) clone detection via rolling hash',
  },
  MAINTAINABILITY: {
    dimension: 'MAINTAINABILITY',
    displayName: 'Maintainability',
    weight: 0.20,
    ruleRange: 'MAINT-001..004',
    description: 'Code smells, deep nesting, long methods, and exception handling anti-patterns',
  },
};

/**
 * Score & density per dimension.
 * Direct 1:1 mapping to PostgreSQL table `run_dimension_score` (Bible Section 9).
 */
export interface RunDimensionScore {
  dimension: DimensionType;
  weightedDensity: number; // Findings weighted sum / max(LOC, 10) * 100
  subScore: number; // clamp(100 - k_d * D_d, 0, 100)
  
  // --- FRONTEND-ONLY PROPOSED FIELDS ---
  findingCount?: number; // Total number of findings in this dimension
}

/**
 * Core Analysis Run Entity
 * Direct 1:1 mapping to PostgreSQL table `analysis_run` (Bible Section 9).
 */
export interface AnalysisRun {
  runId: string;
  submissionId: string;
  status: RunStatus;
  engineVersion: string;
  rulesetVersion: string;
  scoringVersion: string;
  overallScore: number | null; // 0 to 100, null if not yet completed or PARSE_FAILED
  grade: QualityGrade | null;
  startedAt: string; // ISO 8601
  finishedAt: string | null;
  dimensions: RunDimensionScore[];
  error?: string | null;
}

/**
 * Full Audit Report DTO
 * Grounded in GET /api/v1/analysis-runs/{runId}/report (Bible Section 11).
 */
export interface AnalysisReport {
  run: AnalysisRun;
  scoreFormula: string; // Transparent mathematical string for UI display
  findingContributions: Record<string, number>; // Finding fingerprint -> score point deduction
  findings: Finding[];
  
  // --- FRONTEND-ONLY PROPOSED FIELDS ---
  summaryText?: string; // High-level executive audit summary
}
