/**
 * Run Comparison and Delta Models
 * Grounded in Project Bible: Section 4 (Finding Identity), Section 9 (fn_compare_runs), Section 11 (API).
 */

import type { DimensionType } from './analysis';
import type { Finding } from './finding';

export type FindingChangeKind = 'RESOLVED' | 'INTRODUCED' | 'PERSISTING';

export interface DimensionDelta {
  dimension: DimensionType;
  densityDelta: number;
  scoreDelta: number;
}

/**
 * Run Comparison DTO
 * Grounded in GET /api/v1/comparisons/runs (Bible Section 11).
 */
export interface RunComparison {
  baseRunId: string;
  targetRunId: string;
  resolved: Finding[];
  introduced: Finding[];
  persisting: Finding[];
  scoreDelta: number;
  dimensionDeltas: DimensionDelta[];
}
