import { createContext } from 'react';

export type SidebarState = 'expanded' | 'compact' | 'hidden';

export interface NavigationContextType {
  sidebarState: SidebarState;
  setSidebarState: (state: SidebarState) => void;
  cycleSidebarState: () => void;
  hideSidebar: () => void;
  showSidebar: () => void;
  mobileOpen: boolean;
  setMobileOpen: (open: boolean) => void;
  toggleMobileOpen: () => void;
}

export const NavigationContext = createContext<NavigationContextType | undefined>(undefined);
export const SIDEBAR_STORAGE_KEY = 'auditor-sidebar-state';
