import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import {
  AdminCourseOptionDto,
  AdminQuizAttemptDto,
  AdminQuizAttemptFilters,
  AdminQuizAttemptPageDto,
  AdminQuizOptionDto,
  AdminUserOptionDto
} from '../../core/admin/admin-stats.models';
import { AdminStatsService } from '../../core/admin/admin-stats.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

const PAGE_SIZE = 20;
const PLAYER_SEARCH_MIN_LENGTH = 2;
const PLAYER_SEARCH_DEBOUNCE_MS = 300;
const PLAYER_SUGGESTIONS_BLUR_DELAY_MS = 150;

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
  private playerSearchTimeout: ReturnType<typeof setTimeout> | null = null;
  private playerSuggestionsBlurTimeout: ReturnType<typeof setTimeout> | null = null;

  readonly page = signal<AdminQuizAttemptPageDto | null>(null);
  readonly isLoading = signal(true);
  readonly errorToasts = signal<ToastItem[]>([]);

  // Course/quiz filter options form a drill-down chain: player -> course -> quiz.
  // Selecting a player narrows which courses/quizzes are worth offering (only ones
  // that player actually has attempts in); selecting a course further narrows quizzes.
  readonly courseOptions = signal<AdminCourseOptionDto[]>([]);
  readonly quizOptions = signal<AdminQuizOptionDto[]>([]);
  readonly selectedCourseId = signal<number | null>(null);
  readonly selectedQuizId = signal<number | null>(null);

  readonly playerSearchTerm = signal('');
  readonly playerSuggestions = signal<AdminUserOptionDto[]>([]);
  readonly isSearchingPlayers = signal(false);
  readonly selectedPlayer = signal<AdminUserOptionDto | null>(null);

  readonly attempts = computed(() => this.page()?.items ?? []);
  readonly currentPageNumber = computed(() => this.page()?.pageNumber ?? 0);
  readonly totalPages = computed(() => this.page()?.totalPages ?? 0);
  readonly totalItems = computed(() => this.page()?.totalItems ?? 0);
  readonly hasNext = computed(() => this.page()?.hasNext ?? false);
  readonly hasPrevious = computed(() => this.page()?.hasPrevious ?? false);

  constructor() {
    this.refreshCourseOptions();
    this.loadAttempts(0);

    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }

      if (this.playerSearchTimeout) {
        clearTimeout(this.playerSearchTimeout);
      }

      if (this.playerSuggestionsBlurTimeout) {
        clearTimeout(this.playerSuggestionsBlurTimeout);
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

  trackByCourseOptionId(_index: number, option: AdminCourseOptionDto): number {
    return option.courseId;
  }

  trackByQuizOptionId(_index: number, option: AdminQuizOptionDto): number {
    return option.quizId;
  }

  trackByPlayerOptionId(_index: number, option: AdminUserOptionDto): number {
    return option.userId;
  }

  accuracy(attempt: AdminQuizAttemptDto): number {
    if (!attempt.totalQuestions) {
      return 0;
    }

    return Math.round((attempt.correctAnswers / attempt.totalQuestions) * 100);
  }

  onCourseFilterChange(value: string): void {
    this.selectedCourseId.set(value ? Number(value) : null);
    this.refreshQuizOptions();
  }

  onQuizFilterChange(value: string): void {
    this.selectedQuizId.set(value ? Number(value) : null);
  }

  onPlayerSearchInput(value: string): void {
    this.playerSearchTerm.set(value);

    const selected = this.selectedPlayer();
    if (selected && selected.email !== value) {
      this.selectedPlayer.set(null);
      this.refreshCourseOptions();
    }

    if (this.playerSearchTimeout) {
      clearTimeout(this.playerSearchTimeout);
    }

    const trimmed = value.trim();

    if (trimmed.length < PLAYER_SEARCH_MIN_LENGTH) {
      this.playerSuggestions.set([]);
      return;
    }

    this.playerSearchTimeout = setTimeout(() => this.searchPlayers(trimmed), PLAYER_SEARCH_DEBOUNCE_MS);
  }

  onPlayerSearchBlur(): void {
    this.playerSuggestionsBlurTimeout = setTimeout(() => {
      this.playerSuggestions.set([]);
    }, PLAYER_SUGGESTIONS_BLUR_DELAY_MS);
  }

  selectPlayer(player: AdminUserOptionDto): void {
    this.selectedPlayer.set(player);
    this.playerSearchTerm.set(player.email);
    this.playerSuggestions.set([]);
    this.refreshCourseOptions();
  }

  clearPlayerSelection(): void {
    const hadPlayer = this.selectedPlayer() !== null;

    this.selectedPlayer.set(null);
    this.playerSearchTerm.set('');
    this.playerSuggestions.set([]);

    if (hadPlayer) {
      this.refreshCourseOptions();
    }
  }

  search(): void {
    this.loadAttempts(0);
  }

  clearFilters(): void {
    this.selectedCourseId.set(null);
    this.selectedQuizId.set(null);
    this.clearPlayerSelection();
    this.refreshCourseOptions();
    this.loadAttempts(0);
  }

  previousPage(): void {
    if (!this.hasPrevious() || this.isLoading()) {
      return;
    }

    this.loadAttempts(this.currentPageNumber() - 1);
  }

  nextPage(): void {
    if (!this.hasNext() || this.isLoading()) {
      return;
    }

    this.loadAttempts(this.currentPageNumber() + 1);
  }

  // Course options depend only on the selected player. Refreshing them always
  // also refreshes quiz options underneath, since quiz options depend on both
  // the player and the course.
  private refreshCourseOptions(): void {
    const userId = this.selectedPlayer()?.userId ?? null;

    this.adminStatsService
      .fetchCourseOptions(userId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (options) => {
          this.courseOptions.set(options);

          const currentCourseId = this.selectedCourseId();
          const stillValid = currentCourseId == null || options.some((course) => course.courseId === currentCourseId);

          if (!stillValid) {
            this.selectedCourseId.set(null);
          }

          this.refreshQuizOptions();
        },
        error: (error) => this.pushErrorToast(extractApiMessage(error) ?? 'Unable to load course filter options.')
      });
  }

  // Quiz options depend on both the selected player and the selected course.
  private refreshQuizOptions(): void {
    const userId = this.selectedPlayer()?.userId ?? null;
    const courseId = this.selectedCourseId();

    this.adminStatsService
      .fetchQuizOptions(userId, courseId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (options) => {
          this.quizOptions.set(options);

          const currentQuizId = this.selectedQuizId();
          const stillValid = currentQuizId == null || options.some((quiz) => quiz.quizId === currentQuizId);

          if (!stillValid) {
            this.selectedQuizId.set(null);
          }
        },
        error: (error) => this.pushErrorToast(extractApiMessage(error) ?? 'Unable to load quiz filter options.')
      });
  }

  private searchPlayers(term: string): void {
    this.isSearchingPlayers.set(true);

    this.adminStatsService
      .searchPlayers(term)
      .pipe(
        finalize(() => this.isSearchingPlayers.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (players) => this.playerSuggestions.set(players),
        error: () => this.playerSuggestions.set([])
      });
  }

  private loadAttempts(pageNumber: number): void {
    this.isLoading.set(true);

    const filters: AdminQuizAttemptFilters = {
      courseId: this.selectedCourseId(),
      quizId: this.selectedQuizId(),
      userId: this.selectedPlayer()?.userId ?? null
    };

    this.adminStatsService
      .fetchQuizAttempts(pageNumber, PAGE_SIZE, filters)
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (page) => {
          this.page.set(page);
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
