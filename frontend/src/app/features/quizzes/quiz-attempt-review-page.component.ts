import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AttemptService } from '../../core/attempts/attempt.service';
import { QuizAttemptDetailDto } from '../../core/attempts/attempt.models';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { RichTextHtmlPipe } from '../../shared/rich-text/rich-text-html.pipe';
import { extractRichTextPlainText } from '../../shared/rich-text/rich-text.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-quiz-attempt-review-page',
  imports: [DatePipe, RouterLink, RichTextHtmlPipe, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './quiz-attempt-review-page.component.html',
  styleUrl: './quiz-attempt-review-page.component.scss'
})
export class QuizAttemptReviewPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);
  private readonly attemptService = inject(AttemptService);
  private readonly questionService = inject(QuestionService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly attemptId = Number.parseInt(this.route.snapshot.paramMap.get('attemptId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly attempt = signal<QuizAttemptDetailDto | null>(null);
  readonly questionsById = signal<Record<number, QuestionDto>>({});
  readonly isLoading = signal(true);
  readonly toasts = signal<ToastItem[]>([]);
  readonly expandedDetailQuestionIds = signal<Set<number>>(new Set<number>());

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

  resolvedExplanation(questionId: number): string | null {
    const reviewedQuestion = this.attempt()?.questions.find((question) => question.questionId === questionId);

    if (extractRichTextPlainText(reviewedQuestion?.explanation).length) {
      return reviewedQuestion?.explanation ?? null;
    }

    return this.questionsById()[questionId]?.explanation ?? null;
  }

  resolvedCategories(questionId: number): QuestionDto['categories'] {
    return this.questionsById()[questionId]?.categories ?? [];
  }

  hasReviewDetails(questionId: number): boolean {
    return this.resolvedCategories(questionId).length > 0 || extractRichTextPlainText(this.resolvedExplanation(questionId)).length > 0;
  }

  isDetailsExpanded(questionId: number): boolean {
    return this.expandedDetailQuestionIds().has(questionId);
  }

  toggleDetails(questionId: number): void {
    this.expandedDetailQuestionIds.update((current) => {
      const next = new Set(current);

      if (next.has(questionId)) {
        next.delete(questionId);
      } else {
        next.add(questionId);
      }

      return next;
    });
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId) || !Number.isFinite(this.attemptId)) {
      this.isLoading.set(false);
      this.pushToast('Invalid link', 'This attempt link is invalid.', 'error');
      return;
    }

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      attempt: this.attemptService.fetchAttemptDetail(this.courseId, this.attemptId),
      questions: this.questionService.fetchQuestions(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, attempt, questions }) => {
          this.course.set(course);
          this.attempt.set(attempt);
          this.questionsById.set(Object.fromEntries(questions.map((question) => [question.id, question])));
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
