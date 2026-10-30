/**
 * Finding Data Models
 * Grounded in Project Bible: Section 4 (Finding Identity & Rules), Section 9 (DDL: table finding, table rule), Section 11 (API).
 */

import type { DimensionType } from './analysis';

/**
 * Finding Severity Levels
 * Direct 1:1 mapping to PostgreSQL table `finding` CHECK constraint (Bible Section 9).
 */
export type FindingSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

/**
 * Severity weights used in transparent density calculation (Bible Section 4).
 * CRITICAL = 10, HIGH = 5, MEDIUM = 2, LOW = 1, INFO = 0
 */
export const SEVERITY_WEIGHTS: Record<FindingSeverity, number> = {
  CRITICAL: 10,
  HIGH: 5,
  MEDIUM: 2,
  LOW: 1,
  INFO: 0,
};

/**
 * Design-time rule confidence rating (Bible Section 4 Analyzer 3).
 * Represents analyzer certainty and potential for JDBC / naming false-positives.
 */
export type RuleConfidence = 'HIGH' | 'MEDIUM' | 'LOW';

/**
 * Core Deterministic Finding Entity
 * Direct 1:1 mapping to PostgreSQL table `finding` joined with `rule` (Bible Section 9 & 11).
 */
export interface Finding {
  findingId: string;
  runId: string;
  ruleId: string;
  ruleCode: string; // e.g. 'SEC-001', 'COMP-001', 'MAINT-001', 'SYN-001'
  dimension: DimensionType;
  cweId: number | null; // e.g. 89 for SQLi, 798 for hardcoded secrets, null for smells
  severity: FindingSeverity;
  startLine: number; // 1-based line index
  endLine: number; // 1-based line index, endLine >= startLine
  message: string;
  snippet: string | null; // Exact code token slice flagged by parser
  fingerprint: string; // SHA-256 hash (64 chars) of rule + method signature + tokens (Bible Section 4)
  confidence: RuleConfidence;

  // --- FRONTEND-ONLY PROPOSED FIELDS (for editor gutter & visual callouts) ---
  startColumn?: number; // 1-based column for inline editor underline
  endColumn?: number;
  scoreDeduction?: number; // Pre-calculated point impact on overall score
}
