import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowUp, Code2, SlidersHorizontal } from 'lucide-react';

interface FloatingQuickDockProps {
  onToggleSample?: () => void;
  showSamplePreview?: boolean;
}

export const FloatingQuickDock: React.FC<FloatingQuickDockProps> = ({
  onToggleSample,
  showSamplePreview,
}) => {
  const navigate = useNavigate();

  const scrollToTop = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <aside
      aria-label="Quick Actions Dock"
      className="fixed bottom-6 left-1/2 -translate-x-1/2 z-30 hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-[#0D101C]/85 border border-white/[0.1] shadow-[0_10px_35px_-10px_rgba(0,0,0,0.8),0_0_20px_-5px_rgba(56,189,248,0.2)] backdrop-blur-2xl transition-all hover:border-sky-400/40"
    >
      {/* 1. Jump to Top */}
      <button
        type="button"
        onClick={scrollToTop}
        className="p-2 rounded-full text-slate-400 hover:text-white hover:bg-white/[0.08] transition-all duration-200 hover:scale-120 cursor-pointer"
        title="Scroll to top"
        aria-label="Scroll to top"
      >
        <ArrowUp className="h-4 w-4" />
      </button>

      <span className="w-px h-4 bg-white/[0.1]" />

      {/* 2. Analyze Java Code Shortcut */}
      <button
        type="button"
        onClick={() => navigate('/submit')}
        className="p-2 rounded-full text-sky-400 hover:text-sky-300 hover:bg-sky-400/10 transition-all duration-200 hover:scale-120 cursor-pointer"
        title="Quick open: Analyze Java Code"
        aria-label="Quick open: Analyze Java Code"
      >
        <Code2 className="h-4 w-4" />
      </button>

      {/* 3. Toggle Sample Fixtures */}
      {onToggleSample && (
        <>
          <span className="w-px h-4 bg-white/[0.1]" />
          <button
            type="button"
            onClick={onToggleSample}
            className={`p-2 rounded-full transition-all duration-200 hover:scale-120 cursor-pointer ${
              showSamplePreview
                ? 'text-amber-400 bg-amber-400/10 hover:bg-amber-400/20'
                : 'text-slate-400 hover:text-white hover:bg-white/[0.08]'
            }`}
            title={
              showSamplePreview
                ? 'Switch to Honest Empty State'
                : 'Preview Sample Test Fixtures'
            }
            aria-label="Toggle sample test fixtures preview"
          >
            <SlidersHorizontal className="h-4 w-4" />
          </button>
        </>
      )}
    </aside>
  );
};
