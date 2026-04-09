import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, ActionButtonComponent, AuthCardComponent, ToastStackComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss'
})
export class LoginPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly isSubmitting = signal(false);
  readonly hasSubmitted = signal(false);
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly errorToasts = signal<ToastItem[]>([]);

  readonly form = this.formBuilder.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128)]]
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.hasSubmitted.set(false);
      this.serverFieldErrors.set({});
    });

    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }
    });
  }

  submit(): void {
    this.hasSubmitted.set(true);

    if (this.form.invalid) {
      return;
    }

    const { email, password } = this.form.getRawValue();

    this.serverFieldErrors.set({});
    this.isSubmitting.set(true);

    this.authService
      .login({ email, password })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          void this.router.navigateByUrl('/app');
        },
        error: (error) => {
          this.serverFieldErrors.set(extractFieldErrors(error));
          this.pushErrorToast(extractApiMessage(error) ?? 'Unable to sign you in right now.');
        }
      });
  }

  dismissToast(id: number): void {
    const timeout = this.toastTimeouts.get(id);

    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    this.errorToasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  hasError(controlName: 'email' | 'password'): boolean {
    const control = this.form.controls[controlName];

    if (this.serverFieldErrors()[controlName]) {
      return true;
    }

    return this.hasSubmitted() && control.invalid;
  }

  getErrorMessage(controlName: 'email' | 'password'): string | null {
    const serverError = this.serverFieldErrors()[controlName];

    if (serverError) {
      return serverError;
    }

    const control = this.form.controls[controlName];

    if (!control.errors || !this.hasSubmitted()) {
      return null;
    }

    if (control.errors['required']) {
      return controlName === 'email' ? 'Email is required.' : 'Password is required.';
    }

    if (control.errors['email']) {
      return 'Enter a valid email address.';
    }

    if (control.errors['minlength']) {
      return 'Password must be at least 12 characters long.';
    }

    if (control.errors['maxlength']) {
      return 'Password cannot be longer than 128 characters.';
    }

    return null;
  }

  private pushErrorToast(message: string): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => {
      this.dismissToast(id);
    }, 5000);

    this.toastTimeouts.set(id, timeout);
    this.errorToasts.update((toasts) => [
      ...toasts,
      {
        id,
        message,
        tone: 'error'
      }
    ]);
  }
}
