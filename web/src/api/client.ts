/**
 * HTTP Client & Transport Foundation
 * Grounded in docs/api/contract.md and docs/api/openapi.yaml.
 * 
 * Conventions:
 * - Base path: /api/v1 (or environment variable VITE_API_BASE_URL)
 * - Content type: application/json
 * - Error response: RFC 7807 application/problem+json
 * - Short-lived access token held strictly in memory
 * - Refresh token delivered only via HttpOnly cookie (credentials: 'include')
 * - State-changing requests protected via CSRF header (X-XSRF-TOKEN)
 */

import type { CsrfTokenResponse } from '../types/auth';

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  [key: string]: unknown;
}

export class ApiClientError extends Error {
  public status: number;
  public problem?: ProblemDetails;

  constructor(message: string, status: number, problem?: ProblemDetails) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.problem = problem;
  }
}

export interface ApiFetchOptions extends RequestInit {
  skipCsrf?: boolean;
  skipAuth?: boolean;
  skipRefreshRetry?: boolean;
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) || '';

/**
 * In-memory Access Token Storage
 * Security rule: Never persist access tokens to localStorage or sessionStorage.
 */
let inMemoryAccessToken: string | null = null;

export function getAccessToken(): string | null {
  return inMemoryAccessToken;
}

export function setAccessToken(token: string | null): void {
  inMemoryAccessToken = token;
}

/**
 * In-memory CSRF Token Storage
 */
let cachedCsrfToken: CsrfTokenResponse | null = null;

export function getCachedCsrfToken(): CsrfTokenResponse | null {
  return cachedCsrfToken;
}

export function setCachedCsrfToken(csrf: CsrfTokenResponse | null): void {
  cachedCsrfToken = csrf;
}

/**
 * Read the readable XSRF-TOKEN cookie if provisioned by Spring Security.
 */
export function getXsrfCookieValue(): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Fetch a fresh CSRF token from GET /api/v1/csrf.
 */
export async function fetchCsrfToken(): Promise<CsrfTokenResponse> {
  const data = await apiFetch<CsrfTokenResponse>('/api/v1/csrf', {
    method: 'GET',
    skipCsrf: true,
    skipAuth: true,
    skipRefreshRetry: true,
  });
  cachedCsrfToken = data;
  return data;
}

/**
 * Ensure a valid CSRF token is available.
 * Checks cookie first, then in-memory cache, or fetches from /api/v1/csrf.
 */
export async function ensureCsrfToken(): Promise<CsrfTokenResponse> {
  const cookieValue = getXsrfCookieValue();
  if (cookieValue) {
    const tokenObj: CsrfTokenResponse = {
      token: cookieValue,
      headerName: cachedCsrfToken?.headerName || 'X-XSRF-TOKEN',
    };
    cachedCsrfToken = tokenObj;
    return tokenObj;
  }

  if (cachedCsrfToken) {
    return cachedCsrfToken;
  }

  return fetchCsrfToken();
}

/**
 * Single-flight token refresh handler callback.
 */
type RefreshHandler = () => Promise<string | null>;
let tokenRefreshHandler: RefreshHandler | null = null;
let activeRefreshPromise: Promise<string | null> | null = null;

export function setTokenRefreshHandler(handler: RefreshHandler | null): void {
  tokenRefreshHandler = handler;
}

/**
 * Execute an HTTP request with JSON conventions, credentials, CSRF, and RFC 7807 error parsing.
 */
export async function apiFetch<T>(
  endpoint: string,
  options: ApiFetchOptions = {}
): Promise<T> {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = new Headers(options.headers || {});
  const method = (options.method || 'GET').toUpperCase();

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  // Inject in-memory Bearer token when authenticated unless explicitly skipped
  if (!options.skipAuth && inMemoryAccessToken && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${inMemoryAccessToken}`);
  }

  // Inject CSRF token for mutating state-changing requests unless explicitly skipped
  const isMutating = ['POST', 'PATCH', 'PUT', 'DELETE'].includes(method);
  if (isMutating && !options.skipCsrf) {
    try {
      const csrf = await ensureCsrfToken();
      if (csrf && !headers.has(csrf.headerName)) {
        headers.set(csrf.headerName, csrf.token);
      }
    } catch {
      // If fetching CSRF fails, continue and let backend return appropriate response
    }
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers,
      credentials: 'include', // Always send HttpOnly cookies (refresh_token, XSRF-TOKEN)
    });

    if (!response.ok) {
      let problem: ProblemDetails | undefined;
      const contentType = response.headers.get('Content-Type') || '';

      if (contentType.includes('application/json') || contentType.includes('problem+json')) {
        try {
          problem = (await response.json()) as ProblemDetails;
        } catch {
          // JSON parsing failed; problem remains undefined
        }
      }

      // Handle 401 Unauthorized with single-flight silent refresh retry if authenticated
      if (
        response.status === 401 &&
        !options.skipRefreshRetry &&
        tokenRefreshHandler &&
        inMemoryAccessToken &&
        !endpoint.includes('/api/v1/auth/refresh') &&
        !endpoint.includes('/api/v1/auth/login')
      ) {
        if (!activeRefreshPromise) {
          activeRefreshPromise = tokenRefreshHandler().finally(() => {
            activeRefreshPromise = null;
          });
        }

        const newAccessToken = await activeRefreshPromise;
        if (newAccessToken) {
          // Retry the original request once with new token
          return apiFetch<T>(endpoint, {
            ...options,
            skipRefreshRetry: true,
          });
        }
      }

      const errorMessage =
        problem?.detail ||
        problem?.title ||
        `HTTP ${response.status}: ${response.statusText}`;

      throw new ApiClientError(errorMessage, response.status, problem);
    }

    if (response.status === 204) {
      return null as T;
    }

    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiClientError) {
      throw error;
    }
    throw new ApiClientError(
      error instanceof Error ? error.message : 'Network transport error',
      0
    );
  }
}
