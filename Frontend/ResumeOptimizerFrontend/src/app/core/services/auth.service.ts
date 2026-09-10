import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, catchError, finalize, of, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStorageService } from './token-storage.service';
import {
  AuthResponse,
  CurrentUser,
  LoginRequest,
  RefreshRequest,
  RegisterRequest
} from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private http = inject(HttpClient);
  private tokenStorage = inject(TokenStorageService);

  private baseUrl = `${environment.apiBaseUrl}/api/auth`;

  private currentUserSignal = signal<CurrentUser | null>(this.tokenStorage.getUser());

  currentUser = this.currentUserSignal.asReadonly();
  isAuthenticated = computed(() => !!this.currentUserSignal());

  /**
   * Set while a token refresh is in flight so that N concurrent 401s trigger
   * exactly one /refresh call. Without this, a dashboard that fires six
   * parallel requests would rotate the refresh token six times and five of
   * those rotations would be rejected, logging the user out mid-session.
   */
  private refreshInFlight$: Observable<AuthResponse> | null = null;

  /** Emits when the session ends for any reason (logout, dead refresh token). */
  sessionExpired = new Subject<void>();

  login(payload: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, payload).pipe(
      tap(res => this.handleAuthSuccess(res))
    );
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`, payload).pipe(
      tap(res => this.handleAuthSuccess(res))
    );
  }

  /**
   * Exchanges the refresh token for a new access token. Concurrent callers
   * share one HTTP request via shareReplay.
   */
  refresh(): Observable<AuthResponse> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const refreshToken = this.tokenStorage.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const body: RefreshRequest = { refreshToken };

    this.refreshInFlight$ = this.http.post<AuthResponse>(`${this.baseUrl}/refresh`, body).pipe(
      tap(res => this.handleAuthSuccess(res)),
      catchError(err => {
        // The refresh token is dead — nothing left to recover the session with.
        this.clearSession();
        this.sessionExpired.next();
        return throwError(() => err);
      }),
      finalize(() => { this.refreshInFlight$ = null; }),
      shareReplay({ bufferSize: 1, refCount: false })
    );

    return this.refreshInFlight$;
  }

  logout(): Observable<unknown> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    if (!refreshToken) {
      return of(null);
    }
    const body: RefreshRequest = { refreshToken };
    // Never let a failing logout call block the client-side sign-out.
    return this.http.post(`${this.baseUrl}/logout`, body).pipe(catchError(() => of(null)));
  }

  clearSession(): void {
    this.refreshInFlight$ = null;
    this.tokenStorage.clear();
    this.currentUserSignal.set(null);
  }

  /** Called by the OAuth callback route once tokens arrive via query params. */
  setSessionFromTokens(
    accessToken: string,
    refreshToken: string,
    user: CurrentUser,
    expiresInMs?: number
  ): void {
    this.tokenStorage.setTokens(accessToken, refreshToken, expiresInMs);
    this.tokenStorage.setUser(user);
    this.currentUserSignal.set(user);
  }

  /** Keeps the header/avatar in step after a profile edit. */
  patchCurrentUser(patch: Partial<CurrentUser>): void {
    const current = this.currentUserSignal();
    if (!current) return;

    const updated = { ...current, ...patch };
    this.tokenStorage.setUser(updated);
    this.currentUserSignal.set(updated);
  }

  private handleAuthSuccess(res: AuthResponse): void {
    this.tokenStorage.setTokens(res.accessToken, res.refreshToken, res.expiresInMs);
    const user: CurrentUser = {
      userId: res.userId,
      name: res.name,
      email: res.email,
      role: res.role
    };
    this.tokenStorage.setUser(user);
    this.currentUserSignal.set(user);
  }
}
