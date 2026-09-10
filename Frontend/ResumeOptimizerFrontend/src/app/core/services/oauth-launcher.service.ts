import { Injectable, inject } from '@angular/core';
import { TokenStorageService } from './token-storage.service';
import { environment } from '../../../environments/environment';

/**
 * Starts an external sign-in.
 *
 * Two things here fix "I picked a different account but it logged me back into
 * the old one":
 *
 * 1. `prompt=select_account` — GitHub otherwise reuses whichever account is
 *    already signed in on github.com and redirects straight back without ever
 *    showing a picker. This parameter forces the picker every time.
 * 2. The local session is wiped *before* leaving the page. Otherwise the stale
 *    access token is still in localStorage when the callback route loads, and a
 *    failure part-way through the handshake leaves the previous user signed in.
 */
@Injectable({ providedIn: 'root' })
export class OauthLauncherService {

  private tokenStorage = inject(TokenStorageService);

  /**
   * @param returnUrl in-app route to land on after a successful sign-in.
   */
  startGithub(returnUrl?: string | null): void {
    this.tokenStorage.clear();

    if (returnUrl) {
      try {
        sessionStorage.setItem('ro_oauth_return_url', returnUrl);
      } catch {
        // Non-fatal: we just fall back to /dashboard after sign-in.
      }
    }

    const url = new URL(environment.githubOAuthUrl);
    url.searchParams.set('prompt', 'select_account');

    window.location.href = url.toString();
  }

  /** Consumes the stored return URL exactly once. */
  takeReturnUrl(): string | null {
    try {
      const value = sessionStorage.getItem('ro_oauth_return_url');
      sessionStorage.removeItem('ro_oauth_return_url');
      return value;
    } catch {
      return null;
    }
  }
}
