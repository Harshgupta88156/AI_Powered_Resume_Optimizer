import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners
} from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { ThemeService } from './core/services/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(
      routes,
      withInMemoryScrolling({
        anchorScrolling: 'enabled',
        // Without this, navigating from the bottom of a long list to a detail
        // page kept the old scroll offset and looked like a half-rendered page.
        scrollPositionRestoration: 'top'
      })
    ),
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(withInterceptors([authInterceptor])),

    // Instantiate ThemeService during bootstrap so the data-theme attribute is
    // owned by Angular from the first frame, rather than only once some
    // component happens to inject it.
    provideAppInitializer(() => {
      inject(ThemeService);
    })
  ]
};
