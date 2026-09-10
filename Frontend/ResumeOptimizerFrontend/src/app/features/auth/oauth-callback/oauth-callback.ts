import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { TokenStorageService } from '../../../core/services/token-storage.service';
import { OauthLauncherService } from '../../../core/services/oauth-launcher.service';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../core/services/toast.service';
import { Spinner } from '../../../shared/components/spinner/spinner';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule, RouterLink, Spinner],
  templateUrl: './oauth-callback.html',
  styleUrl: './oauth-callback.css'
})
export class OauthCallback implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private tokenStorage = inject(TokenStorageService);
  private oauthLauncher = inject(OauthLauncherService);
  private userService = inject(UserService);
  private toast = inject(ToastService);

  failed = signal(false);
  failureMessage = signal('');

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;

    // auth-service / GitHub can hand back an error instead of tokens.
    const providerError = params.get('error') ?? params.get('error_description');
    if (providerError) {
      this.fail(this.describe(providerError));
      return;
    }

    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');
    const expiresInMs = Number(params.get('expiresInMs')) || undefined;

    if (!accessToken || !refreshToken) {
      this.fail('The sign-in link did not include a session. Please try again.');
      return;
    }

    // Drop whatever was in storage before trusting the new tokens. Switching
    // accounts must never leave the previous user's id or name behind.
    this.authService.clearSession();
    this.tokenStorage.setTokens(accessToken, refreshToken, expiresInMs);

    this.userService.getMe().subscribe({
      next: (user) => {
        this.authService.setSessionFromTokens(
          accessToken,
          refreshToken,
          {
            userId: user.userId,
            name: user.name,
            email: user.email,
            role: user.role
          },
          expiresInMs
        );

        this.toast.success(`Signed in as ${user.name}.`);
        this.stripTokensFromUrl();

        const returnUrl = this.oauthLauncher.takeReturnUrl();
        this.router.navigateByUrl(returnUrl || '/dashboard');
      },
      error: () => {
        this.authService.clearSession();
        this.fail('We could not load your account after sign-in. Please try again.');
      }
    });
  }

  retry(): void {
    this.oauthLauncher.startGithub(null);
  }

  private fail(message: string): void {
    this.failureMessage.set(message);
    this.failed.set(true);
    this.toast.error(message);
  }

  private describe(raw: string): string {
    if (raw.includes('access_denied')) {
      return 'GitHub sign-in was cancelled.';
    }
    return 'GitHub sign-in failed. Please try again.';
  }

  /**
   * Tokens arrive as query params, which end up in browser history and in any
   * Referer header the page sends. Replace the URL so they do not linger.
   */
  private stripTokensFromUrl(): void {
    try {
      window.history.replaceState({}, document.title, window.location.pathname);
    } catch {
      // ignore
    }
  }
}
