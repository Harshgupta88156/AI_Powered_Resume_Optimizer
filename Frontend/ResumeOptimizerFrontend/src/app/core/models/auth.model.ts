export type UserRole = 'USER' | 'ADMIN';
export type AuthProvider = 'LOCAL' | 'GITHUB';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  userId: number;
  name: string;
  email: string;
  role: UserRole;
}

export interface CurrentUser {
  userId: number;
  name: string;
  email: string;
  role: UserRole;
}

