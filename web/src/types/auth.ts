/**
 * Authentication and User Domain Types
 * Grounded in backend API contracts and Spring Security DTOs:
 * - RegisterRequest.java
 * - LoginRequest.java
 * - LoginResponse.java
 * - UserResponse.java
 * - CsrfController.java
 */

export type UserRole = 'USER' | 'RESEARCHER' | 'ADMIN';

export interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
}

export type User = UserResponse;

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

export interface CsrfTokenResponse {
  token: string;
  headerName: string;
}
