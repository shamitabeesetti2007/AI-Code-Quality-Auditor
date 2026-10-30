import React, { useState, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  Search,
  BookOpen,
  ThumbsUp,
  ThumbsDown,
  Info,
  ExternalLink,
  ShieldAlert,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import { SeverityBadge } from '../components/common/Badge';
import { MOCK_FINDINGS } from '../fixtures/mockFindings';
import { MOCK_EXPLANATIONS } from '../fixtures/mockExplanations';
import type { FindingSeverity } from '../types/finding';
import type { DimensionType } from '../types/analysis';

export const FindingsPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const highlightedFindingId = searchParams.get('findingId');

  // Filter state
  const [selectedSeverity, setSelectedSeverity] = useState<string>('ALL');
  const [selectedDimension, setSelectedDimension] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Expandable explanation state: user toggle takes precedence, otherwise defaults to query param or first finding
  const [userToggledId, setUserToggledId] = useState<string | null | undefined>(undefined);

  const expandedFindingId =
    userToggledId !== undefined
      ? userToggledId
      : highlightedFindingId || 'fnd-89a1-001';

  // Local simulated triage feedback state (findingId -> 'TRUE_POSITIVE' | 'FALSE_POSITIVE')
  const [feedbackState, setFeedbackState] = useState<Record<string, string>>({});

  const filteredFindings = useMemo(() => {
    return MOCK_FINDINGS.filter((f) => {
      if (selectedSeverity !== 'ALL' && f.severity !== selectedSeverity) return false;
      if (selectedDimension !== 'ALL' && f.dimension !== selectedDimension) return false;
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const matchCode = f.ruleCode.toLowerCase().includes(query);
        const matchMsg = f.message.toLowerCase().includes(query);
        if (!matchCode && !matchMsg) return false;
      }
      return true;
    });
  }, [selectedSeverity, selectedDimension, searchQuery]);

  const handleFeedback = (findingId: string, label: 'TRUE_POSITIVE' | 'FALSE_POSITIVE') => {
    setFeedbackState((prev) => ({
      ...prev,
      [findingId]: label,
    }));
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
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono font-medium text-amber-400 bg-amber-500/10 border border-amber-400/20">
          <Info className="h-3.5 w-3.5" />
          <span>TRIAGE EXPLORER • SIMULATED DATA</span>
        </div>
      </div>

      {/* Page Title */}
      <div className="space-y-2">
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[var(--text-main)] tracking-tight">
          Findings Explorer &amp; Grounded Guidance
        </h1>
        <p className="text-xs text-[var(--text-muted)] max-w-3xl leading-relaxed">
          Review located defects, explore retrieval-augmented plain-language explanations with
          verifiable CWE citations, and label findings to assess analyzer precision.
        </p>
      </div>

      {/* Filter Toolbar */}
      <div className="rounded-2xl glass-surface p-4 space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-12 gap-3">
          {/* Search Box */}
          <div className="sm:col-span-5 relative">
            <Search className="h-4 w-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search rule or message (e.g. SEC-001, SQL)..."
              className="w-full pl-9 pr-3.5 py-2 rounded-xl bg-black/30 border border-white/[0.08] text-xs font-mono text-[var(--text-main)] placeholder-slate-600 focus:outline-none focus:border-sky-400/50"
            />
          </div>

          {/* Severity Dropdown */}
          <div className="sm:col-span-3">
            <select
              value={selectedSeverity}
              onChange={(e) => setSelectedSeverity(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/30 border border-white/[0.08] text-xs font-mono text-[var(--text-main)] focus:outline-none focus:border-sky-400/50"
            >
              <option value="ALL">All Severities</option>
              {(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'] as FindingSeverity[]).map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>

          {/* Dimension Dropdown */}
          <div className="sm:col-span-4">
            <select
              value={selectedDimension}
              onChange={(e) => setSelectedDimension(e.target.value)}
              className="w-full px-3 py-2 rounded-xl bg-black/30 border border-white/[0.08] text-xs font-mono text-[var(--text-main)] focus:outline-none focus:border-sky-400/50"
            >
              <option value="ALL">All Dimensions</option>
              {(['SECURITY', 'COMPLEXITY', 'DUPLICATION', 'MAINTAINABILITY'] as DimensionType[]).map(
                (d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                )
              )}
            </select>
          </div>
        </div>

        {/* Quick Filter Counts */}
        <div className="flex items-center justify-between text-xs font-mono text-slate-500 pt-1 border-t border-white/[0.04]">
          <span>Showing {filteredFindings.length} of {MOCK_FINDINGS.length} findings</span>
          {(selectedSeverity !== 'ALL' || selectedDimension !== 'ALL' || searchQuery) && (
            <button
              type="button"
              onClick={() => {
                setSelectedSeverity('ALL');
                setSelectedDimension('ALL');
                setSearchQuery('');
              }}
              className="text-sky-400 hover:underline cursor-pointer"
            >
              Reset Filters
            </button>
          )}
        </div>
      </div>

      {/* Findings List */}
      <div className="space-y-4">
        {filteredFindings.map((finding) => {
          const isExpanded = expandedFindingId === finding.findingId;
          const feedback = feedbackState[finding.findingId];

          // Look up matching mock explanation
          const explanation = MOCK_EXPLANATIONS.find(
            (e) => e.findingId === finding.findingId
          );

          return (
            <div
              key={finding.findingId}
              className={`rounded-2xl glass-surface p-6 space-y-4 border transition-all ${
                highlightedFindingId === finding.findingId
                  ? 'border-sky-400/60 shadow-[0_0_25px_-5px_rgba(56,189,248,0.25)]'
                  : 'border-white/[0.06] hover:border-sky-400/30'
              }`}
            >
              {/* Finding Header */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div className="flex flex-wrap items-center gap-2">
                  <SeverityBadge severity={finding.severity} />
                  <span className="font-mono text-xs font-bold text-sky-400">
                    [{finding.ruleCode}]
                  </span>
                  {finding.cweId && (
                    <a
                      href={`https://cwe.mitre.org/data/definitions/${finding.cweId}.html`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-[11px] font-mono text-slate-400 hover:text-sky-400 bg-white/[0.04] px-2 py-0.5 rounded border border-white/[0.06] transition-colors"
                      title="View MITRE CWE Definition"
                    >
                      <span>CWE-{finding.cweId}</span>
                      <ExternalLink className="h-3 w-3" />
                    </a>
                  )}
                  <span className="text-xs text-slate-500 font-mono">
                    Lines {finding.startLine}–{finding.endLine}
                  </span>
                  <span className="text-[10px] font-mono text-slate-500 uppercase px-2 py-0.5 rounded bg-white/[0.02]">
                    {finding.dimension}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <span
                    className={`text-xs font-mono font-bold ${
                      (finding.scoreDeduction ?? 0) > 0 ? 'text-rose-400' : 'text-slate-400'
                    }`}
                  >
                    {(finding.scoreDeduction ?? 0) > 0
                      ? `-${finding.scoreDeduction} pts impact`
                      : '0 pts impact'}
                  </span>
                </div>
              </div>

              {/* Message */}
              <p className="text-xs sm:text-sm text-[var(--text-main)] font-medium leading-relaxed">
                {finding.message}
              </p>

              {/* Code Snippet */}
              {finding.snippet && (
                <div className="p-3.5 rounded-xl bg-[#06080C] border border-white/[0.04] font-code text-xs text-slate-300 overflow-x-auto leading-5">
                  <div className="text-[10px] text-slate-500 select-none pb-1 font-mono">
                    Flagged Source Slice:
                  </div>
                  {finding.snippet}
                </div>
              )}

              {/* Bottom Action Ribbon: RAG Guidance Toggle & Triage Feedback */}
              <div className="pt-2 border-t border-white/[0.04] flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                {/* RAG Guidance Toggle & Deep Link to Report */}
                <div className="flex flex-wrap items-center gap-3">
                  {explanation ? (
                    <button
                      type="button"
                      onClick={() =>
                        setUserToggledId(isExpanded ? null : finding.findingId)
                      }
                      className="inline-flex items-center gap-2 text-xs font-mono text-sky-400 hover:text-sky-300 transition-colors cursor-pointer"
                    >
                      <BookOpen className="h-3.5 w-3.5" />
                      <span>
                        {isExpanded ? 'Hide Grounded Guidance' : 'View Grounded AI Explanation'}
                      </span>
                      {isExpanded ? (
                        <ChevronUp className="h-3.5 w-3.5" />
                      ) : (
                        <ChevronDown className="h-3.5 w-3.5" />
                      )}
                    </button>
                  ) : (
                    <span className="text-xs font-mono text-slate-500">
                      Rule guidance unavailable for this rule
                    </span>
                  )}

                  <Link
                    to={`/report?runId=${finding.runId}`}
                    className="inline-flex items-center gap-1 text-xs font-mono text-slate-400 hover:text-sky-300 transition-colors"
                  >
                    <span>View in Run Report</span>
                    <ArrowRight className="h-3 w-3" />
                  </Link>
                </div>

                {/* Precision Triage Feedback Buttons */}
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-mono text-slate-500">Label Precision:</span>
                  <button
                    type="button"
                    onClick={() => handleFeedback(finding.findingId, 'TRUE_POSITIVE')}
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono transition-all border ${
                      feedback === 'TRUE_POSITIVE'
                        ? 'bg-emerald-500/20 text-emerald-300 border-emerald-400/40'
                        : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05]'
                    }`}
                  >
                    <ThumbsUp className="h-3 w-3" />
                    <span>True Positive</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => handleFeedback(finding.findingId, 'FALSE_POSITIVE')}
                    className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-mono transition-all border ${
                      feedback === 'FALSE_POSITIVE'
                        ? 'bg-rose-500/20 text-rose-300 border-rose-400/40'
                        : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05]'
                    }`}
                  >
                    <ThumbsDown className="h-3 w-3" />
                    <span>False Positive</span>
                  </button>
                </div>
              </div>

              {/* Grounded Explanation Drawer (Bible Section 5 Component B) */}
              {isExpanded && explanation && (
                <div className="p-5 rounded-xl bg-white/[0.02] border border-sky-400/20 space-y-4 text-xs animate-in fade-in duration-200">
                  {/* Status Banner */}
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-[10px] uppercase tracking-wider text-sky-400 font-bold">
                      Retrieval-Augmented Explanation (Key-Lookup Only)
                    </span>
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold border ${
                        explanation.status === 'VALIDATED'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                          : 'bg-amber-500/10 text-amber-400 border-amber-500/30'
                      }`}
                    >
                      STATUS: {explanation.status}
                    </span>
                  </div>

                  {explanation.fallbackUsed && (
                    <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs flex items-start gap-2">
                      <ShieldAlert className="h-4 w-4 shrink-0 mt-0.5" />
                      <span>
                        <strong>Deterministic Fallback Active:</strong> AI explanation failed
                        validation checks. Curated rule text is displayed per Project Bible Section 5.B.
                      </span>
                    </div>
                  )}

                  {/* Summary */}
                  <div className="space-y-1">
                    <div className="font-bold text-slate-300">Summary:</div>
                    <p className="text-slate-400 leading-relaxed">{explanation.summary}</p>
                  </div>

                  {/* Why it matters */}
                  <div className="space-y-1">
                    <div className="font-bold text-slate-300">Why It Matters:</div>
                    <p className="text-slate-400 leading-relaxed">{explanation.whyItMatters}</p>
                  </div>

                  {/* How to fix */}
                  <div className="space-y-1">
                    <div className="font-bold text-slate-300">How to Remediate:</div>
                    <p className="text-slate-400 leading-relaxed">{explanation.howToFix}</p>
                  </div>

                  {/* Citations */}
                  {explanation.sources.length > 0 && (
                    <div className="space-y-1.5 pt-2 border-t border-white/[0.04]">
                      <div className="text-[10px] font-mono text-slate-500 uppercase tracking-wider">
                        Curated Knowledge Base Citations:
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {explanation.sources.map((src) => (
                          <a
                            key={src.chunkId}
                            href={src.url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-black/40 text-sky-400 hover:text-sky-300 border border-white/[0.06] text-xs font-mono transition-colors"
                          >
                            <span>{src.title}</span>
                            <ExternalLink className="h-3 w-3" />
                          </a>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};
