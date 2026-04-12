import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AttemptService } from '../../core/attempts/attempt.service';
import { QuizSessionDto } from '../../core/attempts/attempt.models';
import { AuthService } from '../../core/auth/auth.service';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionService } from '../../core/questions/question.service';
import { QuizDto } from '../../core/quizzes/quiz.models';
import { QuizService } from '../../core/quizzes/quiz.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { CourseWorkspaceSectionComponent } from '../../shared/ui/course-workspace-section/course-workspace-section.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';

@Component({
  selector: 'app-course-quizzes-page',
  imports: [CourseWorkspaceSectionComponent, ToastStackComponent],
  templateUrl: './course-quizzes-page.component.html',
  styleUrl: './course-quizzes-page.component.scss'
})
export class CourseQuizzesPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly courseService = inject(CourseService);
  private readonly questionService = inject(QuestionService);
  private readonly quizService = inject(QuizService);
  private readonly attemptService = inject(AttemptService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = this.resolveCourseId();
  readonly course = signal<CourseDto | null>(null);
  readonly quizzes = signal<QuizDto[]>([]);
  readonly sessions = signal<QuizSessionDto[]>([]);
  readonly questionCount = signal(0);
  readonly isLoading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly page = signal(0);
  readonly toasts = signal<ToastItem[]>([]);
  readonly canManageCourse = computed(() => {
    const currentCourse = this.course();
    const currentUserId = this.authService.user()?.id;

    return Boolean(currentCourse && currentUserId === currentCourse.ownerUserId);
  });

  readonly filteredQuizzes = computed(() => {
    const normalizedSearch = this.searchTerm().trim().toLocaleLowerCase();

    if (!normalizedSearch) {
      return this.quizzes();
    }

    return this.quizzes().filter((quiz) =>
      quiz.title.toLocaleLowerCase().includes(normalizedSearch) ||
      quiz.categories.some((category) => category.name.toLocaleLowerCase().includes(normalizedSearch))
    );
  });
  readonly pageSize = 3;
  readonly totalPages = computed(() => {
    const length = this.filteredQuizzes().length;
    return length ? Math.ceil(length / this.pageSize) : 0;
  });
  readonly visibleQuizzes = computed(() => {
    const start = this.page() * this.pageSize;
    return this.filteredQuizzes().slice(start, start + this.pageSize);
  });
  readonly pageLabel = computed(() => {
    const totalPages = this.totalPages();
    return totalPages ? `Page ${this.page() + 1} of ${totalPages}` : 'Page 0 of 0';
  });
  readonly canCreateQuiz = computed(() => this.canManageCourse() && this.questionCount() > 0);
  readonly createQuizHint = 'Add at least one question before creating quizzes.';

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

  updateSearchTerm(value: string): void {
    this.searchTerm.set(value);
    this.page.set(0);
  }

  goToPreviousPage(): void {
    this.page.update((page) => Math.max(page - 1, 0));
  }

  goToNextPage(): void {
    const totalPages = this.totalPages();

    if (!totalPages) {
      return;
    }

    this.page.update((page) => Math.min(page + 1, totalPages - 1));
  }

  openQuizEditor(quizId: number): void {
    void this.router.navigate(['/courses', this.courseId, 'quizzes', quizId]);
  }

  createQuiz(): void {
    if (!this.canCreateQuiz()) {
      this.pushToast('Cannot create quiz', this.createQuizHint, 'info');
      return;
    }

    void this.router.navigate(['/courses', this.courseId, 'quizzes', 'new']);
  }

  trackByQuizId(_index: number, quiz: QuizDto): number {
    return quiz.id;
  }

  quizModeLabel(mode: QuizDto['mode']): string {
    return mode.toUpperCase();
  }

  hasSession(quizId: number): boolean {
    return this.sessions().some((session) => session.quizId === quizId);
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId)) {
      this.isLoading.set(false);
      this.pushToast('Invalid link', 'This course link is invalid.', 'error');
      return;
    }

    this.isLoading.set(true);
    this.loadError.set(null);

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      quizzes: this.quizService.fetchQuizzes(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, quizzes }) => {
          this.course.set(course);
          this.quizzes.set(quizzes);
          this.isLoading.set(false);
          this.loadSupplementalData();
        },
        error: (error: unknown) => {
          this.isLoading.set(false);
          const message = extractApiMessage(error) ?? 'Unable to load the quiz workspace right now.';
          this.loadError.set(message);
          this.pushToast('Load failed', message, 'error');
        }
      });
  }

  private loadSupplementalData(): void {
    this.attemptService.fetchSessions(this.courseId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (sessions) => {
          this.sessions.set(sessions);
        },
        error: (error: unknown) => {
          this.pushToast('Sessions unavailable', extractApiMessage(error) ?? 'Unable to load your quiz sessions right now.', 'error');
        }
      });

    if (!this.canManageCourse()) {
      return;
    }

    this.questionService.fetchQuestions(this.courseId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (questions) => {
          this.questionCount.set(questions.length);
        },
        error: (error: unknown) => {
          this.pushToast('Question count unavailable', extractApiMessage(error) ?? 'Unable to load course questions right now.', 'error');
        }
      });
  }

  private resolveCourseId(): number {
    return Number.parseInt(
      this.route.parent?.snapshot.paramMap.get('courseId') ?? this.route.snapshot.paramMap.get('courseId') ?? '',
      10
    );
  }

  private pushToast(title: string, message: string, tone: ToastItem['tone']): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

    this.toastTimeouts.set(id, timeout);
    this.toasts.update((toasts) => [...toasts, { id, title, message, tone }]);
  }
}
