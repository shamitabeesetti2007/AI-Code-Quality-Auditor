import React, { useEffect, useState } from 'react';
import {
  NavigationContext,
  SIDEBAR_STORAGE_KEY,
  type SidebarState,
} from './NavigationContextCore';

export const NavigationProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [sidebarState, setSidebarStateInternal] = useState<SidebarState>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem(SIDEBAR_STORAGE_KEY);
      if (saved === 'expanded' || saved === 'compact' || saved === 'hidden') {
        return saved;
      }
    }
    return 'expanded';
  });

  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    localStorage.setItem(SIDEBAR_STORAGE_KEY, sidebarState);
  }, [sidebarState]);

  const setSidebarState = (state: SidebarState) => {
    setSidebarStateInternal(state);
  };

  const cycleSidebarState = () => {
    setSidebarStateInternal((prev) => {
      if (prev === 'expanded') return 'compact';
      if (prev === 'compact') return 'hidden';
      return 'expanded';
    });
  };

  const hideSidebar = () => {
    setSidebarStateInternal('hidden');
  };

  const showSidebar = () => {
    setSidebarStateInternal('expanded');
  };

  const toggleMobileOpen = () => {
    setMobileOpen((prev) => !prev);
  };

  return (
    <NavigationContext.Provider
      value={{
        sidebarState,
        setSidebarState,
        cycleSidebarState,
        hideSidebar,
        showSidebar,
        mobileOpen,
        setMobileOpen,
        toggleMobileOpen,
      }}
    >
      {children}
    </NavigationContext.Provider>
  );
};
