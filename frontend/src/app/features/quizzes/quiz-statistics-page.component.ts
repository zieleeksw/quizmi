import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AttemptService } from '../../core/attempts/attempt.service';
import { QuizAttemptDetailDto, QuizAttemptDto } from '../../core/attempts/attempt.models';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionService } from '../../core/questions/question.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuizDto } from '../../core/quizzes/quiz.models';
import { QuizService } from '../../core/quizzes/quiz.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

export interface CategoryStat {
  categoryId: number;
  categoryName: string;
  totalAttempts: number;
  correctAttempts: number;
  accuracy: number;
  status: 'green' | 'amber' | 'red';
}

export interface QuestionStat {
  questionId: number;
  prompt: string;
  categoryNames: string;
  attemptsCount: number;
  correctCount: number;
  accuracy: number;
}

@Component({
  selector: 'app-quiz-statistics-page',
  imports: [DatePipe, RouterLink, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './quiz-statistics-page.component.html',
  styleUrl: './quiz-statistics-page.component.scss'
})
export class QuizStatisticsPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);
  private readonly quizService = inject(QuizService);
  private readonly attemptService = inject(AttemptService);
  private readonly questionService = inject(QuestionService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly quizId = Number.parseInt(this.route.snapshot.paramMap.get('quizId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly quiz = signal<QuizDto | null>(null);
  readonly attempts = signal<QuizAttemptDto[]>([]);
  readonly attemptReviews = signal<QuizAttemptDetailDto[]>([]);
  readonly questions = signal<QuestionDto[]>([]);
  readonly isLoading = signal(true);
  readonly toasts = signal<ToastItem[]>([]);

  readonly quizAttempts = computed(() =>
    this.attempts()
      .filter((attempt) => attempt.quizId === this.quizId)
      .sort((left, right) => right.finishedAt.localeCompare(left.finishedAt))
  );

  readonly averageAccuracy = computed(() => {
    const attempts = this.quizAttempts();

    if (!attempts.length) {
      return 0;
    }

    const percentageSum = attempts.reduce((sum, attempt) => sum + this.attemptPercentage(attempt), 0);
    return Math.round(percentageSum / attempts.length);
  });

  readonly bestAccuracy = computed(() =>
    this.quizAttempts().reduce((best, attempt) => Math.max(best, this.attemptPercentage(attempt)), 0)
  );

  readonly questionStats = computed<QuestionStat[]>(() => {
    const reviews = this.attemptReviews();
    const allQuestions = this.questions();
    const map = new Map<number, QuestionStat>();

    for (const review of reviews) {
      for (const qRev of review.questions) {
        if (!map.has(qRev.questionId)) {
          const actualQ = allQuestions.find((q) => q.id === qRev.questionId);
          map.set(qRev.questionId, {
            questionId: qRev.questionId,
            prompt: qRev.prompt,
            categoryNames: actualQ ? actualQ.categories.map((c) => c.name).join(', ') : 'Unknown',
            attemptsCount: 0,
            correctCount: 0,
            accuracy: 0
          });
        }
        const stat = map.get(qRev.questionId)!;
        stat.attemptsCount++;
        if (qRev.answeredCorrectly) {
          stat.correctCount++;
        }
      }
    }

    return Array.from(map.values())
      .map((s) => ({
        ...s,
        accuracy: Math.round((s.correctCount / s.attemptsCount) * 100) || 0
      }))
      .sort((a, b) => a.accuracy - b.accuracy); // Sort from worst to best
  });

  readonly categoryStats = computed<CategoryStat[]>(() => {
    const reviews = this.attemptReviews();
    const allQuestions = this.questions();
    const map = new Map<number, CategoryStat>();

    for (const review of reviews) {
      for (const qRev of review.questions) {
        const actualQ = allQuestions.find((q) => q.id === qRev.questionId);
        if (!actualQ) continue;

        for (const cat of actualQ.categories) {
          if (!map.has(cat.id)) {
            map.set(cat.id, {
              categoryId: cat.id,
              categoryName: cat.name,
              totalAttempts: 0,
              correctAttempts: 0,
              accuracy: 0,
              status: 'green'
            });
          }
          const stat = map.get(cat.id)!;
          stat.totalAttempts++;
          if (qRev.answeredCorrectly) {
            stat.correctAttempts++;
          }
        }
      }
    }

    return Array.from(map.values())
      .map((s) => {
        const accuracy = Math.round((s.correctAttempts / s.totalAttempts) * 100) || 0;
        let status: 'green' | 'amber' | 'red' = 'green';
        if (accuracy < 50) status = 'red';
        else if (accuracy < 80) status = 'amber';

        return { ...s, accuracy, status };
      })
      .sort((a, b) => a.accuracy - b.accuracy); // Sort lowest accuracy first
  });

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

  attemptPercentage(attempt: QuizAttemptDto): number {
    if (!attempt.totalQuestions) {
      return 0;
    }

    return Math.round((attempt.correctAnswers / attempt.totalQuestions) * 100);
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId) || !Number.isFinite(this.quizId)) {
      this.isLoading.set(false);
      this.pushToast('Invalid link', 'This statistics link is invalid.', 'error');
      return;
    }

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      quiz: this.quizService.fetchQuiz(this.courseId, this.quizId),
      attempts: this.attemptService.fetchAttempts(this.courseId),
      attemptReviews: this.attemptService.fetchAttemptReviews(this.courseId),
      questions: this.questionService.fetchQuestions(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, quiz, attempts, attemptReviews, questions }) => {
          this.course.set(course);
          this.quiz.set(quiz);
          this.attempts.set(attempts);
          this.attemptReviews.set(attemptReviews.filter(r => r.quizId === this.quizId));
          this.questions.set(questions);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load quiz statistics right now.', 'error');
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
