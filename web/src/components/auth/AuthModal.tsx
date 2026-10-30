import React, { useState, useEffect, useRef } from 'react';
import {
  X,
  Mail,
  Lock,
  User,
  AlertCircle,
  CheckCircle2,
  Sparkles,
} from 'lucide-react';
import { Button } from '../common/Button';
import { useAuth } from '../../hooks/useAuth';
import { ApiClientError } from '../../api';

export interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialMode?: 'login' | 'register';
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  initialMode = 'login',
}) => {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>(initialMode);

  // Form fields
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');

  // Status & error state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const modalRef = useRef<HTMLDivElement>(null);

  // Sync mode and reset errors when modal opens (render-phase adjustment without cascading effect)
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  if (isOpen !== prevIsOpen) {
    setPrevIsOpen(isOpen);
    if (isOpen) {
      setMode(initialMode);
      setErrorMessage(null);
      setSuccessMessage(null);
    }
  }

  // Handle escape key and body scroll lock
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleModeChange = (newMode: 'login' | 'register') => {
    setMode(newMode);
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSuccessMessage(null);

    const trimmedEmail = email.trim();
    if (!trimmedEmail) {
      setErrorMessage('Please enter a valid email address.');
      return;
    }

    if (!password) {
      setErrorMessage('Please enter your password.');
      return;
    }

    if (mode === 'register') {
      const trimmedName = displayName.trim();
      if (!trimmedName) {
        setErrorMessage('Please enter your full or display name.');
        return;
      }
      if (password.length < 10) {
        setErrorMessage('Password must be at least 10 characters in length.');
        return;
      }

      setIsSubmitting(true);
      try {
        await register({
          email: trimmedEmail,
          password,
          displayName: trimmedName,
        });

        // Automatically sign in upon successful registration
        try {
          await login({
            email: trimmedEmail,
            password,
          });
          onClose();
        } catch {
          // If auto-login fails, inform user to sign in
          setSuccessMessage('Account created successfully! Please sign in.');
          setMode('login');
        }
      } catch (err) {
        if (err instanceof ApiClientError) {
          if (err.status === 409) {
            setErrorMessage('An account with this email address already exists.');
          } else if (err.status === 400 && err.problem?.detail) {
            setErrorMessage(err.problem.detail);
          } else {
            setErrorMessage(err.message || 'Registration failed. Please try again.');
          }
        } else {
          setErrorMessage('Network connection error. Please try again.');
        }
      } finally {
        setIsSubmitting(false);
      }
    } else {
      // Login mode
      setIsSubmitting(true);
      try {
        await login({
          email: trimmedEmail,
          password,
        });
        onClose();
      } catch (err) {
        if (err instanceof ApiClientError) {
          if (err.status === 401) {
            setErrorMessage('Invalid email or password. Please verify your credentials.');
          } else if (err.status === 429) {
            setErrorMessage('Too many failed login attempts. Please wait before retrying.');
          } else {
            setErrorMessage(err.message || 'Login failed. Please try again.');
          }
        } else {
          setErrorMessage('Network connection error. Please try again.');
        }
      } finally {
        setIsSubmitting(false);
      }
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md animate-in fade-in duration-200"
      onClick={(e) => {
        if (modalRef.current && !modalRef.current.contains(e.target as Node)) {
          onClose();
        }
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="auth-modal-title"
    >
      <div
        ref={modalRef}
        className="w-full max-w-md rounded-2xl bg-[var(--bg-surface)] border border-[var(--border-subtle)] shadow-[0_25px_60px_-15px_rgba(0,0,0,0.6),0_0_30px_-5px_rgba(56,189,248,0.2)] backdrop-blur-2xl p-6 sm:p-8 space-y-6 relative animate-in zoom-in-95 duration-200"
      >
        {/* Top Close Button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 p-1.5 rounded-xl text-slate-400 hover:text-[var(--text-main)] hover:bg-white/[0.06] transition-colors cursor-pointer"
          aria-label="Close authentication dialog"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Modal Header */}
        <div className="space-y-1 text-center sm:text-left pr-8">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium text-sky-700 dark:text-sky-400 bg-sky-500/10 border border-sky-400/20 mb-1">
            <Sparkles className="h-3 w-3 text-amber-500 dark:text-amber-300" />
            <span>AI CODE QUALITY AUDITOR</span>
          </div>
          <h2
            id="auth-modal-title"
            className="font-heading text-xl sm:text-2xl font-bold text-[var(--text-main)]"
          >
            {mode === 'login' ? 'Sign in to your account' : 'Create an auditor account'}
          </h2>
          <p className="text-xs text-[var(--text-muted)] leading-relaxed">
            {mode === 'login'
              ? 'Access verified Java code reviews, triage findings, and empirical research.'
              : 'Join the platform to conduct deterministic analysis and verified remediations.'}
          </p>
        </div>

        {/* Tab Toggle */}
        <div className="flex rounded-xl bg-[var(--bg-surface-elevated)] p-1 border border-[var(--border-subtle)]">
          <button
            type="button"
            onClick={() => handleModeChange('login')}
            className={`flex-1 py-1.5 text-xs font-mono font-medium rounded-lg transition-all cursor-pointer ${
              mode === 'login'
                ? 'bg-sky-500/20 text-sky-700 dark:text-sky-300 shadow-sm border border-sky-500/30 font-semibold'
                : 'text-slate-600 dark:text-slate-400 hover:text-[var(--text-main)]'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => handleModeChange('register')}
            className={`flex-1 py-1.5 text-xs font-mono font-medium rounded-lg transition-all cursor-pointer ${
              mode === 'register'
                ? 'bg-sky-500/20 text-sky-700 dark:text-sky-300 shadow-sm border border-sky-500/30 font-semibold'
                : 'text-slate-600 dark:text-slate-400 hover:text-[var(--text-main)]'
            }`}
          >
            Create Account
          </button>
        </div>

        {/* Status Alerts */}
        {errorMessage && (
          <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-700 dark:text-rose-300 text-xs flex items-start gap-2.5 animate-in fade-in duration-150">
            <AlertCircle className="h-4 w-4 shrink-0 text-rose-500 mt-0.5" />
            <span className="leading-snug">{errorMessage}</span>
          </div>
        )}

        {successMessage && (
          <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-700 dark:text-emerald-300 text-xs flex items-start gap-2.5 animate-in fade-in duration-150">
            <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-500 mt-0.5" />
            <span className="leading-snug">{successMessage}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {mode === 'register' && (
            <div className="space-y-1.5">
              <label
                htmlFor="auth-display-name"
                className="text-xs font-medium text-[var(--text-main)] flex items-center justify-between"
              >
                <span>Full Name / Display Name</span>
                <span className="text-[10px] text-[var(--text-muted)] font-mono">Max 100</span>
              </label>
              <div className="relative">
                <User className="h-4 w-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  id="auth-display-name"
                  type="text"
                  required
                  maxLength={100}
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  placeholder="e.g. Divya Shree"
                  className="w-full pl-9 pr-3 py-2 rounded-xl bg-[var(--bg-surface-elevated)] border border-[var(--border-subtle)] text-xs text-[var(--text-main)] placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                  autoFocus
                />
              </div>
            </div>
          )}

          <div className="space-y-1.5">
            <label
              htmlFor="auth-email"
              className="text-xs font-medium text-[var(--text-main)]"
            >
              Email Address
            </label>
            <div className="relative">
              <Mail className="h-4 w-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                id="auth-email"
                type="email"
                required
                maxLength={254}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="user@example.com"
                className="w-full pl-9 pr-3 py-2 rounded-xl bg-[var(--bg-surface-elevated)] border border-[var(--border-subtle)] text-xs text-[var(--text-main)] placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
                autoFocus={mode === 'login'}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label
                htmlFor="auth-password"
                className="text-xs font-medium text-[var(--text-main)]"
              >
                Password
              </label>
              {mode === 'register' && (
                <span className="text-[10px] font-mono text-[var(--text-muted)]">
                  Min 10 characters
                </span>
              )}
            </div>
            <div className="relative">
              <Lock className="h-4 w-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                id="auth-password"
                type="password"
                required
                minLength={mode === 'register' ? 10 : undefined}
                maxLength={72}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={mode === 'register' ? 'At least 10 characters...' : 'Enter your password'}
                className="w-full pl-9 pr-3 py-2 rounded-xl bg-[var(--bg-surface-elevated)] border border-[var(--border-subtle)] text-xs text-[var(--text-main)] placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-sky-400 focus:border-transparent transition-all"
              />
            </div>
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="md"
              loading={isSubmitting}
              className="w-full justify-center text-xs font-semibold"
            >
              {mode === 'login' ? 'Sign In to Account' : 'Create Free Account'}
            </Button>
          </div>
        </form>

        {/* Modal Footer Security & Switch Prompt */}
        <div className="pt-2 border-t border-[var(--border-subtle)] text-center space-y-2">
          <p className="text-xs text-[var(--text-muted)]">
            {mode === 'login' ? "Don't have an account yet?" : 'Already have an auditor account?'}{' '}
            <button
              type="button"
              onClick={() => handleModeChange(mode === 'login' ? 'register' : 'login')}
              className="font-semibold text-sky-600 dark:text-sky-400 hover:underline cursor-pointer"
            >
              {mode === 'login' ? 'Create an account' : 'Sign in here'}
            </button>
          </p>
          <p className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">
            Protected by Spring Security • JWT &amp; HttpOnly Cookie Authentication
          </p>
        </div>
      </div>
    </div>
  );
};
