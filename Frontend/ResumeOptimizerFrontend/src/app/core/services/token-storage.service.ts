import { Injectable } from '@angular/core';
import { CurrentUser } from '../models/auth.model';

const ACCESS_TOKEN_KEY = 'ro_access_token';
const REFRESH_TOKEN_KEY = 'ro_refresh_token';
const USER_KEY = 'ro_current_user';
const EXPIRES_AT_KEY = 'ro_access_expires_at';

/**
 * Single owner of the auth material in localStorage.
 *
 * Every read is defensive: a half-written or hand-edited entry must not throw
 * during app bootstrap, so a corrupt value is treated as "not signed in"
 * instead of crashing the router.
 */
@Injectable({ providedIn: 'root' })
export class TokenStorageService {

  getAccessToken(): string | null {
    return this.read(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return this.read(REFRESH_TOKEN_KEY);
  }

  /** @param expiresInMs lifetime of the access token as reported by the server. */
  setTokens(accessToken: string, refreshToken: string, expiresInMs?: number): void {
    this.write(ACCESS_TOKEN_KEY, accessToken);
    this.write(REFRESH_TOKEN_KEY, refreshToken);

    if (expiresInMs && expiresInMs > 0) {
      this.write(EXPIRES_AT_KEY, String(Date.now() + expiresInMs));
    } else {
      this.remove(EXPIRES_AT_KEY);
    }
  }

  /** Epoch millis at which the access token stops being accepted, if known. */
  getAccessTokenExpiresAt(): number | null {
    const raw = this.read(EXPIRES_AT_KEY);
    if (!raw) return null;
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  /**
   * True when the access token is gone or within `skewMs` of expiring.
   * The skew stops us from firing a request that is certain to 401 in flight.
   */
  isAccessTokenExpiring(skewMs = 60_000): boolean {
    if (!this.getAccessToken()) return true;

    const expiresAt = this.getAccessTokenExpiresAt();
    if (expiresAt == null) return false;

    return Date.now() + skewMs >= expiresAt;
  }

  getUser(): CurrentUser | null {
    const raw = this.read(USER_KEY);
    if (!raw) return null;

    try {
      const parsed = JSON.parse(raw) as CurrentUser;
      return parsed && typeof parsed === 'object' && parsed.userId != null ? parsed : null;
    } catch {
      this.remove(USER_KEY);
      return null;
    }
  }

  setUser(user: CurrentUser): void {
    this.write(USER_KEY, JSON.stringify(user));
  }

  clear(): void {
    this.remove(ACCESS_TOKEN_KEY);
    this.remove(REFRESH_TOKEN_KEY);
    this.remove(USER_KEY);
    this.remove(EXPIRES_AT_KEY);
  }

  /**
   * A refresh token alone is enough to re-establish a session, so it - not the
   * short-lived access token - decides whether the user is "signed in".
   */
  isAuthenticated(): boolean {
    return !!this.getAccessToken() || !!this.getRefreshToken();
  }

  private read(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // Storage unavailable (private mode, quota). Session lives in memory only.
    }
  }

  private remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // ignore
    }
  }
}
