import { AbstractControl, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { RegistrationService } from '../../core/auth/registration.service';
import { UserDto } from '../../core/auth/registration.models';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { AuthCardComponent } from '../../shared/ui/auth-card/auth-card.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';

const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (!password || !confirmPassword) {
    return null;
  }

  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, ActionButtonComponent, AuthCardComponent, ToastStackComponent],
  templateUrl: './register-page.component.html',
  styleUrl: './register-page.component.scss'
})
export class RegisterPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly registrationService = inject(RegistrationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly isSubmitting = signal(false);
  readonly hasSubmitted = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly createdUser = signal<UserDto | null>(null);
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly errorToasts = signal<ToastItem[]>([]);

  readonly form = this.formBuilder.nonNullable.group(
    {
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(128)]],
      confirmPassword: ['', [Validators.required]]
    },
    {
      validators: [passwordMatchValidator]
    }
  );

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.hasSubmitted.set(false);
      this.successMessage.set(null);
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

    this.successMessage.set(null);
    this.serverFieldErrors.set({});
    this.isSubmitting.set(true);

    this.registrationService
      .register({ email, password })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (user) => {
          this.createdUser.set(user);
          this.successMessage.set(`Account created for ${user.email}. Assigned role: ${user.role}.`);
          this.form.reset();
          this.hasSubmitted.set(false);
          this.serverFieldErrors.set({});
        },
        error: (error) => {
          this.serverFieldErrors.set(extractFieldErrors(error));
          this.pushErrorToast(extractApiMessage(error) ?? 'Unable to create your account right now.');
        }
      });
  }

  clearSuccess(): void {
    this.createdUser.set(null);
    this.successMessage.set(null);
  }

  dismissToast(id: number): void {
    const timeout = this.toastTimeouts.get(id);

    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    this.errorToasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  hasError(controlName: 'email' | 'password' | 'confirmPassword'): boolean {
    const control = this.form.controls[controlName];

    if (controlName !== 'confirmPassword' && this.serverFieldErrors()[controlName]) {
      return true;
    }

    if (controlName === 'confirmPassword' && this.form.hasError('passwordMismatch')) {
      return this.hasSubmitted();
    }

    return this.hasSubmitted() && control.invalid;
  }

  getErrorMessage(controlName: 'email' | 'password' | 'confirmPassword'): string | null {
    if (controlName !== 'confirmPassword') {
      const serverError = this.serverFieldErrors()[controlName];

      if (serverError) {
        return serverError;
      }
    }

    const control = this.form.controls[controlName];

    if (!control.errors && !(controlName === 'confirmPassword' && this.form.hasError('passwordMismatch'))) {
      return null;
    }

    if (!this.hasSubmitted()) {
      return null;
    }

    if (control.errors?.['required']) {
      if (controlName === 'email') {
        return 'Email is required.';
      }

      return controlName === 'password' ? 'Password is required.' : 'Please confirm your password.';
    }

    if (control.errors?.['email']) {
      return 'Enter a valid email address.';
    }

    if (control.errors?.['minlength']) {
      return 'Password must be at least 12 characters long.';
    }

    if (control.errors?.['maxlength']) {
      return 'Password cannot be longer than 128 characters.';
    }

    if (controlName === 'confirmPassword' && this.form.hasError('passwordMismatch')) {
      return 'Passwords must match.';
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
