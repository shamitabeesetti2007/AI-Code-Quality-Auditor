import React from 'react';
import { Navbar } from './Navbar';
import { Sidebar } from './Sidebar';
import { NavigationProvider } from '../../context/NavigationContext';
import { useNavigation } from '../../hooks/useNavigation';
import { PanelLeftOpen } from 'lucide-react';

const ShellContent: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { sidebarState, showSidebar } = useNavigation();
  const isHidden = sidebarState === 'hidden';

  return (
    <div className="min-h-screen flex flex-col bg-[var(--bg-page)] text-[var(--text-body)] antialiased transition-colors duration-300 relative selection:bg-sky-500/20 selection:text-sky-300">
      {/* Top Fixed Header */}
      <Navbar />

      {/* Main Workspace: Sidebar + Smoothly Expanding Canvas */}
      <div className="flex-1 flex overflow-hidden relative">
        <Sidebar />

        {/* Small, Accessible Edge-Mounted Toggle to Reopen when Sidebar is Hidden */}
        {isHidden && (
          <button
            type="button"
            onClick={showSidebar}
            className="hidden md:flex fixed left-0 top-24 z-40 items-center gap-1.5 pl-2.5 pr-3 py-2 rounded-r-xl bg-white dark:bg-[#0C0F17]/95 hover:bg-slate-50 dark:hover:bg-[#151A2B] text-sky-700 dark:text-sky-400 border-y border-r border-sky-400/30 hover:border-sky-400 shadow-[0_4px_25px_rgba(0,0,0,0.15),0_0_20px_-3px_rgba(56,189,248,0.25)] dark:shadow-[0_4px_25px_rgba(0,0,0,0.6),0_0_20px_-3px_rgba(56,189,248,0.35)] backdrop-blur-xl transition-all duration-200 cursor-pointer group focus-visible:outline-2 focus-visible:outline-sky-400"
            title="Open navigation sidebar"
            aria-label="Open navigation sidebar"
            aria-expanded="false"
            aria-controls="main-sidebar"
          >
            <PanelLeftOpen className="h-4 w-4 text-sky-400 transition-transform group-hover:translate-x-0.5" />
            <span className="text-[11px] font-mono font-semibold tracking-tight">Sidebar</span>
          </button>
        )}

        {/* Scrollable Main Canvas: Smoothly expands to 100% when sidebar collapses */}
        <main
          id="main-content"
          role="main"
          tabIndex={-1}
          className="flex-1 overflow-y-auto bg-tech-grid bg-[var(--bg-page)] px-4 py-8 sm:px-8 lg:px-12 transition-all duration-300 ease-in-out focus:outline-none"
        >
          <div className="max-w-6xl mx-auto space-y-12">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
};

export const AppShell: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <NavigationProvider>
      <ShellContent>{children}</ShellContent>
    </NavigationProvider>
  );
};
