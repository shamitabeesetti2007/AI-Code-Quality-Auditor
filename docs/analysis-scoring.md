# Analysis scoring policy

The `complexity-v1` policy scores only results from a fully parsed and completed
analysis. It requires at least one individual `cyclomatic.complexity` metric
and one individual `cognitive.complexity` metric from the `java-complexity`
analyzer. Aggregate metrics are not scored because their component metrics
already contribute. Missing either metric family, an unsupported unclassified
finding, or a non-completed analysis makes the score unavailable; score and
grade remain null.

The score starts at 100 and subtracts:

- Cyclomatic complexity: 2 points per point above 10, capped at 20 per
  callable.
- Cognitive complexity: 1 point per point above 15, capped at 15 per callable.
- Findings with a supplied severity: INFO 0, LOW 3, MEDIUM 8, HIGH 15, and
  CRITICAL 25 points per distinct fingerprint.

Known complexity findings are not charged in addition to the corresponding
metric at the same source location. If no matching metric is present, the
finding's measured value and threshold are used. Other findings without a
severity or explicitly supported impact rule make scoring unavailable.

The result is rounded to two decimal places and clamped to 0–100. Grades are
A (90–100), B (80–89.99), C (70–79.99), D (60–69.99), and F (below 60).
Thresholds, deductions, caps, and policy version are configurable with the
`auditor.analysis.scoring` properties in `application.properties`.

The current engine supplies cyclomatic and cognitive complexity only. It has
no maintainability, security, or reliability analyzer, and its findings do not
provide severity. This policy does not synthesize those unsupported measures
or infer severity; reported scores are therefore limited to the engine data
that exists.
