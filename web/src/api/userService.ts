/**
 * User API Service
 * Strictly aligned with verified backend endpoints:
 * - GET /api/v1/users/me
 */

import { apiFetch } from './client';
import type { UserResponse } from '../types/auth';

/**
 * Fetch the authenticated user's profile.
 * Protected endpoint: GET /api/v1/users/me
 * Requires: Authorization: Bearer <token>
 */
export async function getCurrentUser(): Promise<UserResponse> {
  return apiFetch<UserResponse>('/api/v1/users/me', {
    method: 'GET',
  });
}
