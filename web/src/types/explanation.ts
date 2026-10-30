/**
 * AI Explanation and Grounded Guidance Models
 * Grounded in Project Bible: Section 5 (Component B: Retrieval-Augmented Explanation), Section 9 (kb_chunk, explanation), Section 11 (API).
 */

export type ExplanationStatus = 'PENDING' | 'VALIDATED' | 'REJECTED';

export interface GuidanceSource {
  chunkId: string;
  title: string;
  url: string; // CWE or OWASP reference documentation URL
}

/**
 * Finding Explanation DTO
 * Grounded in GET /api/v1/findings/{id}/explanations (Bible Section 11).
 */
export interface FindingExplanation {
  explanationId: string;
  findingId: string;
  status: ExplanationStatus;
  summary: string;
  whyItMatters: string;
  howToFix: string;
  sources: GuidanceSource[];
  fallbackUsed: boolean; // TRUE when external validation failed and deterministic rule text is shown
}
