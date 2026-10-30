import React, { useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Code2,
  FileCheck,
  ListFilter,
  History,
  GitCompare,
  FlaskConical,
  ChevronLeft,
  ChevronRight,
  PanelLeftClose,
  X,
  ShieldCheck,
} from 'lucide-react';
import { useNavigation } from '../../hooks/useNavigation';

export const Sidebar: React.FC = () => {
  const {
    sidebarState,
    setSidebarState,
    hideSidebar,
    mobileOpen,
    setMobileOpen,
  } = useNavigation();

  const navItems = [
    {
      to: '/',
      label: 'Dashboard',
      icon: LayoutDashboard,
      description: 'System overview & KPIs',
    },
    {
      to: '/submit',
      label: 'Analyze Code',
      icon: Code2,
      badge: 'Java',
      description: 'Java submission & AST audit',
    },
    {
      to: '/report',
      label: 'Audit Report',
      icon: FileCheck,
      description: 'A–F score & line findings',
    },
    {
      to: '/findings',
      label: 'Findings Explorer',
      icon: ListFilter,
      description: 'Rules & feedback triage',
    },
    {
      to: '/history',
      label: 'History & Lineage',
      icon: History,
      description: 'Remediation version chains',
    },
    {
      to: '/compare',
      label: 'Version Compare',
      icon: GitCompare,
      description: 'Resolved vs introduced delta',
    },
    {
      to: '/experiments',
      label: 'Research Lab',
      icon: FlaskConical,
      badge: '95% CI',
      description: 'Paired task experiment trials',
    },
  ];

  const isExpanded = sidebarState === 'expanded';
  const isCompact = sidebarState === 'compact';
  const isHidden = sidebarState === 'hidden';

  // Handle Escape key to close mobile drawer
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && mobileOpen) {
        setMobileOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileOpen, setMobileOpen]);

  // Lock body scroll when mobile drawer is open
  useEffect(() => {
    if (mobileOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [mobileOpen]);

  // Navigation Links Renderer
  const renderNavLinks = (forMobile = false) => (
    <div className="p-3 space-y-1.5 overflow-y-auto flex-1">
      <div className="px-3 py-2 text-[10px] font-mono uppercase tracking-widest text-slate-500 font-semibold select-none">
        {(isExpanded || forMobile) && <span>Workspace</span>}
      </div>

      {navItems.map((item) => {
        const IconComponent = item.icon;
        return (
          <NavLink
            key={item.to}
            to={item.to}
            tabIndex={isHidden && !forMobile ? -1 : 0}
            onClick={() => forMobile && setMobileOpen(false)}
            aria-label={item.label}
            title={isCompact && !forMobile ? `${item.label} — ${item.description}` : undefined}
            className={({ isActive }) =>
              `group flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-all duration-200 relative select-none ${
                isActive
                  ? 'bg-sky-400/10 text-sky-400 border border-sky-400/30 shadow-[0_0_20px_-5px_rgba(56,189,248,0.25)] font-semibold'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-white/[0.04] border border-transparent'
              } ${isCompact && !forMobile ? 'justify-center' : 'justify-start'}`
            }
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <span
                    className="absolute left-0 top-2 bottom-2 w-1 rounded-r-full bg-sky-400 shadow-[0_0_8px_#38BDF8]"
                    aria-hidden="true"
                  />
                )}
                <span className="shrink-0 transition-transform duration-200 group-hover:scale-110">
                  <IconComponent className="h-4 w-4" />
                </span>

                {(isExpanded || forMobile) && (
                  <div className="flex-1 flex items-center justify-between min-w-0">
                    <span className="truncate">{item.label}</span>
                    {item.badge && (
                      <span className="ml-2 px-1.5 py-0.5 rounded text-[10px] font-mono font-semibold bg-white/[0.05] text-sky-300 border border-sky-400/20">
                        {item.badge}
                      </span>
                    )}
                  </div>
                )}
              </>
            )}
          </NavLink>
        );
      })}
    </div>
  );

  return (
    <>
      {/* =====================================================================
          1. ACCESSIBLE MOBILE DRAWER (Slide-out Overlay)
          ===================================================================== */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-50 md:hidden flex"
          role="dialog"
          aria-modal="true"
          aria-label="Mobile Navigation Drawer"
        >
          {/* Backdrop */}
          <div
            className="fixed inset-0 bg-black/70 backdrop-blur-sm transition-opacity"
            onClick={() => setMobileOpen(false)}
            aria-hidden="true"
          />

          {/* Drawer Content */}
          <div className="relative w-72 max-w-[85vw] bg-white dark:bg-[#0C0F17] border-r border-[var(--border-subtle)] flex flex-col justify-between shadow-2xl z-10 animate-in slide-in-from-left duration-250">
            <div className="p-4 border-b border-[var(--border-subtle)] flex items-center justify-between">
              <span className="font-heading font-bold text-sm text-[var(--text-main)]">
                Navigation
              </span>
              <button
                type="button"
                onClick={() => setMobileOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-white/[0.05] transition-colors cursor-pointer"
                aria-label="Close navigation drawer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {renderNavLinks(true)}

            <div className="p-4 border-t border-[var(--border-subtle)] text-[11px] font-mono text-slate-500">
              AI Code Quality Auditor • v1.0.0
            </div>
          </div>
        </div>
      )}

      {/* =====================================================================
          2. DESKTOP SIDEBAR: 3 States (Expanded, Compact Icon Rail, Fully Hidden)
          ===================================================================== */}
      <aside
        id="main-sidebar"
        role="navigation"
        aria-label="Primary Navigation"
        aria-hidden={isHidden}
        className={`hidden md:flex flex-col justify-between shrink-0 border-r border-[var(--border-subtle)] bg-white/95 dark:bg-[#0A0D15]/90 backdrop-blur-xl transition-all duration-300 ease-in-out relative z-20 ${
          isHidden
            ? 'w-0 min-w-0 max-w-0 border-r-0 overflow-hidden opacity-0 pointer-events-none select-none p-0 m-0'
            : isCompact
            ? 'w-[72px] min-w-[72px] max-w-[72px]'
            : 'w-64 min-w-[16rem] max-w-[16rem]'
        }`}
      >
        {/* Constrained Inner Content Wrapper to prevent flexbox child overflow */}
        <div
          className={`flex flex-col h-full justify-between transition-opacity duration-200 ${
            isHidden ? 'w-0 overflow-hidden invisible' : isCompact ? 'w-[72px]' : 'w-64'
          }`}
        >
          {renderNavLinks(false)}

          {/* Footer Controls: Toggle Compact / Expand / Hide */}
          <div className="p-3 border-t border-white/[0.06] space-y-1.5">
            {/* Subtle Verifier Badge when expanded */}
            {isExpanded && (
              <div className="p-2.5 rounded-xl bg-white/[0.02] border border-white/[0.05] text-[11px] space-y-1">
                <div className="flex items-center gap-1.5 font-bold text-slate-300">
                  <ShieldCheck className="h-3.5 w-3.5 text-sky-400" />
                  <span>Deterministic Verifier</span>
                </div>
                <p className="text-[10px] text-slate-500 font-editorial italic leading-tight">
                  Analyzers operate independently of AI services.
                </p>
              </div>
            )}

            {/* Action Buttons Row */}
            <div className="flex items-center gap-1">
              {/* Expand / Collapse Button */}
              <button
                type="button"
                tabIndex={isHidden ? -1 : 0}
                onClick={() => setSidebarState(isCompact ? 'expanded' : 'compact')}
                className="flex-1 flex items-center justify-center gap-2 p-2 rounded-xl text-xs text-slate-400 hover:text-white hover:bg-white/[0.05] transition-colors cursor-pointer border border-transparent hover:border-white/[0.06]"
                title={isCompact ? 'Expand sidebar with labels' : 'Collapse to compact icon rail'}
                aria-label={isCompact ? 'Expand sidebar with labels' : 'Collapse to compact icon rail'}
              >
                {isCompact ? (
                  <ChevronRight className="h-4 w-4" />
                ) : (
                  <>
                    <ChevronLeft className="h-4 w-4" />
                    <span className="text-[11px] font-mono">Compact</span>
                  </>
                )}
              </button>

              {/* Hide Button (Available in both expanded & compact states) */}
              <button
                type="button"
                tabIndex={isHidden ? -1 : 0}
                onClick={hideSidebar}
                className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/[0.05] transition-colors cursor-pointer border border-transparent hover:border-white/[0.06]"
                title="Hide sidebar completely"
                aria-label="Hide sidebar completely"
              >
                <PanelLeftClose className="h-4 w-4" />
              </button>
            </div>
          </div>
        </div>
      </aside>
    </>
  );
};
