import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AttemptService } from '../../core/attempts/attempt.service';
import { QuizAttemptDetailDto } from '../../core/attempts/attempt.models';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-quiz-attempt-review-page',
  imports: [DatePipe, RouterLink, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './quiz-attempt-review-page.component.html',
  styleUrl: './quiz-attempt-review-page.component.scss'
})
export class QuizAttemptReviewPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);
  private readonly attemptService = inject(AttemptService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly attemptId = Number.parseInt(this.route.snapshot.paramMap.get('attemptId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly attempt = signal<QuizAttemptDetailDto | null>(null);
  readonly isLoading = signal(true);
  readonly toasts = signal<ToastItem[]>([]);

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }
    });

    this.loadPage();
  }

  dismissToast(id: number): void {
    const timeout = this.toastTimeouts.get(id);

    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  isSelected(questionId: number, answerId: number): boolean {
    return this.attempt()?.questions.find((question) => question.questionId === questionId)?.selectedAnswerIds.includes(answerId) ?? false;
  }

  isCorrect(questionId: number, answerId: number): boolean {
    return this.attempt()?.questions.find((question) => question.questionId === questionId)?.correctAnswerIds.includes(answerId) ?? false;
  }

  isSelectedWrong(questionId: number, answerId: number): boolean {
    return this.isSelected(questionId, answerId) && !this.isCorrect(questionId, answerId);
  }

  isSelectedCorrect(questionId: number, answerId: number): boolean {
    return this.isSelected(questionId, answerId) && this.isCorrect(questionId, answerId);
  }

  isMissedCorrect(questionId: number, answerId: number): boolean {
    return !this.isSelected(questionId, answerId) && this.isCorrect(questionId, answerId);
  }

  answerStateLabel(questionId: number, answerId: number): string | null {
    if (this.isSelectedCorrect(questionId, answerId)) {
      return 'Your answer is correct';
    }

    if (this.isSelectedWrong(questionId, answerId)) {
      return 'Your answer is incorrect';
    }

    if (this.isMissedCorrect(questionId, answerId)) {
      return 'Correct answer';
    }

    return null;
  }

  reviewHint(questionId: number): string | null {
    const question = this.attempt()?.questions.find((entry) => entry.questionId === questionId);

    if (!question || question.answeredCorrectly) {
      return null;
    }

    const selectedWrongCount = question.selectedAnswerIds.filter((answerId) => !question.correctAnswerIds.includes(answerId)).length;
    const missedCorrectCount = question.correctAnswerIds.filter((answerId) => !question.selectedAnswerIds.includes(answerId)).length;

    if (selectedWrongCount > 0 && missedCorrectCount > 0) {
      return 'You selected an incorrect answer and missed a correct one.';
    }

    if (selectedWrongCount > 0) {
      return selectedWrongCount === 1
        ? 'You selected an incorrect answer.'
        : `You selected ${selectedWrongCount} incorrect answers.`;
    }

    if (missedCorrectCount > 0) {
      return missedCorrectCount === 1
        ? 'You missed a correct answer.'
        : `You missed ${missedCorrectCount} correct answers.`;
    }

    return null;
  }

  answerLabel(index: number): string {
    return String.fromCharCode(65 + index);
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId) || !Number.isFinite(this.attemptId)) {
      this.isLoading.set(false);
      this.pushToast('Invalid link', 'This attempt link is invalid.', 'error');
      return;
    }

    this.courseService.fetchCourse(this.courseId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (course) => this.course.set(course),
        error: () => undefined
      });

    this.attemptService.fetchAttemptDetail(this.courseId, this.attemptId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (attempt) => {
          this.attempt.set(attempt);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load this attempt review right now.', 'error');
        }
      });
  }

  private pushToast(title: string, message: string, tone: ToastItem['tone']): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

    this.toastTimeouts.set(id, timeout);
    this.toasts.update((toasts) => [...toasts, { id, title, message, tone }]);
  }
}
