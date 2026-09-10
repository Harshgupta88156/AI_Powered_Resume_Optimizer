import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { OauthLauncherService } from '../../../core/services/oauth-launcher.service';
import { ThemeToggle } from '../../../shared/components/theme-toggle/theme-toggle';
import { ToastService } from '../../../core/services/toast.service';
import { extractErrorMessage } from '../../../core/utils/error.util';

function passwordMatchValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const password = control.get('password')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (!password || !confirmPassword) {
      return null;
    }

    return password === confirmPassword ? null : { passwordMismatch: true };
  };
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ThemeToggle],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class Register {

  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);
  private oauthLauncher = inject(OauthLauncherService);
  private toast = inject(ToastService);

  loading = signal(false);
  hidePassword = signal(true);
  hideConfirmPassword = signal(true);
  errorMessage = signal('');

  registerForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).+$/)
      ]
    ],
    confirmPassword: ['', Validators.required]
  }, {
    validators: passwordMatchValidator()
  });

  get name() { return this.registerForm.controls.name; }
  get email() { return this.registerForm.controls.email; }
  get password() { return this.registerForm.controls.password; }
  get confirmPassword() { return this.registerForm.controls.confirmPassword; }

  togglePassword(): void {
    this.hidePassword.update(v => !v);
  }

  toggleConfirmPassword(): void {
    this.hideConfirmPassword.update(v => !v);
  }

  getPasswordStrength(): string {
    const password = this.password.value ?? '';

    if (password.length < 8) {
      return 'Weak';
    }

    let score = 0;
    if (/[A-Z]/.test(password)) score++;
    if (/[a-z]/.test(password)) score++;
    if (/\d/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    if (password.length >= 12) score++;

    if (score <= 2) return 'Weak';
    if (score <= 4) return 'Medium';
    return 'Strong';
  }

  register(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.register({
      name: (this.name.value ?? '').trim(),
      email: (this.email.value ?? '').trim(),
      password: this.password.value ?? ''
    }).subscribe({
      next: () => {
        this.loading.set(false);
        // /register already returns a full session, so send them straight in
        // rather than bouncing to the login form they just filled out.
        this.toast.success('Account created. Welcome aboard!');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        const message = extractErrorMessage(err, 'Registration failed. Please try again.');
        this.errorMessage.set(message);
        this.toast.error(message);
      }
    });
  }

  registerWithGithub(): void {
    this.oauthLauncher.startGithub(null);
  }
}
