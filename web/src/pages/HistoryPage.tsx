import React, { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  GitBranch,
  ShieldCheck,
  Info,
  Calendar,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import {
  GradeBadge,
  ProvenanceBadge,
  OutcomeBadge,
} from '../components/common/Badge';
import { MOCK_SUBMISSION_SUMMARIES } from '../fixtures/mockSubmissions';
import { MOCK_REMEDIATIONS } from '../fixtures/mockRemediations';
import type { OriginType } from '../types/submission';

export const HistoryPage: React.FC = () => {
  // Provenance filter
  const [selectedOrigin, setSelectedOrigin] = useState<string>('ALL');

  const filteredSummaries = useMemo(() => {
    if (selectedOrigin === 'ALL') return MOCK_SUBMISSION_SUMMARIES;
    return MOCK_SUBMISSION_SUMMARIES.filter((s) => s.originType === selectedOrigin);
  }, [selectedOrigin]);

  return (
    <div className="space-y-8 max-w-5xl mx-auto pb-16">
      {/* Top Header & Simulation Pill */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <Link to="/">
          <Button variant="ghost" size="sm" icon={<ArrowLeft className="h-4 w-4" />}>
            Back to Dashboard
          </Button>
        </Link>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono font-medium text-amber-400 bg-amber-500/10 border border-amber-400/20">
          <Info className="h-3.5 w-3.5" />
          <span>AUDIT HISTORY • SIMULATED RECORDS</span>
        </div>
      </div>

      {/* Page Title */}
      <div className="space-y-2">
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[var(--text-main)] tracking-tight">
          Submission History &amp; Remediation Lineage
        </h1>
        <p className="text-xs text-[var(--text-muted)] max-w-3xl leading-relaxed">
          Inspect historical Java audit records, filter by code provenance lineage, and trace
          parent-child version DAGs resulting from automated AI remediation verification.
        </p>
      </div>

      {/* Filter Tabs */}
      <div className="rounded-2xl glass-surface p-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="text-xs font-mono text-slate-400">Filter Provenance:</span>
          {(['ALL', 'HUMAN', 'AI_GENERATED', 'AI_REMEDIATED'] as (OriginType | 'ALL')[]).map(
            (origin) => (
              <button
                key={origin}
                type="button"
                onClick={() => setSelectedOrigin(origin)}
                className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all border ${
                  selectedOrigin === origin
                    ? 'bg-sky-500/20 text-sky-300 border-sky-400/40 shadow-[0_0_15px_-3px_rgba(56,189,248,0.25)]'
                    : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05] hover:text-white'
                }`}
              >
                {origin}
              </button>
            )
          )}
        </div>

        <span className="text-xs font-mono text-slate-500">
          Showing {filteredSummaries.length} submissions
        </span>
      </div>

      {/* Submissions Table */}
      <div className="rounded-2xl glass-surface overflow-hidden border border-white/[0.06]">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-white/[0.06] text-slate-500 uppercase tracking-widest font-mono text-[10px]">
                <th className="py-3 px-4">File / Unit</th>
                <th className="py-3 px-4">Provenance</th>
                <th className="py-3 px-4 text-center">Grade</th>
                <th className="py-3 px-4 text-right">Score</th>
                <th className="py-3 px-4 text-right">Logical LOC</th>
                <th className="py-3 px-4 text-right">Findings</th>
                <th className="py-3 px-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.04]">
              {filteredSummaries.map((sub) => (
                <tr key={sub.submissionId} className="hover:bg-white/[0.02] transition-colors">
                  <td className="py-3.5 px-4">
                    <span className="font-medium text-[var(--text-main)] block">
                      {sub.title}
                    </span>
                    <span className="text-[10px] text-slate-500 font-mono flex items-center gap-1 mt-0.5">
                      <Calendar className="h-3 w-3" />
                      {new Date(sub.createdAt).toLocaleDateString()}
                    </span>
                  </td>
                  <td className="py-3.5 px-4">
                    <ProvenanceBadge type={sub.originType} />
                  </td>
                  <td className="py-3.5 px-4 text-center">
                    <GradeBadge grade={sub.latestGrade || 'N/A'} size="sm" />
                  </td>
                  <td className="py-3.5 px-4 text-right font-mono text-slate-200">
                    {sub.latestScore ? `${sub.latestScore.toFixed(1)}` : '—'}
                  </td>
                  <td className="py-3.5 px-4 text-right font-mono text-slate-400">
                    {sub.logicalLoc}
                  </td>
                  <td className="py-3.5 px-4 text-right font-mono">
                    {(sub.findingsCount ?? 0) > 0 ? (
                      <span className="text-amber-400 font-bold">{sub.findingsCount}</span>
                    ) : (
                      <span className="text-emerald-400 font-medium">0</span>
                    )}
                  </td>
                  <td className="py-3.5 px-4 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <Link to={`/report?runId=${sub.latestRunId}`}>
                        <Button variant="outline" size="sm">
                          Inspect
                        </Button>
                      </Link>
                      {sub.originType === 'AI_REMEDIATED' && (
                        <Link to="/compare">
                          <Button variant="ghost" size="sm" className="text-emerald-400 hover:text-emerald-300">
                            Diff
                          </Button>
                        </Link>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Multi-Generation Remediation Lineage DAG (Bible Section 4 & 9 Lineage Chain) */}
      <div className="rounded-2xl glass-surface p-6 sm:p-8 space-y-6">
        <div className="flex items-center gap-2">
          <GitBranch className="h-5 w-5 text-sky-400" />
          <h2 className="font-heading text-lg font-bold text-[var(--text-main)]">
            Remediation Lineage DAG: LoginDao Version Chain
          </h2>
        </div>
        <p className="text-xs text-[var(--text-muted)] leading-relaxed">
          Demonstrates Bible Section 4 &amp; 9 self-referential lineage (<code>parent_submission_id</code>).
          When an AI candidate patch passes the verification sandbox, an <code>AI_REMEDIATED</code> child
          submission is created with its own independent analysis run.
        </p>

        {/* Lineage Visual Flow */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-center">
          {/* Node 1: Parent Baseline */}
          <div className="md:col-span-5 p-5 rounded-xl bg-white/[0.02] border border-white/[0.06] space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-mono text-xs text-sky-400 font-bold">
                Generation 0 (Parent)
              </span>
              <GradeBadge grade="C" size="sm" />
            </div>
            <div>
              <div className="font-bold text-sm text-[var(--text-main)]">LoginDao.java</div>
              <div className="text-[11px] font-mono text-slate-500">
                sub-9b1e-001 • 15 LLOC • HUMAN
              </div>
            </div>
            <div className="p-2.5 rounded-lg bg-black/30 text-xs font-mono space-y-1 text-slate-400">
              <div className="text-rose-400">1 High-Severity Vulnerability:</div>
              <div>[SEC-001] SQL Injection (CWE-89)</div>
              <div className="text-slate-500 text-[10px]">Score: 68.5 / 100</div>
            </div>
            <div className="pt-1">
              <Link
                to="/report?runId=run-c7a2-001"
                className="text-xs font-mono text-sky-400 hover:underline flex items-center gap-1"
              >
                <span>Inspect Baseline Report</span>
                <ArrowRight className="h-3 w-3" />
              </Link>
            </div>
          </div>

          {/* Transition: Remediation Edge */}
          <div className="md:col-span-2 text-center space-y-2 py-2">
            <div className="inline-flex p-2 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div className="text-[10px] font-mono font-bold text-emerald-400 block">
              AI PATCH VERIFIED
            </div>
            <OutcomeBadge outcome="VERIFIED_FIX" className="scale-90" />
          </div>

          {/* Node 2: Child Remediated */}
          <div className="md:col-span-5 p-5 rounded-xl bg-white/[0.02] border border-emerald-500/30 space-y-3 shadow-[0_0_20px_-5px_rgba(16,185,129,0.15)]">
            <div className="flex items-center justify-between">
              <span className="font-mono text-xs text-emerald-400 font-bold">
                Generation 1 (Child)
              </span>
              <GradeBadge grade="A" size="sm" />
            </div>
            <div>
              <div className="font-bold text-sm text-[var(--text-main)]">
                LoginDao.java (Remediated v2)
              </div>
              <div className="text-[11px] font-mono text-slate-500">
                sub-7f2a-004 • 18 LLOC • AI_REMEDIATED
              </div>
            </div>
            <div className="p-2.5 rounded-lg bg-black/30 text-xs font-mono space-y-1 text-slate-400">
              <div className="text-emerald-400 font-semibold">0 Security Findings:</div>
              <div>PreparedStatement parameterized &apos;?&apos; placeholders</div>
              <div className="text-emerald-300 text-[10px] font-bold">
                Score: 98.0 / 100 (+29.5 pts)
              </div>
            </div>
            <div className="pt-1">
              <Link
                to="/report?runId=run-88ae-004"
                className="text-xs font-mono text-emerald-400 hover:underline flex items-center gap-1"
              >
                <span>Inspect Remediated Report</span>
                <ArrowRight className="h-3 w-3" />
              </Link>
            </div>
          </div>
        </div>

        {/* CTA to Compare */}
        <div className="flex justify-end pt-2">
          <Link to="/compare">
            <Button variant="primary" icon={<ArrowRight className="h-4 w-4" />}>
              Open Version Comparison &amp; Delta Inspector
            </Button>
          </Link>
        </div>
      </div>

      {/* 5 Outcomes Taxonomy Card */}
      <div className="rounded-2xl glass-surface p-6 space-y-4">
        <h3 className="font-heading text-sm font-bold text-[var(--text-main)]">
          The 5 Closed-Loop Remediation Outcome Categories in Research
        </h3>
        <p className="text-xs text-[var(--text-muted)]">
          Every remediation generated by the LLM is classified strictly into one of five deterministic outcome classes:
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {MOCK_REMEDIATIONS.map((rem) => (
            <div
              key={rem.outcome}
              className="p-3.5 rounded-xl bg-black/30 border border-white/[0.06] space-y-2 text-xs"
            >
              <OutcomeBadge outcome={rem.outcome} />
              <p className="text-[11px] text-slate-400 leading-normal">
                {rem.explanationSummary}
              </p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
