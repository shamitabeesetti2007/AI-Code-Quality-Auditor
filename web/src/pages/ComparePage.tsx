import React from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  GitCompare,
  TrendingUp,
  CheckCircle2,
  AlertCircle,
  XCircle,
  Info,
  ShieldCheck,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import {
  GradeBadge,
  SeverityBadge,
  ProvenanceBadge,
} from '../components/common/Badge';
import { MOCK_COMPARISON_LOGIN_DAO } from '../fixtures/mockComparisons';
import { MOCK_RUN_LOGIN_DAO, MOCK_RUN_LOGIN_DAO_REMEDIATED } from '../fixtures/mockAnalysisRuns';
import { MOCK_REMEDIATION_SEC_001 } from '../fixtures/mockRemediations';

export const ComparePage: React.FC = () => {
  const comparison = MOCK_COMPARISON_LOGIN_DAO;

  return (
    <div className="space-y-8 max-w-5xl mx-auto pb-16">
      {/* Top Header & Simulation Notice */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <Link to="/history">
          <Button variant="ghost" size="sm" icon={<ArrowLeft className="h-4 w-4" />}>
            Back to Lineage &amp; History
          </Button>
        </Link>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono font-medium text-amber-400 bg-amber-500/10 border border-amber-400/20">
          <Info className="h-3.5 w-3.5" />
          <span>SIMULATED RUN COMPARISON • FIXTURE DATA</span>
        </div>
      </div>

      {/* Page Title */}
      <div className="space-y-2">
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[var(--text-main)] tracking-tight">
          Version Compare &amp; Delta Inspector
        </h1>
        <p className="text-xs text-[var(--text-muted)] max-w-3xl leading-relaxed">
          Statically compare two analysis runs to detect resolved defects, persisting smells, and
          new regressions. Finding identity is tracked via robust SHA-256 token fingerprints rather
          than fragile line numbers.
        </p>
      </div>

      {/* High-Level Comparison Strip: Parent vs Child */}
      <div className="rounded-2xl glass-surface p-6 sm:p-8 space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
          {/* Base Run (Parent) */}
          <div className="md:col-span-5 p-5 rounded-xl bg-white/[0.02] border border-white/[0.06] space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-mono uppercase tracking-wider text-slate-500">
                Base Run (Original)
              </span>
              <GradeBadge grade={MOCK_RUN_LOGIN_DAO.grade || 'C'} size="sm" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-[var(--text-main)]">LoginDao.java</h3>
              <div className="text-[11px] font-mono text-slate-500">
                Run ID: {MOCK_RUN_LOGIN_DAO.runId}
              </div>
            </div>
            <div className="flex items-center justify-between pt-1">
              <ProvenanceBadge type="HUMAN" />
              <span className="font-mono text-lg font-bold text-slate-300">
                {MOCK_RUN_LOGIN_DAO.overallScore?.toFixed(1)} pts
              </span>
            </div>
            <div className="pt-2 border-t border-white/[0.04]">
              <Link
                to={`/report?runId=${MOCK_RUN_LOGIN_DAO.runId}`}
                className="text-xs font-mono text-sky-400 hover:underline flex items-center gap-1"
              >
                <span>Inspect Base Run Report</span>
                <ArrowRight className="h-3 w-3" />
              </Link>
            </div>
          </div>

          {/* Delta Indicator */}
          <div className="md:col-span-2 text-center space-y-1">
            <div className="inline-flex p-3 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 shadow-[0_0_20px_-3px_rgba(16,185,129,0.3)]">
              <TrendingUp className="h-6 w-6" />
            </div>
            <div className="font-mono text-xl font-black text-emerald-400 block">
              +{comparison.scoreDelta.toFixed(1)}
            </div>
            <span className="text-[10px] font-mono uppercase text-slate-400 block">
              Quality Delta
            </span>
          </div>

          {/* Target Run (Remediated Child) */}
          <div className="md:col-span-5 p-5 rounded-xl bg-white/[0.02] border border-emerald-500/30 space-y-3 shadow-[0_0_20px_-5px_rgba(16,185,129,0.15)]">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-mono uppercase tracking-wider text-emerald-400">
                Target Run (Remediated)
              </span>
              <GradeBadge grade={MOCK_RUN_LOGIN_DAO_REMEDIATED.grade || 'A'} size="sm" />
            </div>
            <div>
              <h3 className="font-bold text-sm text-[var(--text-main)]">
                LoginDao.java (Remediated v2)
              </h3>
              <div className="text-[11px] font-mono text-slate-500">
                Run ID: {MOCK_RUN_LOGIN_DAO_REMEDIATED.runId}
              </div>
            </div>
            <div className="flex items-center justify-between pt-1">
              <ProvenanceBadge type="AI_REMEDIATED" />
              <span className="font-mono text-lg font-bold text-emerald-400">
                {MOCK_RUN_LOGIN_DAO_REMEDIATED.overallScore?.toFixed(1)} pts
              </span>
            </div>
            <div className="pt-2 border-t border-white/[0.04]">
              <Link
                to={`/report?runId=${MOCK_RUN_LOGIN_DAO_REMEDIATED.runId}`}
                className="text-xs font-mono text-emerald-400 hover:underline flex items-center gap-1"
              >
                <span>Inspect Remediated Run Report</span>
                <ArrowRight className="h-3 w-3" />
              </Link>
            </div>
          </div>
        </div>

        {/* Dimension Score Deltas Grid */}
        <div className="space-y-3 pt-4 border-t border-white/[0.06]">
          <span className="text-[11px] font-mono uppercase tracking-wider text-slate-400 font-bold block">
            Per-Dimension Quality Deltas (fn_compare_runs):
          </span>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {comparison.dimensionDeltas.map((dim) => (
              <div
                key={dim.dimension}
                className="p-3.5 rounded-xl bg-black/30 border border-white/[0.06] space-y-1 text-xs font-mono"
              >
                <div className="text-slate-400 uppercase text-[10px] font-bold">
                  {dim.dimension}
                </div>
                <div className="flex items-baseline justify-between">
                  <span
                    className={`font-bold ${
                      dim.scoreDelta > 0
                        ? 'text-emerald-400'
                        : dim.scoreDelta < 0
                        ? 'text-rose-400'
                        : 'text-slate-400'
                    }`}
                  >
                    {dim.scoreDelta > 0 ? `+${dim.scoreDelta.toFixed(0)}` : dim.scoreDelta.toFixed(0)}{' '}
                    pts
                  </span>
                  <span className="text-[10px] text-slate-500">
                    Density: {dim.densityDelta.toFixed(1)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Tri-Split Finding Change Triage (Bible Section 4) */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* 1. Resolved Findings */}
        <div className="rounded-2xl glass-surface p-5 space-y-3 border-emerald-500/30">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4 text-emerald-400" />
              <h3 className="font-heading font-bold text-xs text-[var(--text-main)] uppercase tracking-wider">
                Resolved Defects ({comparison.resolved.length})
              </h3>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-500/15 text-emerald-300">
              ELIMINATED
            </span>
          </div>

          <div className="space-y-2">
            {comparison.resolved.map((f) => (
              <div
                key={f.findingId}
                className="p-3 rounded-xl bg-emerald-500/5 border border-emerald-500/20 text-xs space-y-1.5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono font-bold text-emerald-400">[{f.ruleCode}]</span>
                  <SeverityBadge severity={f.severity} />
                </div>
                <p className="text-[11px] text-slate-300">{f.message}</p>
                <div className="text-[10px] font-mono text-emerald-300 font-semibold">
                  Gain: +{f.scoreDeduction} pts recovered
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 2. Persisting Findings */}
        <div className="rounded-2xl glass-surface p-5 space-y-3 border-amber-500/30">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="h-4 w-4 text-amber-400" />
              <h3 className="font-heading font-bold text-xs text-[var(--text-main)] uppercase tracking-wider">
                Persisting Smells ({comparison.persisting.length})
              </h3>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-500/15 text-amber-300">
              UNMODIFIED
            </span>
          </div>

          <div className="space-y-2">
            {comparison.persisting.map((f) => (
              <div
                key={f.findingId}
                className="p-3 rounded-xl bg-amber-500/5 border border-amber-500/20 text-xs space-y-1.5"
              >
                <div className="flex items-center justify-between">
                  <span className="font-mono font-bold text-amber-400">[{f.ruleCode}]</span>
                  <SeverityBadge severity={f.severity} />
                </div>
                <p className="text-[11px] text-slate-300">{f.message}</p>
                <div className="text-[10px] font-mono text-slate-500">
                  Penalty persists (-{f.scoreDeduction} pts)
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 3. Introduced Regressions */}
        <div className="rounded-2xl glass-surface p-5 space-y-3 border-rose-500/30">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <XCircle className="h-4 w-4 text-rose-400" />
              <h3 className="font-heading font-bold text-xs text-[var(--text-main)] uppercase tracking-wider">
                New Regressions ({comparison.introduced.length})
              </h3>
            </div>
            <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-white/[0.04] text-slate-400">
              CLEAN (0)
            </span>
          </div>

          {comparison.introduced.length === 0 ? (
            <div className="p-6 text-center text-xs text-slate-500 space-y-1">
              <CheckCircle2 className="h-6 w-6 text-emerald-400 mx-auto" />
              <p>Zero new regressions introduced by candidate fix.</p>
            </div>
          ) : (
            comparison.introduced.map((f) => (
              <div
                key={f.findingId}
                className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-xs space-y-1"
              >
                <span className="font-mono font-bold text-rose-400">[{f.ruleCode}]</span>
                <p className="text-[11px] text-slate-300">{f.message}</p>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Unified Source Diff */}
      <div className="rounded-2xl glass-surface p-6 sm:p-8 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <GitCompare className="h-5 w-5 text-sky-400" />
            <h3 className="font-heading font-bold text-sm text-[var(--text-main)]">
              Unified Code Diff: Statement to PreparedStatement
            </h3>
          </div>
          <span className="text-[11px] font-mono text-slate-400">
            Edit Footprint: {(MOCK_REMEDIATION_SEC_001.editFootprint! * 100).toFixed(0)}% lines
          </span>
        </div>

        <div className="rounded-xl overflow-hidden border border-white/[0.08] bg-[#06080C] font-code text-xs leading-6">
          <pre className="p-4 overflow-x-auto text-slate-300 whitespace-pre">
            {MOCK_REMEDIATION_SEC_001.diff}
          </pre>
        </div>

        <div className="p-3.5 rounded-xl bg-black/30 border border-white/[0.04] text-xs font-mono text-slate-400 flex items-start gap-2">
          <ShieldCheck className="h-4 w-4 text-sky-400 shrink-0 mt-0.5" />
          <span>
            <strong>Robust Finding Identity:</strong> Line numbers shifted from line 12 to 14, but
            the platform recognized the same method and AST tokens using fingerprint hashing
            (Bible Section 4.5).
          </span>
        </div>
      </div>
    </div>
  );
};
