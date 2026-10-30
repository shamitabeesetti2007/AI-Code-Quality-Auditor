import React from 'react';
import {
  User,
  Bot,
  Sparkles,
  ShieldCheck,
  AlertOctagon,
  AlertTriangle,
  AlertCircle,
  Info,
  CheckCircle2,
  XCircle,
} from 'lucide-react';

import type { OriginType } from '../../types/submission';
import type { FindingSeverity } from '../../types/finding';
import type { QualityGrade } from '../../types/analysis';
import type { RemediationOutcome } from '../../types/remediation';

export type ProvenanceType = OriginType;
export type SeverityType = FindingSeverity;
export type { QualityGrade };
export type OutcomeType =
  | RemediationOutcome
  | 'VERIFIED_CLEAN'
  | 'VERIFIED_WITH_WARNINGS'
  | 'PARTIAL_FIX'
  | 'REGRESSED';

interface ProvenanceBadgeProps {
  type: ProvenanceType;
  showIcon?: boolean;
  className?: string;
}

export const ProvenanceBadge: React.FC<ProvenanceBadgeProps> = ({
  type,
  showIcon = true,
  className = '',
}) => {
  const configs = {
    HUMAN: {
      label: 'Human Written',
      icon: <User className="h-3 w-3 text-slate-500 dark:text-slate-400" />,
      style:
        'bg-slate-500/10 text-slate-700 border-slate-400/30 dark:bg-slate-800/40 dark:text-slate-300 dark:border-white/10',
    },
    AI_GENERATED: {
      label: 'AI Generated',
      icon: <Bot className="h-3 w-3 text-purple-600 dark:text-purple-400" />,
      style:
        'bg-purple-500/10 text-purple-800 border-purple-400/30 dark:bg-purple-950/40 dark:text-purple-300 dark:border-purple-500/30 shadow-[0_0_15px_-5px_rgba(168,85,247,0.2)]',
    },
    AI_ASSISTED: {
      label: 'AI Assisted',
      icon: <Sparkles className="h-3 w-3 text-cyan-600 dark:text-cyan-400" />,
      style:
        'bg-cyan-500/10 text-cyan-800 border-cyan-400/30 dark:bg-cyan-950/40 dark:text-cyan-300 dark:border-cyan-500/30 shadow-[0_0_15px_-5px_rgba(6,182,212,0.2)]',
    },
    AI_REMEDIATED: {
      label: 'AI Remediated',
      icon: <ShieldCheck className="h-3 w-3 text-emerald-600 dark:text-emerald-400" />,
      style:
        'bg-emerald-500/10 text-emerald-800 border-emerald-400/30 dark:bg-emerald-950/40 dark:text-emerald-300 dark:border-emerald-500/30 shadow-[0_0_15px_-5px_rgba(16,185,129,0.2)]',
    },
  }[type];

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-mono font-medium border backdrop-blur-md ${configs.style} ${className}`}
      title={`Provenance: ${configs.label}`}
    >
      {showIcon && configs.icon}
      <span>{configs.label}</span>
    </span>
  );
};

interface SeverityBadgeProps {
  severity: SeverityType;
  showIcon?: boolean;
  className?: string;
}

export const SeverityBadge: React.FC<SeverityBadgeProps> = ({
  severity,
  showIcon = true,
  className = '',
}) => {
  const configs = {
    CRITICAL: {
      label: 'Critical',
      icon: <AlertOctagon className="h-3 w-3" />,
      style:
        'bg-[var(--severity-critical-bg)] text-[var(--severity-critical)] border-[var(--severity-critical-border)]',
    },
    HIGH: {
      label: 'High',
      icon: <AlertTriangle className="h-3 w-3" />,
      style:
        'bg-[var(--severity-high-bg)] text-[var(--severity-high)] border-[var(--severity-high-border)]',
    },
    MEDIUM: {
      label: 'Medium',
      icon: <AlertCircle className="h-3 w-3" />,
      style:
        'bg-[var(--severity-medium-bg)] text-[var(--severity-medium)] border-[var(--severity-medium-border)]',
    },
    LOW: {
      label: 'Low',
      icon: <Info className="h-3 w-3" />,
      style:
        'bg-[var(--severity-low-bg)] text-[var(--severity-low)] border-[var(--severity-low-border)]',
    },
    INFO: {
      label: 'Info',
      icon: <Info className="h-3 w-3" />,
      style:
        'bg-[var(--severity-info-bg)] text-[var(--severity-info)] border-[var(--severity-info-border)]',
    },
  }[severity];

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-md text-xs font-mono font-semibold border backdrop-blur-md ${configs.style} ${className}`}
    >
      {showIcon && configs.icon}
      <span>{configs.label}</span>
    </span>
  );
};

interface GradeBadgeProps {
  grade: QualityGrade;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export const GradeBadge: React.FC<GradeBadgeProps> = ({
  grade,
  size = 'md',
  className = '',
}) => {
  const gradeStyles = {
    A: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/30 shadow-[0_0_20px_-5px_rgba(52,211,153,0.3)]',
    B: 'bg-sky-500/10 text-sky-700 dark:text-sky-400 border-sky-500/30 shadow-[0_0_20px_-5px_rgba(56,189,248,0.3)]',
    C: 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/30 shadow-[0_0_20px_-5px_rgba(251,191,36,0.3)]',
    D: 'bg-orange-500/10 text-orange-700 dark:text-orange-400 border-orange-500/30 shadow-[0_0_20px_-5px_rgba(249,115,22,0.3)]',
    F: 'bg-rose-500/10 text-rose-700 dark:text-rose-400 border-rose-500/30 shadow-[0_0_20px_-5px_rgba(244,63,94,0.3)]',
    'N/A': 'bg-slate-500/10 text-slate-700 dark:text-slate-400 border-slate-500/30',
  }[grade];

  const sizeStyles = {
    sm: 'w-6 h-6 text-xs font-bold rounded-lg',
    md: 'w-8 h-8 text-sm font-black rounded-xl',
    lg: 'w-16 h-16 text-3xl font-black rounded-2xl',
  }[size];

  return (
    <span
      className={`inline-flex items-center justify-center border font-mono select-none backdrop-blur-md ${gradeStyles} ${sizeStyles} ${className}`}
      title={`Overall Quality Grade: ${grade}`}
    >
      {grade}
    </span>
  );
};

interface OutcomeBadgeProps {
  outcome: OutcomeType;
  className?: string;
}

export const OutcomeBadge: React.FC<OutcomeBadgeProps> = ({
  outcome,
  className = '',
}) => {
  const configs: Record<OutcomeType, { label: string; icon: React.ReactNode; style: string }> = {
    VERIFIED_FIX: {
      label: 'VERIFIED_FIX',
      icon: <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />,
      style:
        'bg-emerald-500/15 text-emerald-800 dark:text-emerald-300 border-emerald-500/30 shadow-[0_0_20px_-5px_rgba(16,185,129,0.25)]',
    },
    VERIFIED_CLEAN: {
      label: 'VERIFIED_FIX',
      icon: <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400" />,
      style:
        'bg-emerald-500/15 text-emerald-800 dark:text-emerald-300 border-emerald-500/30 shadow-[0_0_20px_-5px_rgba(16,185,129,0.25)]',
    },
    VERIFIED_WITH_WARNINGS: {
      label: 'PARTIAL',
      icon: <AlertCircle className="h-3.5 w-3.5 text-teal-600 dark:text-teal-400" />,
      style:
        'bg-teal-500/15 text-teal-800 dark:text-teal-300 border-teal-500/30 shadow-[0_0_20px_-5px_rgba(20,184,166,0.25)]',
    },
    PARTIAL: {
      label: 'PARTIAL',
      icon: <AlertTriangle className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400" />,
      style:
        'bg-amber-500/15 text-amber-800 dark:text-amber-300 border-amber-500/30 shadow-[0_0_20px_-5px_rgba(245,158,11,0.25)]',
    },
    PARTIAL_FIX: {
      label: 'PARTIAL',
      icon: <AlertTriangle className="h-3.5 w-3.5 text-amber-600 dark:text-amber-400" />,
      style:
        'bg-amber-500/15 text-amber-800 dark:text-amber-300 border-amber-500/30 shadow-[0_0_20px_-5px_rgba(245,158,11,0.25)]',
    },
    NO_EFFECT: {
      label: 'NO_EFFECT',
      icon: <AlertCircle className="h-3.5 w-3.5 text-slate-600 dark:text-slate-400" />,
      style:
        'bg-slate-500/15 text-slate-800 dark:text-slate-300 border-slate-500/30 shadow-[0_0_20px_-5px_rgba(148,163,184,0.25)]',
    },
    REGRESSION: {
      label: 'REGRESSION',
      icon: <XCircle className="h-3.5 w-3.5 text-rose-600 dark:text-rose-400" />,
      style:
        'bg-rose-500/15 text-rose-800 dark:text-rose-300 border-rose-500/30 shadow-[0_0_20px_-5px_rgba(244,63,94,0.25)]',
    },
    REGRESSED: {
      label: 'REGRESSION',
      icon: <XCircle className="h-3.5 w-3.5 text-rose-600 dark:text-rose-400" />,
      style:
        'bg-rose-500/15 text-rose-800 dark:text-rose-300 border-rose-500/30 shadow-[0_0_20px_-5px_rgba(244,63,94,0.25)]',
    },
    BROKEN: {
      label: 'BROKEN',
      icon: <AlertOctagon className="h-3.5 w-3.5 text-red-600 dark:text-red-400" />,
      style:
        'bg-red-500/15 text-red-800 dark:text-red-300 border-red-500/30 shadow-[0_0_20px_-5px_rgba(239,68,68,0.25)]',
    },
  };

  const selectedConfig = configs[outcome] || configs.BROKEN;

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-lg text-xs font-mono font-semibold border backdrop-blur-md ${selectedConfig.style} ${className}`}
    >
      {selectedConfig.icon}
      <span>{selectedConfig.label}</span>
    </span>
  );
};
