import React, { useState, useRef, useEffect } from 'react';
import {
  Sun,
  Moon,
  PanelLeftOpen,
  Menu,
  ChevronDown,
  Mail,
  ShieldCheck,
  User as UserIcon,
  LogIn,
  LogOut,
} from 'lucide-react';
import { useTheme } from '../../hooks/useTheme';
import { useNavigation } from '../../hooks/useNavigation';
import { useAuth } from '../../hooks/useAuth';
import { checkBackendHealth } from '../../api';
import { AuthModal } from '../auth/AuthModal';
import logoSvg from '../../assets/logo.svg';

export const Navbar: React.FC = () => {
  const { theme, toggleTheme } = useTheme();
  const { sidebarState, showSidebar, mobileOpen, toggleMobileOpen } = useNavigation();
  const { user, isAuthenticated, isLoading: authLoading, logout } = useAuth();

  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [authModalMode, setAuthModalMode] = useState<'login' | 'register'>('login');
  const [backendConnected, setBackendConnected] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let mounted = true;
    checkBackendHealth()
      .then((res) => {
        if (mounted) {
          setBackendConnected(res.connected);
        }
      })
      .catch(() => {
        if (mounted) {
          setBackendConnected(false);
        }
      });
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!userMenuOpen) return;

    const handleOutsideClick = (event: MouseEvent | TouchEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target as Node)) {
        setUserMenuOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setUserMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    document.addEventListener('touchstart', handleOutsideClick);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      document.removeEventListener('touchstart', handleOutsideClick);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [userMenuOpen]);

  const userInitial = user
    ? (user.displayName || user.email || 'U').charAt(0).toUpperCase()
    : 'U';

  const userDisplayRole = user?.role || 'USER';

  return (
    <>
      <header className="sticky top-0 z-30 h-16 w-full border-b border-[var(--border-subtle)] bg-[var(--bg-page)]/80 backdrop-blur-xl transition-colors px-4 sm:px-6">
        <div className="flex h-full items-center justify-between gap-4">
          {/* Left: Reopen Trigger (when sidebar is hidden or on mobile) & Brand */}
          <div className="flex items-center gap-3">
            {/* Mobile Drawer Trigger */}
            <button
              type="button"
              onClick={toggleMobileOpen}
              className="md:hidden p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/[0.06] transition-colors cursor-pointer"
              aria-label="Open mobile navigation drawer"
              aria-expanded={mobileOpen}
            >
              <Menu className="h-5 w-5" />
            </button>

            {/* Desktop Reopen Sidebar Button (Visible only when hidden) */}
            {sidebarState === 'hidden' && (
              <button
                type="button"
                onClick={showSidebar}
                className="hidden md:flex items-center gap-2 px-2.5 py-1.5 rounded-xl text-xs font-mono text-sky-400 bg-sky-500/10 border border-sky-500/20 hover:bg-sky-500/20 transition-all cursor-pointer glow-cyan-sm"
                title="Expand left navigation sidebar"
                aria-label="Expand left navigation sidebar"
                aria-expanded="false"
                aria-controls="main-sidebar"
              >
                <PanelLeftOpen className="h-4 w-4" />
                <span>Sidebar</span>
              </button>
            )}

            <a href="/" className="flex items-center gap-2.5 group">
              <div className="relative">
                <img
                  src={logoSvg}
                  alt="AI Code Quality Auditor Logo"
                  className="h-8 w-8 transition-transform group-hover:scale-105"
                />
                <div className="absolute -inset-1 bg-sky-400/20 rounded-full blur-xs -z-10 opacity-0 group-hover:opacity-100 transition-opacity" />
              </div>
              <div>
                <span className="font-sans font-bold text-sm tracking-tight text-[var(--text-main)] block leading-none">
                  AI Code Quality Auditor
                </span>
                <span className="font-editorial italic text-xs text-[var(--text-muted)] tracking-wide">
                  Verified Remediation
                </span>
              </div>
            </a>
          </div>

          {/* Right: Discreet Status, Theme Toggle, & Authentication / Profile */}
          <div className="flex items-center gap-3">
            {/* Simplified, High-End Health/Simulation Indicator */}
            <div
              className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-mono text-[var(--text-muted)] bg-[var(--bg-surface-elevated)] border border-[var(--border-subtle)] backdrop-blur-md"
              title={
                backendConnected
                  ? 'Backend connected: Spring Boot /actuator/health is UP.'
                  : 'Backend disconnected: Operating in honest simulation mode with typed Bible fixtures.'
              }
            >
              <span
                className={`h-1.5 w-1.5 rounded-full ${
                  backendConnected
                    ? 'bg-emerald-500 animate-pulse'
                    : 'bg-amber-500 dark:bg-amber-400 animate-pulse'
                }`}
              />
              <span className="hidden sm:inline font-medium text-slate-700 dark:text-slate-300">
                {backendConnected ? 'Live Backend' : 'Simulation Mode'}
              </span>
            </div>

            {/* Theme Toggle Button */}
            <button
              type="button"
              onClick={toggleTheme}
              aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
              className="p-2 rounded-xl text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-200/50 dark:hover:bg-white/[0.05] border border-transparent hover:border-[var(--border-subtle)] transition-colors cursor-pointer"
            >
              {theme === 'dark' ? (
                <Sun className="h-4 w-4 text-amber-300" />
              ) : (
                <Moon className="h-4 w-4 text-slate-700" />
              )}
            </button>

            {/* Authentication / User Profile Capsule */}
            {authLoading ? (
              <div className="pl-2 border-l border-[var(--border-subtle)]">
                <div className="h-8 w-20 rounded-xl bg-[var(--bg-surface-elevated)] animate-pulse border border-[var(--border-subtle)]" />
              </div>
            ) : isAuthenticated && user ? (
              <div className="relative pl-2 border-l border-[var(--border-subtle)]" ref={userMenuRef}>
                <button
                  type="button"
                  onClick={() => setUserMenuOpen((prev) => !prev)}
                  aria-expanded={userMenuOpen}
                  aria-haspopup="menu"
                  aria-label="User profile and account details"
                  className="flex items-center gap-2 p-1.5 rounded-xl hover:bg-slate-200/50 dark:hover:bg-white/[0.06] border border-transparent hover:border-[var(--border-subtle)] transition-all cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400"
                >
                  <div className="relative">
                    <div className="h-7 w-7 rounded-full bg-gradient-to-tr from-sky-500 to-cyan-300 text-slate-950 font-sans font-bold text-xs flex items-center justify-center shadow-[0_0_12px_rgba(56,189,248,0.35)]">
                      {userInitial}
                    </div>
                    <span className="absolute -bottom-0.5 -right-0.5 h-2 w-2 rounded-full bg-emerald-400 ring-2 ring-[var(--bg-page)]" />
                  </div>
                  <span className="hidden lg:inline text-xs font-medium text-[var(--text-main)] max-w-[120px] truncate">
                    {user.displayName || user.email}
                  </span>
                  <ChevronDown
                    className={`h-3.5 w-3.5 text-slate-500 dark:text-slate-400 transition-transform duration-200 ${
                      userMenuOpen ? 'rotate-180 text-sky-600 dark:text-sky-400' : ''
                    }`}
                  />
                </button>

                {/* Authenticated Dropdown Menu */}
                {userMenuOpen && (
                  <div
                    role="menu"
                    aria-orientation="vertical"
                    aria-label="User account details"
                    className="absolute right-0 top-full mt-2 w-72 sm:w-80 rounded-2xl p-4 bg-[var(--bg-surface)] border border-[var(--border-subtle)] shadow-[0_20px_45px_-10px_rgba(0,0,0,0.5),0_0_25px_-5px_rgba(56,189,248,0.15)] backdrop-blur-2xl z-50 animate-in fade-in zoom-in-95 duration-150 space-y-3"
                  >
                    {/* Header with Avatar, Display Name, and Role */}
                    <div className="flex items-center gap-3 pb-3 border-b border-[var(--border-subtle)]">
                      <div className="h-10 w-10 rounded-full bg-gradient-to-tr from-sky-500 to-cyan-300 text-slate-950 font-sans font-bold text-sm flex items-center justify-center shadow-[0_0_16px_rgba(56,189,248,0.35)] shrink-0">
                        {userInitial}
                      </div>
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5">
                          <span className="font-sans font-semibold text-sm text-[var(--text-main)] truncate">
                            {user.displayName}
                          </span>
                        </div>
                        <span className="text-xs text-[var(--text-muted)] flex items-center gap-1 mt-0.5 font-mono">
                          <ShieldCheck className="h-3 w-3 text-sky-400 shrink-0" />
                          <span>{userDisplayRole}</span>
                        </span>
                      </div>
                    </div>

                    {/* Account Details */}
                    <div className="space-y-2.5 text-xs">
                      {/* Email Block */}
                      <div className="p-2.5 rounded-xl bg-[var(--bg-surface-elevated)] border border-[var(--border-subtle)]">
                        <div className="flex items-center justify-between text-[10px] font-mono tracking-wider text-[var(--text-muted)] uppercase mb-1">
                          <span className="flex items-center gap-1 font-semibold">
                            <Mail className="h-3 w-3 text-sky-600 dark:text-sky-400" />
                            Account Email
                          </span>
                          <span className="px-1.5 py-0.2 rounded bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border border-emerald-500/20 text-[9px] font-mono font-medium">
                            Verified
                          </span>
                        </div>
                        <div className="font-mono text-xs text-sky-700 dark:text-sky-300 font-medium select-all break-all">
                          {user.email}
                        </div>
                      </div>

                      {/* Role & Workspace */}
                      <div className="flex items-center justify-between px-1 text-xs">
                        <span className="text-[var(--text-muted)] flex items-center gap-1.5">
                          <UserIcon className="h-3.5 w-3.5 text-slate-500 dark:text-slate-400" />
                          System Role
                        </span>
                        <span className="font-mono text-[11px] text-[var(--text-body)] font-medium">
                          {userDisplayRole}
                        </span>
                      </div>

                      {/* Session State */}
                      <div className="flex items-center justify-between px-1 text-xs">
                        <span className="text-[var(--text-muted)]">Session Mode</span>
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-mono bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border border-emerald-500/30 font-medium">
                          <span className="h-1 w-1 rounded-full bg-emerald-500" />
                          Authenticated
                        </span>
                      </div>
                    </div>

                    {/* Sign Out Button */}
                    <div className="pt-2 border-t border-[var(--border-subtle)]">
                      <button
                        type="button"
                        onClick={async () => {
                          setUserMenuOpen(false);
                          await logout();
                        }}
                        className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl text-xs font-mono font-medium text-rose-700 dark:text-rose-400 hover:text-white bg-rose-500/10 hover:bg-rose-600 border border-rose-500/20 transition-all cursor-pointer"
                      >
                        <LogOut className="h-3.5 w-3.5" />
                        <span>Sign Out</span>
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-2 pl-2 border-l border-[var(--border-subtle)]">
                <button
                  type="button"
                  onClick={() => {
                    setAuthModalMode('login');
                    setAuthModalOpen(true);
                  }}
                  className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-mono font-medium text-sky-700 dark:text-sky-300 bg-sky-500/10 hover:bg-sky-500/20 border border-sky-500/30 transition-all cursor-pointer glow-cyan-sm"
                  aria-label="Sign in to your account"
                >
                  <LogIn className="h-3.5 w-3.5 text-sky-500 dark:text-sky-400" />
                  <span>Sign In</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Authentication Modal */}
      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        initialMode={authModalMode}
      />
    </>
  );
};
