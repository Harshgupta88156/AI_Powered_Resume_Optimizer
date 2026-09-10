import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService, ThemePreference } from '../../../core/services/theme.service';

/**
 * Three-state colour scheme control: Light -> Dark -> System.
 * Rendered as a single button so it fits the header on mobile too.
 */
@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.css'
})
export class ThemeToggle {

  private themeService = inject(ThemeService);

  preference = this.themeService.preference;
  resolved = this.themeService.resolved;

  cycle(): void {
    this.themeService.cycle();
  }

  get label(): string {
    switch (this.preference() as ThemePreference) {
      case 'light': return 'Light';
      case 'dark': return 'Dark';
      default: return 'System';
    }
  }

  get title(): string {
    const next = this.preference() === 'light'
      ? 'dark'
      : this.preference() === 'dark' ? 'system' : 'light';
    return `Theme: ${this.label}. Switch to ${next}.`;
  }
}
