/**
 * Code Analysis Utilities (Client-Side Simulation & Validation)
 * Grounded in Project Bible: Section 4 (Language Gate & Logical LOC).
 * 
 * Invariant: Never executes submitted code. Only inspects string structure.
 */

export interface LanguageGateResult {
  isJava: boolean;
  detectedTypes: string[];
  message: string;
}

/**
 * Client-Side Language Gate Check
 * Bible Section 4.2: "reject input that is clearly not Java source (empty, binary, no type declaration)
 * with HTTP 422; Java that merely has syntax errors is accepted and handled in the next step."
 */
export function checkLanguageGate(code: string): LanguageGateResult {
  const trimmed = code.trim();
  if (!trimmed) {
    return {
      isJava: false,
      detectedTypes: [],
      message: 'Input is empty. Please enter or paste Java source code.',
    };
  }

  // Regex patterns for Java top-level declarations (class, interface, enum, record, @interface)
  const typeRegex = /\b(class|interface|enum|record|@interface)\s+([A-Za-z_$][A-Za-z0-9_$]*)/g;
  const matches: string[] = [];
  let match: RegExpExecArray | null;

  while ((match = typeRegex.exec(trimmed)) !== null) {
    matches.push(`${match[1]} ${match[2]}`);
  }

  if (matches.length > 0) {
    return {
      isJava: true,
      detectedTypes: matches,
      message: `Valid Java type declaration detected: ${matches.join(', ')}`,
    };
  }

  // Check for common non-Java keywords to provide helpful feedback
  if (/^\s*def\s+[a-zA-Z_]/m.test(trimmed) || /import\s+sys/m.test(trimmed)) {
    return {
      isJava: false,
      detectedTypes: [],
      message: 'Client-Side Language Gate: Python syntax detected. Input must be Java source code containing a valid type declaration.',
    };
  }

  if (/fn\s+[a-zA-Z_]/m.test(trimmed) || /let\s+mut\s+/m.test(trimmed)) {
    return {
      isJava: false,
      detectedTypes: [],
      message: 'Client-Side Language Gate: Rust syntax detected. Input must be Java source code containing a valid type declaration.',
    };
  }

  return {
    isJava: false,
    detectedTypes: [],
    message: 'Client-Side Language Gate: No top-level Java declaration (class, interface, enum, record) found. Submission is disabled.',
  };
}

/**
 * Calculate Logical Lines of Code (LLOC)
 * Bible Section 4.1: "logical LOC (non-blank, non-comment lines)"
 */
export function calculateLogicalLoc(code: string): number {
  if (!code.trim()) return 0;

  const lines = code.split('\n');
  let count = 0;
  let inBlockComment = false;

  for (const rawLine of lines) {
    const line = rawLine.trim();

    // Inside multi-line block comment
    if (inBlockComment) {
      if (line.includes('*/')) {
        inBlockComment = false;
      }
      continue;
    }

    // Start of block comment
    if (line.startsWith('/*')) {
      if (!line.includes('*/')) {
        inBlockComment = true;
      }
      continue;
    }

    // Blank line or single-line comment
    if (!line || line.startsWith('//') || line.startsWith('*')) {
      continue;
    }

    count++;
  }

  return count;
}
