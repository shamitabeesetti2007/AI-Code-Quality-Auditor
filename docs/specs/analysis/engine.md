# Deterministic Analysis Engine

## Entry Point and Models

The public entry point is `AnalyzerRegistry.analyze(AnalysisInput)`. Create a
registry with `new AnalyzerRegistry(new JavaSourceParser(),
List.of(new ComplexityAnalyzer()))`; the core has no Spring dependency.
`AnalysisInput` contains `sourceName` and
submitted Java `source`; the source is parsed as Java 21 and is never executed.
The immutable `AnalysisResult` contains `status`, syntax
`diagnostics`, `metrics`, and `findings`. Locations include the source name and
1-based start/end line and column; zero means JavaParser did not provide a range.
Metrics carry an analyzer id, name, numeric value, and location. Findings carry a
stable rule id, message, measured value, threshold, and location.

`PARSE_FAILED` is an engine-local `AnalysisStatus`. The orchestrator maps it by
name to the canonical `RunStatus.PARSE_FAILED`; the engine does not own or define
the persistence/run-status enum. On parse failure diagnostics are returned and no
analyzer is invoked, so there are no metrics, findings, grades, or scores.

## Analyzer Registration

An orchestrator invokes the registry directly or exposes it through a framework
adapter outside the engine package. Additional analyzers implement `Analyzer`,
provide a unique `analyzerId()`, and return an `AnalyzerResult` from
`analyze(AnalysisContext)`. Pass the analyzer instances in the registry's
constructor; the registry invokes them in analyzer-id order for stable ordering.

## Complexity Conventions

Every method and constructor starts at cyclomatic complexity 1. Each `if`,
`for`, enhanced `for`, `while`, `do-while`, `catch`, ternary, and `&&`/`||`
operator adds 1. Each non-default switch label adds 1; `default` adds 0. Class
aggregates are the sum of direct methods and constructors; nested classes have
their own aggregates. Abstract methods have the baseline of 1.

Cognitive complexity adds 1 for each control structure (if, loop, switch,
catch, or ternary), plus its current nesting depth. A plain `else` adds 1;
`else if` is counted as another if without a separate else increment. Each
contiguous logical-operator sequence adds 1, and changing between `&&` and `||`
adds 1. Direct calls whose name matches the current method add 1 each to
cognitive complexity; this name-based convention does not resolve overloaded
methods. Recursion does not affect cyclomatic complexity. Lambda bodies are
excluded from the enclosing callable's measurement.

Default findings are emitted only when a method or constructor exceeds
cyclomatic complexity 10 or cognitive complexity 15. Thresholds can be changed
by passing a `ComplexityThresholds` value to `ComplexityAnalyzer`. Rule ids are
`JAVA-CYCLOMATIC-COMPLEXITY` and `JAVA-COGNITIVE-COMPLEXITY`.