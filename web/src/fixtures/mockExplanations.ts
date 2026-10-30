/**
 * Mock Explanation Fixtures
 * Grounded in Project Bible: Section 5 (Component B: Retrieval-Augmented Explanation via Key Lookup) and Section 11 (API).
 */

import type { FindingExplanation } from '../types/explanation';
import { MOCK_FINDING_SEC_001 } from './mockFindings';

/**
 * Validated Retrieval-Augmented Explanation for SEC-001 (SQL Injection)
 * Grounded in CWE-89 and OWASP Top 10 A03 citations via key-lookup.
 */
export const MOCK_EXPLANATION_SEC_001: FindingExplanation = {
  explanationId: 'exp-5f3d-001',
  findingId: MOCK_FINDING_SEC_001.findingId,
  status: 'VALIDATED',
  summary:
    'The authenticate method constructs an SQL query by concatenating untrusted method parameters into java.sql.Statement.executeQuery().',
  whyItMatters:
    "SQL injection occurs when user input is treated as executable SQL syntax. An attacker can submit input such as ' OR '1'='1 to alter query logic, bypass authentication checks, and read or modify database records without authorization.",
  howToFix:
    "Replace java.sql.Statement with java.sql.PreparedStatement and substitute parameters with '?' placeholders. PreparedStatement pre-compiles the SQL command so user input is treated strictly as literal data values.",
  sources: [
    {
      chunkId: 'kb-cwe-89',
      title: 'CWE-89: Improper Neutralization of Special Elements used in an SQL Command',
      url: 'https://cwe.mitre.org/data/definitions/89.html',
    },
    {
      chunkId: 'kb-owasp-a03',
      title: 'OWASP Top 10:2021 — A03 Injection',
      url: 'https://owasp.org/Top10/A03_2021-Injection/',
    },
  ],
  fallbackUsed: false,
};

/**
 * Fallback Explanation Example
 * Demonstrates Bible Section 5.B behavior: When the LLM explanation fails external validation,
 * status is REJECTED, fallbackUsed is set to true, and curated rule text is displayed.
 */
export const MOCK_EXPLANATION_FALLBACK: FindingExplanation = {
  explanationId: 'exp-9a1c-002',
  findingId: 'fnd-45b2-002',
  status: 'REJECTED',
  summary: 'Rule COMP-001: Method cyclomatic complexity exceeds configured threshold (10).',
  whyItMatters:
    'High cyclomatic complexity indicates deeply branching logic with numerous independent execution paths, increasing error rates and testing burden.',
  howToFix:
    'Decompose the complex method into smaller, single-responsibility helper methods and reduce nested decision branches.',
  sources: [
    {
      chunkId: 'kb-clean-code-01',
      title: 'Clean Code: Functions Should Do One Thing',
      url: 'https://learning.oreilly.com/library/view/clean-code-a/9780136083238/',
    },
  ],
  fallbackUsed: true, // Deterministic fallback activated due to validator rejection
};

export const MOCK_EXPLANATION_SEC_003: FindingExplanation = {
  explanationId: 'exp-7b2a-003',
  findingId: 'fnd-99d4-004',
  status: 'VALIDATED',
  summary:
    'MessageDigest.getInstance("MD5") initializes a cryptographically broken and deprecated hash algorithm.',
  whyItMatters:
    'MD5 is vulnerable to collision attacks (CWE-327). An attacker can generate two distinct input payloads that produce the same checksum, enabling signature forgery and credential tampering.',
  howToFix:
    'Replace MessageDigest.getInstance("MD5") with a secure standard algorithm such as SHA-256 or SHA-512 (e.g. MessageDigest.getInstance("SHA-256")).',
  sources: [
    {
      chunkId: 'kb-cwe-327',
      title: 'CWE-327: Use of a Broken or Risky Cryptographic Algorithm',
      url: 'https://cwe.mitre.org/data/definitions/327.html',
    },
  ],
  fallbackUsed: false,
};

export const MOCK_EXPLANATION_MAINT_001: FindingExplanation = {
  explanationId: 'exp-3c1d-004',
  findingId: 'fnd-12c3-003',
  status: 'VALIDATED',
  summary:
    'An empty catch block silently swallows java.sql.SQLException without logging, rethrowing, or recovery logic.',
  whyItMatters:
    'Swallowing exceptions hides database connection and query failures. When errors occur at runtime, the failure is invisible, leading to data corruption and difficult debugging.',
  howToFix:
    'Log the caught exception using a logger (e.g. log.error("Database query failed", e)) or wrap and rethrow it as a domain-specific RuntimeException.',
  sources: [
    {
      chunkId: 'kb-clean-code-02',
      title: 'Clean Code: Error Handling — Don\'t Ignore or Swallow Exceptions',
      url: 'https://learning.oreilly.com/library/view/clean-code-a/9780136083238/',
    },
  ],
  fallbackUsed: false,
};

export const MOCK_EXPLANATIONS: FindingExplanation[] = [
  MOCK_EXPLANATION_SEC_001,
  MOCK_EXPLANATION_FALLBACK,
  MOCK_EXPLANATION_SEC_003,
  MOCK_EXPLANATION_MAINT_001,
];
