import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { AdminQuizAttemptDto } from '../../core/admin/admin-stats.models';
import { AdminStatsService } from '../../core/admin/admin-stats.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-admin-console-page',
  imports: [DatePipe, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './admin-console-page.component.html',
  styleUrl: './admin-console-page.component.scss'
})
export class AdminConsolePageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly adminStatsService = inject(AdminStatsService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly attempts = signal<AdminQuizAttemptDto[]>([]);
  readonly isLoading = signal(true);
  readonly errorToasts = signal<ToastItem[]>([]);

  constructor() {
    this.loadAttempts();

    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
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

  trackByAttemptId(_index: number, attempt: AdminQuizAttemptDto): number {
    return attempt.attemptId;
  }

  accuracy(attempt: AdminQuizAttemptDto): number {
    if (!attempt.totalQuestions) {
      return 0;
    }

    return Math.round((attempt.correctAnswers / attempt.totalQuestions) * 100);
  }

  private loadAttempts(): void {
    this.isLoading.set(true);

    this.adminStatsService
      .fetchAllQuizAttempts()
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (attempts) => {
          this.attempts.set(attempts);
        },
        error: (error) => {
          this.pushErrorToast(extractApiMessage(error) ?? 'Unable to load admin statistics right now.');
        }
      });
  }

  private pushErrorToast(message: string): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

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
