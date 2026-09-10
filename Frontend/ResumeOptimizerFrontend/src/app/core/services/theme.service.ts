import { DOCUMENT } from '@angular/common';
import { Injectable, computed, effect, inject, signal } from '@angular/core';

export type ThemePreference = 'light' | 'dark' | 'system';
export type ResolvedTheme = 'light' | 'dark';

const STORAGE_KEY = 'ro_theme';

/**
 * Owns the application colour scheme.
 *
 * The resolved theme is written to `<html data-theme="...">`; every colour in
 * the app is a CSS variable defined for both values in styles.css, so a single
 * attribute swap re-themes the whole UI with no component-level work.
 *
 * `system` follows the OS setting live via a media-query listener.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {

  private document = inject(DOCUMENT);

  /** What the user chose. Persisted. */
  preference = signal<ThemePreference>(this.readStoredPreference());

  /** What the OS reports right now. Updated live. */
  private systemTheme = signal<ResolvedTheme>(this.readSystemTheme());

  /** What is actually painted. */
  resolved = computed<ResolvedTheme>(() => {
    const pref = this.preference();
    return pref === 'system' ? this.systemTheme() : pref;
  });

  isDark = computed(() => this.resolved() === 'dark');

  constructor() {
    this.watchSystemTheme();

    effect(() => {
      const theme = this.resolved();
      const root = this.document.documentElement;
      root.setAttribute('data-theme', theme);
      root.style.colorScheme = theme;

      // Keep the mobile browser chrome in step with the app background.
      const meta = this.document.querySelector('meta[name="theme-color"]');
      if (meta) {
        meta.setAttribute('content', theme === 'dark' ? '#0B1220' : '#F8FAFC');
      }
    });
  }

  setPreference(preference: ThemePreference): void {
    this.preference.set(preference);
    try {
      localStorage.setItem(STORAGE_KEY, preference);
    } catch {
      // Private mode / storage disabled — the theme still applies for this session.
    }
  }

  /** Light -> Dark -> System -> Light. */
  cycle(): void {
    const order: ThemePreference[] = ['light', 'dark', 'system'];
    const next = order[(order.indexOf(this.preference()) + 1) % order.length];
    this.setPreference(next);
  }

  /** Straight flip between light and dark, resolving `system` first. */
  toggle(): void {
    this.setPreference(this.resolved() === 'dark' ? 'light' : 'dark');
  }

  private readStoredPreference(): ThemePreference {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        return stored;
      }
    } catch {
      // ignore
    }
    return 'system';
  }

  private readSystemTheme(): ResolvedTheme {
    const win = this.document.defaultView;
    if (!win?.matchMedia) {
      return 'light';
    }
    return win.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  private watchSystemTheme(): void {
    const win = this.document.defaultView;
    if (!win?.matchMedia) {
      return;
    }

    const query = win.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (event: MediaQueryListEvent) =>
      this.systemTheme.set(event.matches ? 'dark' : 'light');

    if (typeof query.addEventListener === 'function') {
      query.addEventListener('change', onChange);
    } else {
      // Safari < 14
      query.addListener(onChange);
    }
  }
}
