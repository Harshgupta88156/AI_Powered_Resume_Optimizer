import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { OauthLauncherService } from '../../../core/services/oauth-launcher.service';
import { ThemeToggle } from '../../../shared/components/theme-toggle/theme-toggle';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ThemeToggle],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class Login {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private oauthLauncher = inject(OauthLauncherService);
  private toast = inject(ToastService);

  loading = signal(false);
  hidePassword = signal(true);
  errorMessage = signal('');

  /** Set by the auth guard when an expired session interrupted a page load. */
  private returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  get email() {
    return this.loginForm.controls.email;
  }

  get password() {
    return this.loginForm.controls.password;
  }

  togglePassword(): void {
    this.hidePassword.update(v => !v);
  }

  login(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login({
      email: (this.email.value ?? '').trim(),
      password: this.password.value ?? ''
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.toast.success('Welcome back!');
        this.router.navigateByUrl(this.returnUrl || '/dashboard');
      },
      error: (err) => {
        this.loading.set(false);
        const message = extractErrorMessage(err, 'Invalid email or password.');
        this.errorMessage.set(message);
        this.toast.error(message);
      }
    });
  }

  loginWithGithub(): void {
    this.oauthLauncher.startGithub(this.returnUrl);
  }
}
