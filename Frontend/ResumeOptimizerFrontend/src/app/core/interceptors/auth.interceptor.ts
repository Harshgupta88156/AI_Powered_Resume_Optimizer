import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../services/auth.service';

/** Endpoints that must never carry (or trigger a refresh of) the access token. */
function isAuthEndpoint(req: HttpRequest<unknown>): boolean {
  return req.url.includes('/api/auth/');
}

/**
 * Attaches the JWT to outgoing requests and — crucially — recovers from an
 * expired access token instead of dumping the user on the login screen.
 *
 * On a 401 the interceptor asks AuthService for a fresh token (all concurrent
 * 401s share a single /refresh call) and replays the original request. Only if
 * the refresh itself fails is the session cleared.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStorage = inject(TokenStorageService);
  const authService = inject(AuthService);
  const router = inject(Router);

  const authEndpoint = isAuthEndpoint(req);

  const withToken = (request: HttpRequest<unknown>) => {
    const token = tokenStorage.getAccessToken();
    return token && !authEndpoint
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;
  };

  return next(withToken(req)).pipe(
    catchError((error: HttpErrorResponse) => {
      const canRetry =
        error.status === 401 &&
        !authEndpoint &&
        !!tokenStorage.getRefreshToken();

      if (!canRetry) {
        if (error.status === 401 && !authEndpoint) {
          authService.clearSession();
          redirectToLogin(router);
        }
        return throwError(() => error);
      }

      return authService.refresh().pipe(
        // Rebuild the request so it picks up the newly stored token.
        switchMap(() => next(withToken(req))),
        catchError((refreshError) => {
          authService.clearSession();
          redirectToLogin(router);
          return throwError(() => refreshError);
        })
      );
    })
  );
};

function redirectToLogin(router: Router): void {
  const current = router.url;
  const isPublic =
    current.startsWith('/login') ||
    current.startsWith('/register') ||
    current === '/' ||
    current.startsWith('/oauth2') ||
    current.startsWith('/callback');

  router.navigate(['/login'], {
    queryParams: isPublic ? {} : { returnUrl: current }
  });
}
