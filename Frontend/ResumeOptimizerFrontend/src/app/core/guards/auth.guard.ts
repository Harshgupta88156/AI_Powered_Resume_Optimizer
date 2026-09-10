import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStorageService } from '../services/token-storage.service';

/**
 * Guards return a UrlTree rather than calling router.navigate(). Navigating
 * from inside a guard while another navigation is resolving causes the two to
 * race, which is what produces the occasional blank screen on a hard refresh.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  if (tokenStorage.isAuthenticated()) {
    return true;
  }

  // Remember where they were headed so login can send them back.
  return router.createUrlTree(['/login'], {
    queryParams: state.url && state.url !== '/' ? { returnUrl: state.url } : {}
  });
};

export const guestGuard: CanActivateFn = () => {
  const tokenStorage = inject(TokenStorageService);
  const router = inject(Router);

  return tokenStorage.isAuthenticated()
    ? router.createUrlTree(['/dashboard'])
    : true;
};
