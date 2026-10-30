import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Code2,
  ArrowRight,
  SlidersHorizontal,
  CheckCircle2,
  Sparkles,
  ArrowUpRight,
  ShieldCheck,
  Zap,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import {
  ProvenanceBadge,
  GradeBadge,
} from '../components/common/Badge';
import { FloatingQuickDock } from '../components/common/FloatingQuickDock';
import { MOCK_SUBMISSION_SUMMARIES } from '../fixtures/mockSubmissions';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  // Honest default: Real data is empty because user has not submitted code yet.
  const [showSamplePreview, setShowSamplePreview] = useState(false);

  return (
    <div className="space-y-16 pb-16">
      {/* =========================================================================
          1. HERO OPENING: Spacious, Cinematic & Visual Focus
          ========================================================================= */}
      <section className="relative pt-6 sm:pt-10 pb-4 text-center space-y-6">
        {/* Subtle Ambient Radial Glow */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[550px] h-[280px] bg-gradient-to-b from-sky-500/12 via-cyan-500/5 to-transparent blur-3xl pointer-events-none -z-10" />

        {/* Top Thesis Pill */}
        <div className="inline-flex items-center gap-2 px-3.5 py-1 rounded-full text-xs font-mono font-medium text-sky-700 dark:text-sky-400 bg-sky-500/10 border border-sky-400/30 backdrop-blur-md shadow-[0_0_20px_-3px_rgba(56,189,248,0.25)]">
          <Sparkles className="h-3.5 w-3.5 text-amber-500 dark:text-amber-300" />
          <span>DETERMINISTIC ANALYSIS • ZERO EXECUTION HOST</span>
        </div>

        {/* Editorial Heading: Merriweather + Cormorant Garamond */}
        <div className="space-y-2 max-w-3xl mx-auto">
          <h1 className="font-heading text-3xl sm:text-5xl lg:text-6xl font-extrabold text-[var(--text-main)] tracking-tight leading-[1.15]">
            Precision Code Quality,{' '}
            <span className="font-editorial italic font-normal text-sky-600 dark:text-sky-400 block sm:inline">
              Verified by Measurement.
            </span>
          </h1>
          <p className="font-editorial italic text-lg sm:text-xl text-[var(--text-body)] max-w-2xl mx-auto leading-relaxed pt-2">
            Deterministic static analyzers are the judge; the LLM is the suspect.
            Measure whether an AI&apos;s code and proposed fixes are actually better.
          </p>
        </div>

        {/* Prominent Primary CTA */}
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
          <Button
            size="lg"
            variant="primary"
            icon={<Code2 className="h-5 w-5" />}
            onClick={() => navigate('/submit')}
            className="w-full sm:w-auto"
          >
            Analyze Java Code
          </Button>

          {/* Toggle Sample Data Action */}
          <button
            type="button"
            onClick={() => setShowSamplePreview(!showSamplePreview)}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-mono text-slate-700 dark:text-slate-400 hover:text-slate-950 dark:hover:text-white bg-white/70 dark:bg-white/[0.03] hover:bg-white dark:hover:bg-white/[0.08] border border-slate-200 dark:border-white/[0.08] transition-all cursor-pointer shadow-xs"
          >
            <SlidersHorizontal className="h-3.5 w-3.5 text-sky-600 dark:text-sky-400" />
            <span>{showSamplePreview ? 'Switch to Empty State' : 'Preview Sample Fixtures'}</span>
          </button>
        </div>
      </section>

      {/* =========================================================================
          2. MINIMALIST METRIC STRIP: De-emphasized, Low Density & Clean
          ========================================================================= */}
      <section className="rounded-2xl glass-surface p-6 sm:p-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6 sm:gap-8 divide-y sm:divide-y-0 sm:divide-x divide-slate-200 dark:divide-white/[0.06]">
          {/* Stat 1: Submissions */}
          <div className="space-y-1 sm:px-4">
            <span className="text-[11px] font-mono uppercase tracking-wider text-slate-600 dark:text-slate-400 block font-semibold">
              Total Submissions
            </span>
            <div className="flex items-baseline gap-2">
              <span className="font-mono text-2xl sm:text-3xl font-black text-[var(--text-main)]">
                {showSamplePreview ? '4' : '0'}
              </span>
              <span className="text-[11px] text-slate-600 dark:text-slate-400 font-mono">
                {showSamplePreview ? 'Java AST' : 'Empty'}
              </span>
            </div>
          </div>

          {/* Stat 2: Average Score */}
          <div className="space-y-1 pt-4 sm:pt-0 sm:px-4">
            <span className="text-[11px] font-mono uppercase tracking-wider text-slate-600 dark:text-slate-400 block font-semibold">
              Avg Quality Score
            </span>
            <div className="flex items-baseline gap-2">
              <span className="font-mono text-2xl sm:text-3xl font-black text-[var(--text-main)]">
                {showSamplePreview ? '82.4' : '—'}
              </span>
              {showSamplePreview && <GradeBadge grade="B" size="sm" />}
            </div>
          </div>

          {/* Stat 3: Verified Fix Rate */}
          <div className="space-y-1 pt-4 sm:pt-0 sm:px-4">
            <span className="text-[11px] font-mono uppercase tracking-wider text-slate-600 dark:text-slate-400 block font-semibold">
              Verified Fix Rate
            </span>
            <div className="flex items-baseline gap-2">
              <span className="font-mono text-2xl sm:text-3xl font-black text-emerald-600 dark:text-emerald-400">
                {showSamplePreview ? '75.0%' : '—'}
              </span>
              <span className="text-[10px] text-slate-600 dark:text-slate-400 font-mono font-medium">
                {showSamplePreview ? '95% CI' : 'Awaiting trial'}
              </span>
            </div>
          </div>

          {/* Stat 4: Security Findings */}
          <div className="space-y-1 pt-4 sm:pt-0 sm:px-4">
            <span className="text-[11px] font-mono uppercase tracking-wider text-slate-600 dark:text-slate-400 block font-semibold">
              High / Critical
            </span>
            <div className="flex items-baseline gap-2">
              <span className="font-mono text-2xl sm:text-3xl font-black text-amber-600 dark:text-amber-400">
                {showSamplePreview ? '1' : '0'}
              </span>
              <span className="text-[11px] text-slate-600 dark:text-slate-400 font-mono font-medium">
                {showSamplePreview ? 'SEC-001 active' : 'Clean'}
              </span>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          3. RECENT SUBMISSIONS / INTENTIONAL POLISHED EMPTY STATE
          ========================================================================= */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-heading text-lg sm:text-xl font-bold text-[var(--text-main)] tracking-tight">
              Recent Submissions &amp; Audits
            </h2>
            <p className="text-xs text-[var(--text-muted)] mt-0.5">
              Deterministic AST analysis records with provenance lineage
            </p>
          </div>
          {showSamplePreview && (
            <Link to="/history" className="text-xs font-mono text-sky-600 dark:text-sky-400 hover:underline flex items-center gap-1 font-medium">
              <span>View Full History</span>
              <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          )}
        </div>

        {showSamplePreview ? (
          <div className="rounded-2xl glass-surface overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-white/[0.06] text-slate-600 dark:text-slate-400 uppercase tracking-widest font-mono text-[10px] font-semibold">
                    <th className="py-3 px-4">File / Snippet</th>
                    <th className="py-3 px-4">Provenance</th>
                    <th className="py-3 px-4 text-center">Grade</th>
                    <th className="py-3 px-4 text-right">LOC</th>
                    <th className="py-3 px-4 text-right">Findings</th>
                    <th className="py-3 px-4 text-right">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-white/[0.04]">
                  {MOCK_SUBMISSION_SUMMARIES.map((sub) => (
                    <tr key={sub.submissionId} className="hover:bg-sky-50/50 dark:hover:bg-white/[0.02] transition-colors">
                      <td className="py-3.5 px-4">
                        <span className="font-medium text-[var(--text-main)] block">
                          {sub.title}
                        </span>
                        <span className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">
                          {sub.createdAt}
                        </span>
                      </td>
                      <td className="py-3.5 px-4">
                        <ProvenanceBadge type={sub.originType} />
                      </td>
                      <td className="py-3.5 px-4 text-center">
                        <GradeBadge grade={sub.latestGrade || 'N/A'} size="sm" />
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono text-slate-800 dark:text-slate-300 font-medium">
                        {sub.logicalLoc}
                      </td>
                      <td className="py-3.5 px-4 text-right font-mono">
                        {(sub.findingsCount ?? 0) > 0 ? (
                          <span className="text-amber-600 dark:text-amber-400 font-semibold">
                            {sub.findingsCount}
                          </span>
                        ) : (
                          <span className="text-emerald-600 dark:text-emerald-400 font-medium">0</span>
                        )}
                      </td>
                      <td className="py-3.5 px-4 text-right">
                        <Link to={`/report?runId=${sub.latestRunId}`}>
                          <Button variant="outline" size="sm">
                            Inspect
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          /* Polished, Intentional Empty State with Verification Flow Visual */
          <div className="rounded-2xl glass-surface p-8 sm:p-12 text-center space-y-8">
            <div className="max-w-xl mx-auto space-y-2">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono text-sky-700 dark:text-sky-300 bg-sky-500/10 border border-sky-400/30">
                <CheckCircle2 className="h-3 w-3 text-sky-600 dark:text-sky-400" />
                <span>Zero Submissions Recorded</span>
              </span>
              <h3 className="font-heading text-xl font-bold text-[var(--text-main)]">
                The Workspace is Ready for Code
              </h3>
              <p className="text-xs text-[var(--text-muted)] leading-relaxed">
                Submit a Java file or snippet to trigger the AST parser and run the four
                in-house analyzers. Code is analyzed statically and never executed.
              </p>
            </div>

            {/* Verification Flow Diagram */}
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-3 max-w-4xl mx-auto text-left">
              {[
                {
                  step: '01',
                  title: 'Submit Java',
                  desc: 'Language Gate checks class/record declarations. Non-Java rejected via HTTP 422.',
                },
                {
                  step: '02',
                  title: '4 Deterministic Audits',
                  desc: 'Complexity, Duplication, Security, Maintainability executed in parallel.',
                },
                {
                  step: '03',
                  title: 'AI Proposes Patch',
                  desc: 'Model suggests candidate remediation (BARE vs GUIDED).',
                },
                {
                  step: '04',
                  title: 'Verified Remediation',
                  desc: 'Sandbox re-analysis classifies outcome: clean, regressed, or broken.',
                },
              ].map((item) => (
                <div
                  key={item.step}
                  className="p-4 rounded-xl bg-sky-50/60 dark:bg-white/[0.02] border border-slate-200/80 dark:border-white/[0.06] space-y-1.5 shadow-2xs"
                >
                  <span className="text-[10px] font-mono text-sky-600 dark:text-sky-400 font-bold block">
                    STEP {item.step}
                  </span>
                  <h4 className="text-xs font-bold text-[var(--text-main)]">
                    {item.title}
                  </h4>
                  <p className="text-[11px] text-slate-600 dark:text-slate-400 leading-normal">
                    {item.desc}
                  </p>
                </div>
              ))}
            </div>

            <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
              <Button
                variant="primary"
                size="md"
                icon={<Code2 className="h-4 w-4" />}
                onClick={() => navigate('/submit')}
              >
                Analyze First Java File
              </Button>
              <Button
                variant="secondary"
                size="md"
                icon={<SlidersHorizontal className="h-4 w-4 text-sky-400" />}
                onClick={() => setShowSamplePreview(true)}
              >
                Preview Sample Fixtures
              </Button>
            </div>
          </div>
        )}
      </section>

      {/* =========================================================================
          4. SCROLL-SPLIT SECTION: The 4 Deterministic Analyzers (Componentry Inspired)
          ========================================================================= */}
      <section className="pt-8 border-t border-slate-200 dark:border-white/[0.06]">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Left Column: Sticky Title & Concept */}
          <div className="lg:col-span-5 lg:sticky lg:top-24 space-y-4">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono text-sky-700 dark:text-sky-400 bg-sky-500/10 border border-sky-400/30">
              <Zap className="h-3 w-3 text-sky-600 dark:text-sky-400" />
              <span>CORE ARCHITECTURE</span>
            </div>
            <h2 className="font-heading text-2xl sm:text-3xl font-extrabold text-[var(--text-main)] leading-tight">
              The 4 Deterministic Analyzers
            </h2>
            <p className="font-editorial italic text-base text-[var(--text-body)] leading-relaxed">
              &ldquo;PMD already reports complexity, CPD finds clones, SpotBugs finds vulnerabilities.
              What did you write?&rdquo;
            </p>
            <p className="text-xs text-[var(--text-muted)] leading-relaxed">
              We wrote our own analyzers for Java AST parsing, token-based clone detection,
              and source-sink pattern matching. They operate independently of any LLM service.
            </p>
            <div className="p-3.5 rounded-xl bg-sky-50/80 dark:bg-white/[0.02] border border-sky-100 dark:border-white/[0.06] text-xs font-mono space-y-1">
              <div className="text-sky-700 dark:text-sky-400 font-bold">Transparent Scoring Weight:</div>
              <div className="text-slate-700 dark:text-slate-400 text-[11px]">
                Security (35%) + Complexity (25%) + Duplication (20%) + Maintainability (20%) = 100%
              </div>
            </div>
          </div>

          {/* Right Column: Flowing Cards for Each Analyzer */}
          <div className="lg:col-span-7 space-y-4">
            {/* Analyzer 1: Security */}
            <div className="p-6 rounded-2xl glass-surface space-y-3 hover:border-sky-400/40 transition-colors shadow-2xs">
              <div className="flex items-center justify-between">
                <span className="font-heading font-bold text-base text-[var(--text-main)]">
                  1. Security Analyzer
                </span>
                <span className="font-mono text-xs font-bold text-amber-700 dark:text-amber-400 bg-amber-400/10 px-2.5 py-0.5 rounded-full border border-amber-400/30">
                  35% Weight
                </span>
              </div>
              <p className="text-xs text-[var(--text-muted)] leading-relaxed">
                Source-sink taint pattern detection for SQL injection, hardcoded cryptographic keys,
                and weak PRNGs. Rule codes map to CWE passages for grounded explanations.
              </p>
              <div className="p-3.5 rounded-xl bg-sky-50/70 dark:bg-black/40 border border-sky-100 dark:border-white/[0.06] font-code text-[11px] text-slate-800 dark:text-slate-300">
                <span className="text-rose-600 dark:text-rose-400 font-semibold">[SEC-001]</span>{' '}
                <span className="text-slate-700 dark:text-slate-300">Statement.executeQuery(&quot;...&quot; + user);</span>
                <span className="block text-emerald-700 dark:text-emerald-400 font-medium mt-1">
                  Fix: PreparedStatement with parameterized &apos;?&apos; placeholders
                </span>
              </div>
            </div>

            {/* Analyzer 2: Complexity */}
            <div className="p-6 rounded-2xl glass-surface space-y-3 hover:border-sky-400/40 transition-colors shadow-2xs">
              <div className="flex items-center justify-between">
                <span className="font-heading font-bold text-base text-[var(--text-main)]">
                  2. Complexity Analyzer
                </span>
                <span className="font-mono text-xs font-bold text-sky-700 dark:text-sky-400 bg-sky-400/10 px-2.5 py-0.5 rounded-full border border-sky-400/30">
                  25% Weight
                </span>
              </div>
              <p className="text-xs text-[var(--text-muted)] leading-relaxed">
                Calculates both cyclomatic and cognitive complexity across method declarations.
                Flags deeply nested branching and combinatorial decision points.
              </p>
              <div className="p-3.5 rounded-xl bg-sky-50/70 dark:bg-black/40 border border-sky-100 dark:border-white/[0.06] font-code text-[11px] text-slate-800 dark:text-slate-300">
                <span className="text-amber-600 dark:text-amber-400 font-semibold">[COMP-001]</span> Cyclomatic Complexity &gt; 15
                <span className="block text-slate-600 dark:text-slate-400 mt-0.5">Method requires decomposition into specialized helpers</span>
              </div>
            </div>

            {/* Analyzer 3: Duplication */}
            <div className="p-6 rounded-2xl glass-surface space-y-3 hover:border-sky-400/40 transition-colors shadow-2xs">
              <div className="flex items-center justify-between">
                <span className="font-heading font-bold text-base text-[var(--text-main)]">
                  3. Duplication Analyzer
                </span>
                <span className="font-mono text-xs font-bold text-cyan-700 dark:text-cyan-400 bg-cyan-400/10 px-2.5 py-0.5 rounded-full border border-cyan-400/30">
                  20% Weight
                </span>
              </div>
              <p className="text-xs text-[var(--text-muted)] leading-relaxed">
                Token-based clone detection. Detects Type-1 (identical tokens) and Type-2 (renamed identifiers)
                repetition without relying on heavyweight compiler dependencies.
              </p>
              <div className="p-3.5 rounded-xl bg-sky-50/70 dark:bg-black/40 border border-sky-100 dark:border-white/[0.06] font-code text-[11px] text-slate-800 dark:text-slate-300">
                <span className="text-cyan-700 dark:text-cyan-400 font-semibold">[DUP-001]</span> Clone group of 28 tokens
                <span className="block text-slate-600 dark:text-slate-400 mt-0.5">Duplicated in 2 occurrences across file</span>
              </div>
            </div>

            {/* Analyzer 4: Maintainability */}
            <div className="p-6 rounded-2xl glass-surface space-y-3 hover:border-sky-400/40 transition-colors shadow-2xs">
              <div className="flex items-center justify-between">
                <span className="font-heading font-bold text-base text-[var(--text-main)]">
                  4. Maintainability Analyzer
                </span>
                <span className="font-mono text-xs font-bold text-teal-700 dark:text-teal-400 bg-teal-400/10 px-2.5 py-0.5 rounded-full border border-teal-400/30">
                  20% Weight
                </span>
              </div>
              <p className="text-xs text-[var(--text-muted)] leading-relaxed">
                Evaluates exception anti-patterns (empty catch, catching Throwable, swallowed exceptions)
                which are unusually common in AI-generated code.
              </p>
              <div className="p-3.5 rounded-xl bg-sky-50/70 dark:bg-black/40 border border-sky-100 dark:border-white/[0.06] font-code text-[11px] text-slate-800 dark:text-slate-300">
                <span className="text-teal-700 dark:text-teal-400 font-semibold">[MAINT-001]</span> Empty catch block detected
                <span className="block text-slate-600 dark:text-slate-400 mt-0.5">Exceptions must be logged or rethrown</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* =========================================================================
          5. CLOSED-LOOP VERIFICATION & 5 OUTCOMES SECTION
          ========================================================================= */}
      <section className="pt-8 border-t border-slate-200 dark:border-white/[0.06] space-y-6">
        <div className="max-w-2xl">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono text-sky-700 dark:text-sky-400 bg-sky-500/10 border border-sky-400/30">
            <ShieldCheck className="h-3 w-3 text-sky-600 dark:text-sky-400" />
            <span>THE ONE DIFFERENTIATOR</span>
          </div>
          <h2 className="font-heading text-2xl font-bold text-[var(--text-main)] mt-2">
            The Closed-Loop Verification Model
          </h2>
          <p className="text-xs text-[var(--text-muted)] mt-1 leading-relaxed">
            Every AI patch is independently re-audited in a sandbox. The result is classified into
            five deterministic outcome classes:
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          {[
            {
              outcome: 'VERIFIED_FIX',
              color: 'text-emerald-800 dark:text-emerald-400 border-emerald-300 dark:border-emerald-500/30 bg-white/90 dark:bg-emerald-500/10 shadow-2xs',
              desc: 'Target finding resolved, zero new findings >= MEDIUM, public signatures intact.',
            },
            {
              outcome: 'PARTIAL',
              color: 'text-teal-800 dark:text-teal-400 border-teal-300 dark:border-teal-500/30 bg-white/90 dark:bg-teal-500/10 shadow-2xs',
              desc: 'Target finding resolved, but low or info smells introduced.',
            },
            {
              outcome: 'NO_EFFECT',
              color: 'text-slate-800 dark:text-slate-400 border-slate-300 dark:border-slate-500/30 bg-white/90 dark:bg-slate-500/10 shadow-2xs',
              desc: 'Target finding remains unresolved after patch.',
            },
            {
              outcome: 'REGRESSION',
              color: 'text-rose-800 dark:text-rose-400 border-rose-300 dark:border-rose-500/30 bg-white/90 dark:bg-rose-500/10 shadow-2xs',
              desc: 'Introduced new findings of MEDIUM, HIGH, or CRITICAL severity.',
            },
            {
              outcome: 'BROKEN',
              color: 'text-red-800 dark:text-red-400 border-red-300 dark:border-red-500/30 bg-white/90 dark:bg-red-500/10 shadow-2xs',
              desc: 'Patched code fails to parse, changes signatures, or JUnit tests fail.',
            },
          ].map((item) => (
            <div
              key={item.outcome}
              className={`p-4 rounded-xl border backdrop-blur-md space-y-2 ${item.color}`}
            >
              <div className="font-mono text-xs font-bold tracking-tight">
                {item.outcome}
              </div>
              <p className="text-[11px] text-slate-700 dark:text-slate-400 leading-normal font-normal">
                {item.desc}
              </p>
            </div>
          ))}
        </div>

        {/* Quick Launch Banner */}
        <div className="rounded-2xl glass-surface-elevated p-6 flex flex-col sm:flex-row items-center justify-between gap-4 border border-sky-400/30 shadow-2xs">
          <div className="space-y-1 text-center sm:text-left">
            <h3 className="font-heading font-bold text-sm text-[var(--text-main)]">
              Ready to test a Java snippet against the analyzers?
            </h3>
            <p className="text-xs text-[var(--text-muted)]">
              Execute client-side Language Gate validation and review logical LOC.
            </p>
          </div>
          <Link to="/submit">
            <Button variant="primary" icon={<ArrowUpRight className="h-4 w-4" />}>
              Open Analyze Code
            </Button>
          </Link>
        </div>
      </section>

      {/* Floating Quick Dock for Secondary Shortcuts */}
      <FloatingQuickDock
        onToggleSample={() => setShowSamplePreview(!showSamplePreview)}
        showSamplePreview={showSamplePreview}
      />
    </div>
  );
};
