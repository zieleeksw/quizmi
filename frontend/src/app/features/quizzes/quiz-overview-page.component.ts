import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AttemptService } from '../../core/attempts/attempt.service';
import { QuizAttemptDetailDto, QuizAttemptDto, QuizSessionDto } from '../../core/attempts/attempt.models';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuizDto } from '../../core/quizzes/quiz.models';
import { QuizService } from '../../core/quizzes/quiz.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

export interface DomainStat {
  categoryId: number;
  categoryName: string;
  totalQuestions: number;
  correctAnswers: number;
  percentage: number;
}

@Component({
  selector: 'app-quiz-overview-page',
  imports: [DatePipe, RouterLink, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './quiz-overview-page.component.html',
  styleUrl: './quiz-overview-page.component.scss'
})
export class QuizOverviewPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
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
  readonly sessions = signal<QuizSessionDto[]>([]);
  readonly attempts = signal<QuizAttemptDto[]>([]);
  readonly questions = signal<QuestionDto[]>([]);
  readonly attemptDetailsById = signal<Record<number, QuizAttemptDetailDto>>({});
  readonly expandedAttemptId = signal<number | null>(null);
  readonly loadingDetailsForAttemptId = signal<number | null>(null);
  readonly isLoading = signal(true);
  readonly isStartingSession = signal(false);
  readonly toasts = signal<ToastItem[]>([]);
  readonly canManageCourse = computed(() => this.course()?.canManage ?? false);

  readonly quizAttempts = computed(() =>
    this.attempts()
      .filter((attempt) => attempt.quizId === this.quizId)
      .sort((left, right) => right.finishedAt.localeCompare(left.finishedAt))
  );

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

  hasSession(): boolean {
    return this.sessions().some((session) => session.quizId === this.quizId);
  }

  quizModeLabel(): string {
    return this.quiz()?.mode.toUpperCase() ?? '';
  }

  quizSummary(): string {
    const quiz = this.quiz();

    if (!quiz) {
      return '';
    }

    if (quiz.mode === 'manual') {
      return `Manual quiz with ${quiz.resolvedQuestionCount} selected question${quiz.resolvedQuestionCount === 1 ? '' : 's'}.`;
    }

    if (quiz.mode === 'random') {
      return `Random quiz drawing ${quiz.randomCount ?? quiz.resolvedQuestionCount} question${(quiz.randomCount ?? quiz.resolvedQuestionCount) === 1 ? '' : 's'} from the course bank.`;
    }

    return `Category quiz pulling ${quiz.randomCount ?? quiz.resolvedQuestionCount} question${(quiz.randomCount ?? quiz.resolvedQuestionCount) === 1 ? '' : 's'} from ${quiz.categories.length} selected categor${quiz.categories.length === 1 ? 'y' : 'ies'}.`;
  }

  attemptPercentage(attempt: QuizAttemptDto): number {
    if (!attempt.totalQuestions) {
      return 0;
    }

    return Math.round((attempt.correctAnswers / attempt.totalQuestions) * 100);
  }

  toggleAttempt(attemptId: number): void {
    if (this.expandedAttemptId() === attemptId) {
      this.expandedAttemptId.set(null);
      return;
    }

    this.expandedAttemptId.set(attemptId);

    if (!this.attemptDetailsById()[attemptId]) {
      this.loadingDetailsForAttemptId.set(attemptId);
      this.attemptService.fetchAttemptDetail(this.courseId, attemptId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (detail) => {
            this.attemptDetailsById.update((records) => ({ ...records, [attemptId]: detail }));
            this.loadingDetailsForAttemptId.set(null);
          },
          error: (error) => {
            this.loadingDetailsForAttemptId.set(null);
            this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load attempt details.', 'error');
          }
        });
    }
  }

  domainStatsFor(attemptId: number): DomainStat[] {
    const detail = this.attemptDetailsById()[attemptId];
    if (!detail) return [];

    const statsMap = new Map<number, DomainStat>();
    const allQuestions = this.questions();

    for (const qReview of detail.questions) {
      const actualQuestion = allQuestions.find((q) => q.id === qReview.questionId);
      if (!actualQuestion) continue;

      for (const category of actualQuestion.categories) {
        let stat = statsMap.get(category.id);
        if (!stat) {
          stat = {
            categoryId: category.id,
            categoryName: category.name,
            totalQuestions: 0,
            correctAnswers: 0,
            percentage: 0
          };
          statsMap.set(category.id, stat);
        }

        stat.totalQuestions++;
        if (qReview.answeredCorrectly) {
          stat.correctAnswers++;
        }
      }
    }

    return Array.from(statsMap.values())
      .map((stat) => ({
        ...stat,
        percentage: Math.round((stat.correctAnswers / stat.totalQuestions) * 100)
      }))
      .sort((a, b) => b.percentage - a.percentage);
  }

  openAttempt(attemptId: number): void {
    void this.router.navigate(['/courses', this.courseId, 'attempts', attemptId]);
  }

  startQuiz(): void {
    this.isStartingSession.set(true);
    this.attemptService.createOrResumeSession(this.courseId, this.quizId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (session) => {
          this.sessions.update((sessions) => {
            const remaining = sessions.filter((existingSession) => existingSession.id !== session.id);
            return [...remaining, session];
          });
          this.isStartingSession.set(false);
          void this.router.navigate(['/courses', this.courseId, 'quizzes', this.quizId, 'play']);
        },
        error: (error) => {
          this.isStartingSession.set(false);
          this.pushToast('Unable to start quiz', extractApiMessage(error) ?? 'Unable to start this quiz right now.', 'error');
        }
      });
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId) || !Number.isFinite(this.quizId)) {
      this.isLoading.set(false);
      this.pushToast('Invalid link', 'This quiz link is invalid.', 'error');
      return;
    }

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      quiz: this.quizService.fetchQuiz(this.courseId, this.quizId),
      sessions: this.attemptService.fetchSessions(this.courseId),
      attempts: this.attemptService.fetchAttempts(this.courseId),
      questions: this.questionService.fetchQuestions(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, quiz, sessions, attempts, questions }) => {
          this.course.set(course);
          this.quiz.set(quiz);
          this.sessions.set(sessions);
          this.attempts.set(attempts);
          this.questions.set(questions);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load this quiz right now.', 'error');
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
