import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { AuthContext } from './AuthContextCore';
import { authService, setTokenRefreshHandler, setAccessToken } from '../api';
import type { UserResponse, LoginRequest, RegisterRequest } from '../types/auth';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Define refresh handler for single-flight token rotation and session restore
  const handleTokenRefresh = useCallback(async (): Promise<string | null> => {
    try {
      const response = await authService.refresh();
      setUser(response.user);
      return response.accessToken;
    } catch {
      setUser(null);
      setAccessToken(null);
      return null;
    }
  }, []);

  // Hook into client.ts token refresh interceptor and restore session on mount
  useEffect(() => {
    let mounted = true;

    setTokenRefreshHandler(handleTokenRefresh);

    const restoreSession = async () => {
      try {
        // Attempt silent session restoration using the HttpOnly refresh_token cookie
        const response = await authService.refresh();
        if (mounted) {
          setUser(response.user);
        }
      } catch {
        // Expected for unauthenticated visitors with no active refresh cookie
        if (mounted) {
          setUser(null);
        }
      } finally {
        if (mounted) {
          setIsLoading(false);
        }
      }
    };

    restoreSession();

    return () => {
      mounted = false;
      setTokenRefreshHandler(null);
    };
  }, [handleTokenRefresh]);

  const login = useCallback(async (request: LoginRequest): Promise<void> => {
    const response = await authService.login(request);
    setUser(response.user);
  }, []);

  const register = useCallback(async (request: RegisterRequest): Promise<UserResponse> => {
    return authService.register(request);
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
    }
  }, []);

  const refreshSession = useCallback(async (): Promise<boolean> => {
    const token = await handleTokenRefresh();
    return token !== null;
  }, [handleTokenRefresh]);

  const contextValue = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      isLoading,
      login,
      register,
      logout,
      refreshSession,
    }),
    [user, isLoading, login, register, logout, refreshSession]
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
};
