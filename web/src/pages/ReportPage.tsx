import React, { useState, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  GitCompare,
  ArrowRight,
  Info,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import {
  GradeBadge,
  SeverityBadge,
  ProvenanceBadge,
  OutcomeBadge,
} from '../components/common/Badge';
import { MOCK_ANALYSIS_RUNS } from '../fixtures/mockAnalysisRuns';
import { MOCK_SUBMISSIONS } from '../fixtures/mockSubmissions';
import { MOCK_FINDINGS } from '../fixtures/mockFindings';
import { MOCK_REMEDIATION_SEC_001 } from '../fixtures/mockRemediations';
import { DIMENSION_CONFIGS } from '../types/analysis';

export const ReportPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const currentRunId = searchParams.get('runId') || 'run-c7a2-001';

  // Toggle for showing the AI remediation patch drawer
  const [showRemediation, setShowRemediation] = useState(false);

  // Active run and matching submission
  const run = useMemo(() => {
    return (
      MOCK_ANALYSIS_RUNS.find((r) => r.runId === currentRunId) ||
      MOCK_ANALYSIS_RUNS[0]
    );
  }, [currentRunId]);

  const submission = useMemo(() => {
    return (
      MOCK_SUBMISSIONS.find((s) => s.submissionId === run.submissionId) ||
      MOCK_SUBMISSIONS[0]
    );
  }, [run]);

  // Findings belonging to this run
  const runFindings = useMemo(() => {
    return MOCK_FINDINGS.filter((f) => f.runId === run.runId);
  }, [run.runId]);

  const handleRunSelect = (runId: string) => {
    setSearchParams({ runId });
    setShowRemediation(false);
  };

  const isParseFailed = run.status === 'PARSE_FAILED';

  return (
    <div className="space-y-8 max-w-5xl mx-auto pb-16">
      {/* Top Header & Simulation Notice */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <Link to="/">
          <Button variant="ghost" size="sm" icon={<ArrowLeft className="h-4 w-4" />}>
            Back to Dashboard
          </Button>
        </Link>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono font-medium text-amber-400 bg-amber-500/10 border border-amber-400/20">
          <Info className="h-3.5 w-3.5" />
          <span>SIMULATED AUDIT REPORT • PRE-COMPUTED FIXTURE</span>
        </div>
      </div>

      {/* Fixture Run Switcher Tabs */}
      <div className="rounded-2xl glass-surface p-4 space-y-2">
        <div className="text-[11px] font-mono uppercase tracking-wider text-slate-400">
          Switch Simulated Run Fixture:
        </div>
        <div className="flex flex-wrap gap-2">
          {MOCK_ANALYSIS_RUNS.map((r) => {
            const sub = MOCK_SUBMISSIONS.find((s) => s.submissionId === r.submissionId);
            const isSelected = r.runId === run.runId;
            return (
              <button
                key={r.runId}
                type="button"
                onClick={() => handleRunSelect(r.runId)}
                className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all border flex items-center gap-2 ${
                  isSelected
                    ? 'bg-sky-500/20 text-sky-300 border-sky-400/40 shadow-[0_0_15px_-3px_rgba(56,189,248,0.3)]'
                    : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05] hover:text-white'
                }`}
              >
                <span>{sub?.title || r.runId}</span>
                <span
                  className={`text-[10px] font-bold px-1.5 py-0.2 rounded ${
                    r.grade === 'A'
                      ? 'bg-emerald-500/20 text-emerald-300'
                      : r.grade === 'C'
                      ? 'bg-amber-500/20 text-amber-300'
                      : r.grade === 'F'
                      ? 'bg-rose-500/20 text-rose-300'
                      : 'bg-slate-500/20 text-slate-300'
                  }`}
                >
                  Grade {r.grade || 'N/A'}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Main Report Banner */}
      <div className="rounded-2xl glass-surface p-6 sm:p-8 space-y-6">
        {/* Title, Provenance & Meta */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.06] pb-6">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[var(--text-main)]">
                {submission.title}
              </h1>
              <ProvenanceBadge type={submission.originType} />
            </div>
            <p className="text-xs text-[var(--text-muted)] font-mono">
              Run ID: {run.runId} • Scoring Engine: {run.scoringVersion} • Logical LOC:{' '}
              {submission.logicalLoc}
            </p>
          </div>

          <div className="flex items-center gap-3">
            <span
              className={`px-3 py-1 rounded-full text-xs font-mono font-bold border ${
                run.status === 'COMPLETED'
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                  : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
              }`}
            >
              STATUS: {run.status}
            </span>
          </div>
        </div>

        {/* Score & Grade Display */}
        {isParseFailed ? (
          <div className="p-6 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 space-y-2">
            <div className="flex items-center gap-2 font-mono font-bold text-sm">
              <AlertTriangle className="h-5 w-5 text-rose-400" />
              <span>JavaParser Syntax Failure: Run Ended with Grade N/A</span>
            </div>
            <p className="text-xs text-slate-300 leading-relaxed">
              Per Project Bible Section 4.3, syntax parse failures produce rule{' '}
              <code className="font-mono text-rose-300 font-bold">SYN-001</code> and yield grade{' '}
              <strong>N/A, not F</strong>. Unparseable submissions represent a distinct research outcome
              that is counted separately from functional code quality.
            </p>
            <div className="p-3 rounded-lg bg-black/40 font-code text-xs text-rose-200 mt-2">
              {run.error}
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
            {/* Grade Hero */}
            <div className="md:col-span-4 flex items-center gap-6 p-4 rounded-xl bg-white/[0.02] border border-white/[0.06]">
              <GradeBadge grade={run.grade || 'N/A'} size="lg" />
              <div className="space-y-1">
                <span className="text-[11px] font-mono uppercase tracking-wider text-slate-500 block">
                  Overall Score
                </span>
                <div className="flex items-baseline gap-2">
                  <span className="font-mono text-3xl font-black text-[var(--text-main)]">
                    {run.overallScore?.toFixed(1)}
                  </span>
                  <span className="text-xs font-mono text-slate-400">/ 100</span>
                </div>
                <span className="text-[11px] font-mono text-slate-400">
                  {runFindings.length} active findings
                </span>
              </div>
            </div>

            {/* Score Formula & Gate Notice */}
            <div className="md:col-span-8 space-y-2">
              <div className="p-4 rounded-xl bg-black/30 border border-white/[0.06] space-y-1">
                <span className="text-[10px] font-mono uppercase tracking-wider text-sky-400 font-bold block">
                  Transparent Deduction Formula (Bible Section 4)
                </span>
                <p className="font-code text-xs text-slate-300">
                  Q = 0.35·S_sec + 0.25·S_comp + 0.20·S_dup + 0.20·S_maint
                </p>
                <p className="text-[11px] text-slate-500 font-mono">
                  Sub-score S_d = clamp(100 − k_d · Density_d, 0, 100)
                </p>
              </div>

              {/* Gate Rule Callout for LoginDao */}
              {run.runId === 'run-c7a2-001' && (
                <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs">
                  <AlertTriangle className="h-4 w-4 shrink-0 text-amber-400" />
                  <span>
                    <strong>Gate Rule Enforced:</strong> CRITICAL/HIGH finding{' '}
                    <code className="font-mono font-bold">SEC-001</code> caps grade at{' '}
                    <strong>C</strong>.
                  </span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* 4 Dimension Score Cards */}
        {!isParseFailed && (
          <div className="space-y-3 pt-2">
            <h3 className="font-heading text-sm font-bold text-[var(--text-main)]">
              Dimension Sub-Scores &amp; Weighted Densities
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {run.dimensions.map((dim) => {
                const config = DIMENSION_CONFIGS[dim.dimension];
                return (
                  <div
                    key={dim.dimension}
                    className="p-4 rounded-xl bg-white/[0.02] border border-white/[0.06] space-y-2 hover:border-sky-400/30 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-[var(--text-main)]">
                        {config.displayName}
                      </span>
                      <span className="text-[10px] font-mono text-sky-400 font-semibold">
                        {(config.weight * 100).toFixed(0)}% wt
                      </span>
                    </div>

                    <div className="flex items-baseline justify-between">
                      <span className="font-mono text-2xl font-black text-slate-200">
                        {dim.subScore.toFixed(0)}
                      </span>
                      <span className="text-[10px] font-mono text-slate-400">
                        Density: {dim.weightedDensity.toFixed(1)}
                      </span>
                    </div>

                    <div className="w-full bg-white/[0.05] rounded-full h-1.5 overflow-hidden">
                      <div
                        className={`h-full rounded-full ${
                          dim.subScore >= 90
                            ? 'bg-emerald-400'
                            : dim.subScore >= 70
                            ? 'bg-sky-400'
                            : dim.subScore >= 50
                            ? 'bg-amber-400'
                            : 'bg-rose-400'
                        }`}
                        style={{ width: `${Math.min(dim.subScore, 100)}%` }}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* Findings Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-heading text-lg font-bold text-[var(--text-main)]">
              Deterministic Findings ({runFindings.length})
            </h2>
            <p className="text-xs text-[var(--text-muted)]">
              AST analyzer detections with location, CWE linkage, and score penalty
            </p>
          </div>

          <Link
            to="/findings"
            className="text-xs font-mono text-sky-400 hover:underline flex items-center gap-1"
          >
            <span>Open Findings Explorer</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        {runFindings.length === 0 ? (
          <div className="rounded-2xl glass-surface p-8 text-center space-y-4">
            <div className="space-y-2">
              <CheckCircle2 className="h-8 w-8 text-emerald-400 mx-auto" />
              <h3 className="font-heading font-bold text-sm text-[var(--text-main)]">
                Zero Findings Detected
              </h3>
              <p className="text-xs text-[var(--text-muted)]">
                This code meets all deterministic standards across Security, Complexity, Duplication, and Maintainability.
              </p>
            </div>
            {submission.parentSubmissionId && (
              <div className="pt-2">
                <Link to="/compare">
                  <Button variant="primary" size="sm" icon={<GitCompare className="h-4 w-4" />}>
                    Compare with Original Version (+29.5 pts delta)
                  </Button>
                </Link>
              </div>
            )}
          </div>
        ) : (
          <div className="space-y-3">
            {runFindings.map((finding) => (
              <div
                key={finding.findingId}
                className="rounded-2xl glass-surface p-5 space-y-3 border border-white/[0.06] hover:border-sky-400/30 transition-all"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <SeverityBadge severity={finding.severity} />
                    <span className="font-mono text-xs font-bold text-sky-400">
                      [{finding.ruleCode}]
                    </span>
                    {finding.cweId && (
                      <span className="text-[11px] font-mono text-slate-400 bg-white/[0.04] px-2 py-0.5 rounded border border-white/[0.06]">
                        CWE-{finding.cweId}
                      </span>
                    )}
                    <span className="text-xs text-slate-400 font-mono">
                      Line {finding.startLine}
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    <span
                      className={`text-xs font-mono font-semibold ${
                        (finding.scoreDeduction ?? 0) > 0 ? 'text-rose-400' : 'text-slate-400'
                      }`}
                    >
                      {(finding.scoreDeduction ?? 0) > 0
                        ? `-${finding.scoreDeduction} pts`
                        : '0 pts deduction'}
                    </span>
                  </div>
                </div>

                <p className="text-xs text-[var(--text-main)] leading-relaxed">
                  {finding.message}
                </p>

                {finding.snippet && (
                  <div className="p-3 rounded-xl bg-black/40 border border-white/[0.04] font-code text-xs text-slate-300 overflow-x-auto">
                    {finding.snippet}
                  </div>
                )}

                {/* Finding Action Links */}
                <div className="flex flex-wrap items-center gap-3 pt-1 border-t border-white/[0.04] text-xs">
                  <Link
                    to={`/findings?findingId=${finding.findingId}`}
                    className="text-sky-400 hover:underline font-mono inline-flex items-center gap-1"
                  >
                    <span>View Grounded RAG Guidance</span>
                    <ArrowRight className="h-3 w-3" />
                  </Link>

                  {/* If finding has pre-computed verified remediation, offer drawer toggle */}
                  {finding.ruleCode === 'SEC-001' && (
                    <button
                      type="button"
                      onClick={() => setShowRemediation(!showRemediation)}
                      className="ml-auto inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-mono font-medium text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 hover:bg-emerald-500/20 transition-all"
                    >
                      <ShieldCheck className="h-3.5 w-3.5" />
                      <span>{showRemediation ? 'Hide Verified Patch' : 'View Verified AI Patch'}</span>
                      {showRemediation ? (
                        <ChevronUp className="h-3.5 w-3.5" />
                      ) : (
                        <ChevronDown className="h-3.5 w-3.5" />
                      )}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Verified AI Remediation Drawer (Bible Section 5.C) */}
      {showRemediation && (
        <div className="rounded-2xl glass-surface-elevated p-6 sm:p-8 space-y-6 border border-emerald-500/30 shadow-[0_0_30px_-5px_rgba(16,185,129,0.2)]">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-white/[0.06] pb-4">
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <ShieldCheck className="h-5 w-5 text-emerald-400" />
                <h3 className="font-heading font-bold text-base text-[var(--text-main)]">
                  Closed-Loop AI Remediation: VERIFIED_FIX
                </h3>
              </div>
              <p className="text-xs text-[var(--text-muted)]">
                Strategy: <strong>GUIDED</strong> (Augmented with CWE-89 key-lookup passage)
              </p>
            </div>

            <OutcomeBadge outcome={MOCK_REMEDIATION_SEC_001.outcome} />
          </div>

          <p className="text-xs text-slate-300 leading-relaxed">
            {MOCK_REMEDIATION_SEC_001.explanationSummary}
          </p>

          {/* 7-Step Verification Checklist */}
          <div className="space-y-2">
            <span className="text-[10px] font-mono uppercase tracking-wider text-slate-400 font-bold block">
              Automated 7-Step Verification Checklist:
            </span>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
              {MOCK_REMEDIATION_SEC_001.verification.steps.map((step) => (
                <div
                  key={step.name}
                  className="p-2.5 rounded-lg bg-black/30 border border-white/[0.06] flex items-center justify-between text-xs font-mono"
                >
                  <span className="text-slate-300">{step.name}</span>
                  <span className="text-emerald-400 font-bold flex items-center gap-1">
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    PASSED
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Unified Diff Viewer */}
          <div className="space-y-1.5">
            <span className="text-[10px] font-mono uppercase tracking-wider text-slate-400 font-bold block">
              Unified Code Diff:
            </span>
            <pre className="p-4 rounded-xl bg-[#06080C] border border-white/[0.06] font-code text-xs text-slate-300 overflow-x-auto leading-5">
              {MOCK_REMEDIATION_SEC_001.diff}
            </pre>
          </div>

          {/* Compare Before vs After Action */}
          <div className="flex justify-end pt-2">
            <Link to="/compare">
              <Button variant="primary" icon={<GitCompare className="h-4 w-4" />}>
                Inspect Full Version Comparison (+29.5 pts)
              </Button>
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};
