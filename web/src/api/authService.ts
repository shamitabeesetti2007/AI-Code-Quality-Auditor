/**
 * Authentication API Service
 * Strictly aligned with verified backend endpoints:
 * - POST /api/v1/auth/register
 * - POST /api/v1/auth/login
 * - POST /api/v1/auth/refresh
 * - POST /api/v1/auth/logout
 */

import { apiFetch, setAccessToken, ensureCsrfToken } from './client';
import type {
  RegisterRequest,
  LoginRequest,
  LoginResponse,
  UserResponse,
} from '../types/auth';

/**
 * Register a new user account.
 * Public endpoint: POST /api/v1/auth/register
 */
export async function register(request: RegisterRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify(request),
    skipAuth: true,
    skipCsrf: true, // Backend ignores CSRF for public registration
  });
}

/**
 * Authenticate with email and password.
 * Public endpoint: POST /api/v1/auth/login
 * Sets access token in memory; backend delivers refresh_token in HttpOnly cookie.
 */
export async function login(request: LoginRequest): Promise<LoginResponse> {
  const data = await apiFetch<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(request),
    skipAuth: true,
    skipCsrf: true, // Backend ignores CSRF for public login
  });

  if (data?.accessToken) {
    setAccessToken(data.accessToken);
  }

  return data;
}

/**
 * Rotate refresh token and obtain new access token.
 * Protected endpoint: POST /api/v1/auth/refresh
 * Requires: refresh_token cookie (credentials: include) and CSRF header.
 */
export async function refresh(): Promise<LoginResponse> {
  // Ensure CSRF token is fetched before calling refresh
  await ensureCsrfToken();

  const data = await apiFetch<LoginResponse>('/api/v1/auth/refresh', {
    method: 'POST',
    skipAuth: true, // Refresh relies on cookie + CSRF, not Bearer token
    skipRefreshRetry: true, // Do not trigger refresh loop if refresh itself fails
  });

  if (data?.accessToken) {
    setAccessToken(data.accessToken);
  }

  return data;
}

/**
 * Revoke refresh token and log out.
 * Protected endpoint: POST /api/v1/auth/logout
 * Requires: refresh_token cookie (credentials: include) and CSRF header.
 */
export async function logout(): Promise<void> {
  try {
    await ensureCsrfToken();
    await apiFetch<void>('/api/v1/auth/logout', {
      method: 'POST',
      skipAuth: true,
      skipRefreshRetry: true,
    });
  } finally {
    // Always clear the in-memory access token, even if server request fails
    setAccessToken(null);
  }
}
