/**
 * AI Remediation and Closed-Loop Verification Models
 * Grounded in Project Bible: Section 2 (The ONE Core Differentiator), Section 5 (Component C), Section 9 (DDL: table remediation), Section 11 (API).
 */

/**
 * Remediation prompt condition / strategy.
 * Direct 1:1 mapping to PostgreSQL table `remediation` (Bible Section 9 & 11).
 * BARE: bare prompt without guidelines
 * GUIDED: prompt augmented with rule-specific CWE/OWASP guidance (experimental variable)
 * RULE_BASED: deterministic AST transformation (used as benchmark control condition)
 */
export type RemediationStrategy = 'BARE' | 'GUIDED' | 'RULE_BASED';

/**
 * The 5 deterministic outcome categories of AI fix verification.
 * Direct 1:1 mapping to PostgreSQL table `remediation` CHECK constraint (Bible Section 9)
 * and Section 5 Component C / Section 11 API response.
 */
export type RemediationOutcome =
  | 'VERIFIED_FIX' // Targeted finding resolved, 0 new findings >= MEDIUM, public signatures intact
  | 'PARTIAL'      // Targeted finding resolved, but low/info smells introduced
  | 'NO_EFFECT'    // Targeted finding remains unresolved after patch
  | 'REGRESSION'   // Introduced new findings of MEDIUM, HIGH, or CRITICAL severity
  | 'BROKEN';      // Patched code does not parse, signatures changed, or JUnit tests failed

/**
 * Verification Step Names
 * Grounded in Project Bible Section 5.7 and Section 11 API examples.
 */
export type VerificationStepName =
  | 'SCHEMA_VALID'
  | 'PARSES'
  | 'SIGNATURES_PRESERVED'
  | 'FOOTPRINT_WITHIN_LIMIT'
  | 'TARGET_RESOLVED'
  | 'NO_NEW_FINDINGS'
  | 'TESTS_PASS';

export interface VerificationStep {
  name: VerificationStepName;
  passed: boolean;
  detail?: string;
}

export interface VerificationReport {
  steps: VerificationStep[];
}

/**
 * Core Remediation Entity
 * Direct 1:1 mapping to PostgreSQL table `remediation` (Bible Section 9 & 11).
 */
export interface Remediation {
  remediationId: string;
  findingId: string;
  originalSubmissionId: string;
  patchedSubmissionId: string | null; // Null if patch was completely broken
  patchedRunId: string | null;
  generationId: string;
  strategy: RemediationStrategy;
  outcome: RemediationOutcome;
  targetResolved: boolean;
  introducedCount: number;
  testsPassed: boolean | null; // For curated benchmark tasks in sandbox (Bible Section 3)
  editFootprint: number | null; // Fraction of method lines changed, e.g. 0.06 (6%)
  verification: VerificationReport;
  
  // --- FRONTEND-ONLY PROPOSED FIELDS ---
  diff?: string; // Unified diff text for split code comparison display
  patchedCode?: string; // Full patched source snippet
  explanationSummary?: string; // Short LLM fix rationale
}

/**
 * Request payload for POST /api/v1/findings/{id}/remediations
 */
export interface RequestRemediationRequest {
  modelKey: string; // e.g. 'gemini-1.5-flash', 'ollama-codellama'
  strategy: RemediationStrategy;
}
