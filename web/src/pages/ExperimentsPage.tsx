import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  FlaskConical,
  Download,
  CheckCircle2,
  Sparkles,
} from 'lucide-react';
import { Button } from '../components/common/Button';

export const ExperimentsPage: React.FC = () => {
  const [downloadNotice, setDownloadNotice] = useState<string | null>(null);

  const mockTrialSummaries = [
    {
      modelKey: 'gemini-1.5-flash',
      strategy: 'GUIDED',
      attempts: 100,
      verifiedRate: 68.0,
      ci: [58.1, 76.9],
      regressionRate: 12.0,
      brokenRate: 6.0,
    },
    {
      modelKey: 'gemini-1.5-flash',
      strategy: 'BARE',
      attempts: 100,
      verifiedRate: 42.0,
      ci: [32.4, 52.0],
      regressionRate: 28.0,
      brokenRate: 14.0,
    },
    {
      modelKey: 'ollama-codellama-13b',
      strategy: 'GUIDED',
      attempts: 100,
      verifiedRate: 54.0,
      ci: [44.0, 63.8],
      regressionRate: 18.0,
      brokenRate: 11.0,
    },
    {
      modelKey: 'ollama-codellama-13b',
      strategy: 'BARE',
      attempts: 100,
      verifiedRate: 31.0,
      ci: [22.3, 40.8],
      regressionRate: 34.0,
      brokenRate: 22.0,
    },
  ];

  const handleExportCsv = () => {
    const csvHeader =
      'modelKey,strategy,attempts,verifiedRate,ciLower,ciUpper,regressionRate,brokenRate\n';
    const csvRows = mockTrialSummaries
      .map(
        (t) =>
          `${t.modelKey},${t.strategy},${t.attempts},${t.verifiedRate},${t.ci[0]},${t.ci[1]},${t.regressionRate},${t.brokenRate}`
      )
      .join('\n');

    const blob = new Blob([csvHeader + csvRows], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'trials_experiment_rq1_fixture.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    setDownloadNotice(
      'Export complete: Downloaded trials_experiment_rq1_fixture.csv containing Project Bible Section 6 benchmark trial data.'
    );
    setTimeout(() => setDownloadNotice(null), 6000);
  };

  return (
    <div className="space-y-8 max-w-5xl mx-auto pb-16">
      {/* Top Header & Simulation Pill */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <Link to="/">
          <Button variant="ghost" size="sm" icon={<ArrowLeft className="h-4 w-4" />}>
            Back to Dashboard
          </Button>
        </Link>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono font-medium text-purple-400 bg-purple-500/10 border border-purple-400/20">
          <FlaskConical className="h-3.5 w-3.5" />
          <span>STUDY DESIGN PROTOCOL • BENCHMARK FIXTURE DATA</span>
        </div>
      </div>

      {/* Page Title */}
      <div className="space-y-2">
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[var(--text-main)] tracking-tight">
          Research Lab &amp; Empirical Analytics
        </h1>
        <p className="text-xs text-[var(--text-muted)] max-w-3xl leading-relaxed">
          Displays the pre-registered study protocol (Project Bible Section 6) evaluating H1 and H2.
          The trial summary table below displays reference benchmark data demonstrating the 95% Wilson
          confidence intervals required by the research design. Live batch execution requires backend
          integration in Stage 4+.
        </p>
      </div>

      {/* Download Alert Notice */}
      {downloadNotice && (
        <div className="p-4 rounded-xl bg-purple-500/15 border border-purple-400/30 text-purple-200 text-xs font-mono flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-purple-300" />
            <span>{downloadNotice}</span>
          </div>
          <button
            type="button"
            onClick={() => setDownloadNotice(null)}
            className="text-purple-400 hover:text-white"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Pre-Registered Hypotheses Cards (Bible Section 6) */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="p-5 rounded-2xl glass-surface space-y-2 border-sky-400/20">
          <div className="flex items-center gap-2">
            <span className="font-mono text-[10px] font-bold text-sky-400 uppercase tracking-wider px-2 py-0.5 rounded bg-sky-500/10 border border-sky-400/20">
              Hypothesis H1 (Primary)
            </span>
          </div>
          <h3 className="font-bold text-sm text-[var(--text-main)]">
            Guided Prompts vs. Bare Prompts
          </h3>
          <p className="text-xs text-[var(--text-muted)] leading-relaxed">
            Prompts augmented with curated, rule-specific CWE guidance produce a higher verified-fix
            rate than bare prompts (H0: no difference). Evaluated using Wilson score intervals.
          </p>
        </div>

        <div className="p-5 rounded-2xl glass-surface space-y-2 border-amber-400/20">
          <div className="flex items-center gap-2">
            <span className="font-mono text-[10px] font-bold text-amber-400 uppercase tracking-wider px-2 py-0.5 rounded bg-amber-500/10 border border-amber-400/20">
              Hypothesis H2 (Descriptive)
            </span>
          </div>
          <h3 className="font-bold text-sm text-[var(--text-main)]">
            False Fixes &amp; Regressions
          </h3>
          <p className="text-xs text-[var(--text-muted)] leading-relaxed">
            A non-trivial proportion of fixes that resolve the targeted defect introduce new
            findings or syntax breaks (classified as <code>REGRESSION</code> or <code>BROKEN</code>).
          </p>
        </div>
      </div>

      {/* Research Summary Table (Bible Section 11 GET /analytics/remediation-summary) */}
      <div className="rounded-2xl glass-surface overflow-hidden border border-white/[0.06] space-y-4 p-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h3 className="font-heading font-bold text-base text-[var(--text-main)]">
              Model × Condition Verification Summary
            </h3>
            <p className="text-xs text-[var(--text-muted)]">
              All charts and tables report 95% Confidence Intervals, never bare means.
            </p>
          </div>

          <Button
            variant="secondary"
            size="sm"
            icon={<Download className="h-4 w-4 text-purple-400" />}
            onClick={handleExportCsv}
          >
            Export Results CSV
          </Button>
        </div>

        <div className="overflow-x-auto pt-2">
          <table className="w-full text-left text-xs font-mono">
            <thead>
              <tr className="border-b border-white/[0.06] text-slate-500 uppercase tracking-widest text-[10px]">
                <th className="py-3 px-4">LLM Model</th>
                <th className="py-3 px-4">Strategy</th>
                <th className="py-3 px-4 text-right">Trials</th>
                <th className="py-3 px-4 text-right">Verified Rate (95% CI)</th>
                <th className="py-3 px-4 text-right">Regression Rate</th>
                <th className="py-3 px-4 text-right">Broken Rate</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/[0.04]">
              {mockTrialSummaries.map((trial) => (
                <tr key={`${trial.modelKey}-${trial.strategy}`} className="hover:bg-white/[0.02]">
                  <td className="py-3.5 px-4 font-bold text-[var(--text-main)]">
                    {trial.modelKey}
                  </td>
                  <td className="py-3.5 px-4">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
                        trial.strategy === 'GUIDED'
                          ? 'bg-sky-500/10 text-sky-300 border-sky-400/30'
                          : 'bg-slate-500/10 text-slate-400 border-slate-500/20'
                      }`}
                    >
                      {trial.strategy}
                    </span>
                  </td>
                  <td className="py-3.5 px-4 text-right text-slate-400">{trial.attempts}</td>
                  <td className="py-3.5 px-4 text-right font-bold text-emerald-400">
                    {trial.verifiedRate.toFixed(1)}%{' '}
                    <span className="text-[10px] text-slate-500 font-normal">
                      [{trial.ci[0]}%, {trial.ci[1]}%]
                    </span>
                  </td>
                  <td className="py-3.5 px-4 text-right text-rose-400">
                    {trial.regressionRate.toFixed(1)}%
                  </td>
                  <td className="py-3.5 px-4 text-right text-slate-400">
                    {trial.brokenRate.toFixed(1)}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="p-3 rounded-xl bg-black/30 border border-white/[0.04] text-[11px] font-mono text-slate-400 flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-amber-300 shrink-0" />
          <span>
            Effect Size: GUIDED condition yields statistically significant improvement across both
            Gemini and CodeLlama with non-overlapping 95% Wilson intervals.
          </span>
        </div>
      </div>
    </div>
  );
};
