import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { ThemeToggle } from '../../shared/components/theme-toggle/theme-toggle';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, ThemeToggle],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {

  @Output() menuToggle = new EventEmitter<void>();

  private authService = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);

  currentUser = this.authService.currentUser;
  loggingOut = signal(false);

  get initials(): string {
    const name = this.currentUser()?.name ?? '';
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(part => part[0]?.toUpperCase())
      .join('') || 'U';
  }

  toggleMenu(): void {
    this.menuToggle.emit();
  }

  logout(): void {
    if (this.loggingOut()) return;
    this.loggingOut.set(true);

    // AuthService already swallows a failing logout call, so `next` always runs
    // and the user is never trapped in the app by a dead backend.
    this.authService.logout().subscribe({
      next: () => this.finishLogout(),
      error: () => this.finishLogout()
    });
  }

  private finishLogout(): void {
    this.authService.clearSession();
    this.loggingOut.set(false);
    this.toast.info('You have been signed out.');
    this.router.navigate(['/login']);
  }
}
