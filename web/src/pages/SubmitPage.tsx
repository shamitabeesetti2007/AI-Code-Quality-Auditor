import React, { useState, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Code2,
  ArrowLeft,
  ShieldCheck,
  AlertTriangle,
  CheckCircle2,
  Sparkles,
  Info,
  Terminal,
} from 'lucide-react';
import { Button } from '../components/common/Button';
import {
  SAMPLE_VULNERABLE_LOGIN_DAO,
  SAMPLE_COMPLEX_SERVICE,
  SAMPLE_CLEAN_TOKEN_CACHE,
  SAMPLE_SYNTAX_ERROR_SNIPPET,
  SAMPLE_NON_JAVA_SNIPPET,
} from '../fixtures/sampleJavaSnippets';
import { checkLanguageGate, calculateLogicalLoc } from '../utils/codeUtils';
import type { OriginType } from '../types/submission';

export const SubmitPage: React.FC = () => {
  const navigate = useNavigate();

  // Code state initialized to the canonical vulnerable DAO fixture
  const [sourceCode, setSourceCode] = useState<string>(SAMPLE_VULNERABLE_LOGIN_DAO);
  const [title, setTitle] = useState<string>('LoginDao.java');
  const [originType, setOriginType] = useState<OriginType>('HUMAN');
  const [activeSnippetKey, setActiveSnippetKey] = useState<string>('login-dao');

  // Live metrics computed purely on client string (never executes code)
  const logicalLoc = useMemo(() => calculateLogicalLoc(sourceCode), [sourceCode]);
  const languageGate = useMemo(() => checkLanguageGate(sourceCode), [sourceCode]);
  const charCount = sourceCode.length;
  const maxChars = 100000;
  const isOverLimit = charCount > maxChars;

  // Map active snippet to its corresponding pre-computed mock run ID
  const targetRunId = useMemo(() => {
    switch (activeSnippetKey) {
      case 'order-service':
        return 'run-88ae-002';
      case 'token-cache':
        return 'run-11ff-003';
      case 'syntax-error':
        return 'run-e32a-005';
      case 'login-dao':
      default:
        return 'run-c7a2-001';
    }
  }, [activeSnippetKey]);

  const loadSnippet = (
    key: string,
    snippet: string,
    snippetTitle: string,
    defaultOrigin: OriginType
  ) => {
    setActiveSnippetKey(key);
    setSourceCode(snippet);
    setTitle(snippetTitle);
    setOriginType(defaultOrigin);
  };

  const lineCount = useMemo(() => {
    const lines = sourceCode.split('\n');
    return Math.max(lines.length, 1);
  }, [sourceCode]);

  const handleAnalyze = () => {
    if (!languageGate.isJava) return;
    // Navigate to simulated report for the selected fixture
    navigate(`/report?runId=${targetRunId}`);
  };

  return (
    <div className="space-y-8 max-w-5xl mx-auto pb-16">
      {/* Top Breadcrumb & Simulation Pill */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <Link to="/">
          <Button variant="ghost" size="sm" icon={<ArrowLeft className="h-4 w-4" />}>
            Back to Dashboard
          </Button>
        </Link>
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono font-medium text-amber-400 bg-amber-500/10 border border-amber-400/20">
          <Info className="h-3.5 w-3.5" />
          <span>SIMULATION MODE • PRE-COMPUTED FIXTURES</span>
        </div>
      </div>

      {/* Page Title & Context */}
      <div className="space-y-2">
        <h1 className="font-heading text-2xl sm:text-3xl font-extrabold text-[var(--text-main)] tracking-tight">
          Submit Java Code for Audit
        </h1>
        <p className="text-xs text-[var(--text-muted)] max-w-3xl leading-relaxed">
          Statically parses Java source into an Abstract Syntax Tree (AST) to evaluate Security,
          Complexity, Duplication, and Maintainability. Code is strictly analyzed statically and is{' '}
          <strong className="text-slate-900 dark:text-slate-200">never executed</strong>.
        </p>
      </div>

      {/* Sample Fixture Quick Selectors */}
      <div className="rounded-2xl glass-surface p-5 space-y-3">
        <div className="flex items-center gap-2 text-xs font-mono font-semibold text-sky-400 uppercase tracking-wider">
          <Sparkles className="h-3.5 w-3.5 text-amber-300" />
          <span>Load Realistic Project Bible Test Fixtures</span>
        </div>
        <div className="flex flex-wrap gap-2 pt-1">
          <button
            type="button"
            onClick={() =>
              loadSnippet('login-dao', SAMPLE_VULNERABLE_LOGIN_DAO, 'LoginDao.java', 'HUMAN')
            }
            className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all border ${
              activeSnippetKey === 'login-dao'
                ? 'bg-sky-500/20 text-sky-300 border-sky-400/40 shadow-[0_0_15px_-3px_rgba(56,189,248,0.3)]'
                : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05] hover:text-white'
            }`}
          >
            Vulnerable LoginDao (SEC-001)
          </button>

          <button
            type="button"
            onClick={() =>
              loadSnippet(
                'order-service',
                SAMPLE_COMPLEX_SERVICE,
                'OrderProcessingService.java',
                'AI_GENERATED'
              )
            }
            className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all border ${
              activeSnippetKey === 'order-service'
                ? 'bg-sky-500/20 text-sky-300 border-sky-400/40 shadow-[0_0_15px_-3px_rgba(56,189,248,0.3)]'
                : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05] hover:text-white'
            }`}
          >
            Complexity Hotspot (COMP-001)
          </button>

          <button
            type="button"
            onClick={() =>
              loadSnippet('token-cache', SAMPLE_CLEAN_TOKEN_CACHE, 'TokenCache.java', 'HUMAN')
            }
            className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all border ${
              activeSnippetKey === 'token-cache'
                ? 'bg-sky-500/20 text-sky-300 border-sky-400/40 shadow-[0_0_15px_-3px_rgba(56,189,248,0.3)]'
                : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05] hover:text-white'
            }`}
          >
            Clean Baseline (Grade A)
          </button>

          <button
            type="button"
            onClick={() =>
              loadSnippet(
                'syntax-error',
                SAMPLE_SYNTAX_ERROR_SNIPPET,
                'MalformedOrder.java',
                'HUMAN'
              )
            }
            className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all border ${
              activeSnippetKey === 'syntax-error'
                ? 'bg-sky-500/20 text-sky-300 border-sky-400/40 shadow-[0_0_15px_-3px_rgba(56,189,248,0.3)]'
                : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05] hover:text-white'
            }`}
          >
            Syntax Error (Grade N/A)
          </button>

          <button
            type="button"
            onClick={() =>
              loadSnippet(
                'non-java',
                SAMPLE_NON_JAVA_SNIPPET,
                'AuthHandler.py (Python)',
                'HUMAN'
              )
            }
            className={`px-3 py-1.5 rounded-lg text-xs font-mono transition-all border ${
              activeSnippetKey === 'non-java'
                ? 'bg-rose-500/20 text-rose-300 border-rose-400/40 shadow-[0_0_15px_-3px_rgba(244,63,94,0.3)]'
                : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05] hover:text-white'
            }`}
          >
            Non-Java Input (Rejection Test)
          </button>

          <button
            type="button"
            onClick={() => {
              setActiveSnippetKey('custom');
              setSourceCode('');
              setTitle('');
            }}
            className="px-3 py-1.5 rounded-lg text-xs font-mono text-slate-400 hover:text-rose-400 bg-white/[0.02] hover:bg-white/[0.05] border border-white/[0.06] transition-all ml-auto"
          >
            Clear Editor
          </button>
        </div>
      </div>

      {/* Submission Metadata Controls */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Title Field */}
        <div className="space-y-1.5">
          <label className="text-xs font-mono uppercase tracking-wider text-slate-400 block">
            File / Unit Name (Optional)
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. AccountRepository.java"
            className="w-full px-3.5 py-2.5 rounded-xl bg-black/30 border border-white/[0.08] text-xs font-mono text-[var(--text-main)] placeholder-slate-600 focus:outline-none focus:border-sky-400/50"
          />
        </div>

        {/* Provenance Origin Selector */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between">
            <label className="text-xs font-mono uppercase tracking-wider text-slate-400 block">
              Code Provenance Lineage
            </label>
            <span className="text-[10px] font-mono text-slate-500">
              Bible Constraint: Client-Set
            </span>
          </div>
          <div className="grid grid-cols-3 gap-2">
            {(['HUMAN', 'AI_GENERATED', 'AI_ASSISTED'] as const).map((origin) => (
              <button
                key={origin}
                type="button"
                onClick={() => setOriginType(origin)}
                className={`py-2 px-2.5 rounded-xl text-[11px] font-mono font-medium border text-center transition-all ${
                  originType === origin
                    ? 'bg-sky-500/15 text-sky-300 border-sky-400/40 shadow-[0_0_15px_-3px_rgba(56,189,248,0.25)]'
                    : 'bg-white/[0.02] text-slate-400 border-white/[0.06] hover:bg-white/[0.05]'
                }`}
              >
                {origin}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Lightweight Line-Numbered Editor Container */}
      <div className="rounded-2xl glass-surface overflow-hidden border border-white/[0.08] shadow-2xl">
        {/* Editor Ribbon */}
        <div className="px-4 py-2.5 bg-white/[0.02] border-b border-white/[0.06] flex items-center justify-between text-xs font-mono">
          <div className="flex items-center gap-2 text-slate-400">
            <Terminal className="h-4 w-4 text-sky-400" />
            <span className="font-semibold text-slate-300">{title || 'Untitled.java'}</span>
          </div>
          <div className="flex items-center gap-4 text-[11px]">
            <span className="text-slate-400">
              Logical LOC:{' '}
              <strong className="text-sky-300 font-bold">{logicalLoc}</strong>
            </span>
            <span
              className={`${
                isOverLimit ? 'text-rose-400 font-bold' : 'text-slate-500'
              }`}
            >
              {charCount.toLocaleString()} / {maxChars.toLocaleString()} chars
            </span>
          </div>
        </div>

        {/* Editor Body with Line-Number Gutter */}
        <div className="relative flex bg-[#06080C] min-h-[380px] max-h-[550px] overflow-auto font-code text-xs">
          {/* Gutter */}
          <div className="select-none py-4 px-3 bg-white/[0.01] border-r border-white/[0.04] text-slate-600 text-right font-mono min-w-[48px] shrink-0 leading-6">
            {Array.from({ length: lineCount }, (_, i) => (
              <div key={i + 1}>{i + 1}</div>
            ))}
          </div>

          {/* Textarea */}
          <textarea
            value={sourceCode}
            onChange={(e) => {
              setSourceCode(e.target.value);
              setActiveSnippetKey('custom');
            }}
            placeholder="// Paste Java source code here... (must contain class, interface, enum, or record)"
            spellCheck={false}
            className="w-full h-full p-4 bg-transparent text-slate-200 resize-none focus:outline-none font-code text-xs leading-6 selection:bg-sky-500/30 whitespace-pre"
            style={{ tabSize: 4 }}
          />
        </div>
      </div>

      {/* Language Gate Result Card */}
      <div
        className={`p-4 rounded-xl border backdrop-blur-md flex items-start gap-3 transition-colors ${
          languageGate.isJava
            ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
            : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
        }`}
      >
        {languageGate.isJava ? (
          <CheckCircle2 className="h-5 w-5 text-emerald-400 shrink-0 mt-0.5" />
        ) : (
          <AlertTriangle className="h-5 w-5 text-rose-400 shrink-0 mt-0.5" />
        )}
        <div className="space-y-1">
          <div className="font-mono text-xs font-bold">
            {languageGate.isJava
              ? 'Language Gate Passed'
              : 'Language Gate: Non-Java Input Detected'}
          </div>
          <p className="text-xs text-slate-300 leading-normal">
            {languageGate.message}
          </p>
          {!languageGate.isJava && (
            <p className="text-[11px] text-slate-400 font-mono pt-1">
              Note: When integrated with the backend API, non-Java input will return HTTP 422 Unprocessable Content.
            </p>
          )}
        </div>
      </div>

      {/* Analyze Action Area */}
      <div className="rounded-2xl glass-surface p-6 flex flex-col sm:flex-row items-center justify-between gap-4 border border-sky-400/20">
        <div className="space-y-1 text-center sm:text-left">
          <div className="flex items-center gap-2 justify-center sm:justify-start">
            <ShieldCheck className="h-4 w-4 text-sky-400" />
            <h3 className="font-heading font-bold text-sm text-[var(--text-main)]">
              {activeSnippetKey === 'custom'
                ? 'Preview Simulated Audit (Pre-Computed Fixture)'
                : 'Ready to Trigger AST Analysis Pipeline'}
            </h3>
          </div>
          <p className="text-xs text-[var(--text-muted)]">
            {activeSnippetKey === 'custom'
              ? 'In mock simulation mode without a live backend compiler, submitting code displays a representative pre-computed audit report.'
              : 'Runs Complexity, Duplication, Security, and Maintainability analyzers in parallel.'}
          </p>
        </div>

        <div className="flex items-center gap-3 w-full sm:w-auto">
          <Button
            variant="primary"
            size="lg"
            icon={<Code2 className="h-5 w-5" />}
            onClick={handleAnalyze}
            disabled={!languageGate.isJava || isOverLimit}
            className="w-full sm:w-auto"
          >
            {activeSnippetKey === 'custom'
              ? 'View Demo Audit Report'
              : 'Analyze Java Code'}
          </Button>
        </div>
      </div>
    </div>
  );
};
