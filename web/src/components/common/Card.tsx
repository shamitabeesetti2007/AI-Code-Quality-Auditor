import React from 'react';

interface CardProps {
  children: React.ReactNode;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  action?: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
  elevated?: boolean;
  glow?: boolean;
}

export const Card: React.FC<CardProps> = ({
  children,
  title,
  subtitle,
  action,
  footer,
  className = '',
  elevated = false,
  glow = false,
}) => {
  const surfaceStyle = elevated ? 'glass-surface-elevated' : 'glass-surface';
  const glowStyle = glow ? 'glow-cyan-md' : '';

  return (
    <div
      className={`rounded-2xl ${surfaceStyle} ${glowStyle} p-6 transition-all duration-300 ${className}`}
    >
      {(title || action) && (
        <div className="flex items-start justify-between gap-4 mb-5 pb-4 border-b border-white/[0.06] dark:border-white/[0.06]">
          <div>
            {typeof title === 'string' ? (
              <h3 className="text-base font-bold text-[var(--text-main)] tracking-tight">
                {title}
              </h3>
            ) : (
              title
            )}
            {subtitle && (
              <p className="mt-1 text-xs text-[var(--text-muted)] leading-relaxed">
                {subtitle}
              </p>
            )}
          </div>
          {action && <div className="shrink-0">{action}</div>}
        </div>
      )}

      <div>{children}</div>

      {footer && (
        <div className="mt-5 pt-4 border-t border-white/[0.06] dark:border-white/[0.06] text-xs text-[var(--text-muted)]">
          {footer}
        </div>
      )}
    </div>
  );
};
