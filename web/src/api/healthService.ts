/**
 * Operational Health Service
 * Grounded in docs/api/openapi.yaml:
 * GET /actuator/health (operationId: getApplicationHealth)
 */

import { apiFetch, ApiClientError } from './client';

export type HealthStatus = 'UP' | 'DOWN' | 'UNKNOWN';

export interface ActuatorHealthResponse {
  status: HealthStatus;
  components?: Record<string, { status: HealthStatus; details?: Record<string, unknown> }>;
}

export interface HealthCheckResult {
  connected: boolean;
  status: HealthStatus;
  message?: string;
}

/**
 * Check backend application health.
 * Non-throwing safe helper for UI health indicator checks.
 */
export async function checkBackendHealth(): Promise<HealthCheckResult> {
  try {
    const data = await apiFetch<ActuatorHealthResponse>('/actuator/health');
    return {
      connected: data.status === 'UP',
      status: data.status,
    };
  } catch (error) {
    return {
      connected: false,
      status: 'DOWN',
      message:
        error instanceof ApiClientError
          ? error.message
          : 'Backend disconnected / unreachable',
    };
  }
}
