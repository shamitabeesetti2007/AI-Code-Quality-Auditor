import React from 'react';
import { Loader2 } from 'lucide-react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  icon?: React.ReactNode;
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  icon,
  className = '',
  ...props
}) => {
  const baseStyles =
    'inline-flex items-center justify-center font-medium transition-all duration-200 select-none cursor-pointer disabled:cursor-not-allowed disabled:opacity-40 focus-visible:outline-2 focus-visible:outline-offset-2';

  const sizeStyles = {
    sm: 'text-xs px-3 py-1.5 rounded-lg gap-1.5',
    md: 'text-sm px-4 py-2 rounded-xl gap-2',
    lg: 'text-base px-6 py-2.5 rounded-xl gap-2.5 font-semibold',
  }[size];

  const variantStyles = {
    primary:
      'bg-sky-400 hover:bg-sky-300 text-slate-950 font-semibold shadow-[0_0_25px_-5px_rgba(56,189,248,0.4)] hover:shadow-[0_0_35px_-5px_rgba(56,189,248,0.6)] active:translate-y-px focus-visible:outline-sky-400',
    secondary:
      'bg-white/[0.04] dark:bg-white/[0.04] text-[var(--text-main)] border border-[var(--border-subtle)] hover:bg-white/[0.08] hover:border-[var(--border-strong)] active:translate-y-px focus-visible:outline-sky-400 backdrop-blur-md',
    outline:
      'bg-transparent text-sky-400 border border-sky-400/30 hover:border-sky-400 hover:bg-sky-400/10 active:translate-y-px focus-visible:outline-sky-400',
    ghost:
      'bg-transparent text-[var(--text-body)] hover:text-[var(--text-main)] hover:bg-white/[0.05] focus-visible:outline-sky-400',
    danger:
      'bg-rose-600/90 hover:bg-rose-600 text-white shadow-[0_0_20px_-5px_rgba(244,63,94,0.4)] active:translate-y-px focus-visible:outline-rose-500',
  }[variant];

  return (
    <button
      disabled={disabled || loading}
      className={`${baseStyles} ${sizeStyles} ${variantStyles} ${className}`}
      {...props}
    >
      {loading ? (
        <Loader2 className="h-4 w-4 animate-spin text-current" />
      ) : icon ? (
        <span className="shrink-0">{icon}</span>
      ) : null}
      <span>{children}</span>
    </button>
  );
};
