import { finalize, forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AttemptService } from '../../core/attempts/attempt.service';
import { QuizAttemptDto, QuizAttemptAnswerRequest, QuizSessionDto } from '../../core/attempts/attempt.models';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { QuizDto } from '../../core/quizzes/quiz.models';
import { QuizService } from '../../core/quizzes/quiz.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-quiz-play-page',
  imports: [RouterLink, ActionButtonComponent, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './quiz-play-page.component.html',
  styleUrl: './quiz-play-page.component.scss'
})
export class QuizPlayPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly quizService = inject(QuizService);
  private readonly questionService = inject(QuestionService);
  private readonly attemptService = inject(AttemptService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly quizId = Number.parseInt(this.route.snapshot.paramMap.get('quizId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly quiz = signal<QuizDto | null>(null);
  readonly session = signal<QuizSessionDto | null>(null);
  readonly questionsById = signal<Record<number, QuestionDto>>({});
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly isSubmitting = signal(false);
  readonly completedAttempt = signal<QuizAttemptDto | null>(null);
  readonly toasts = signal<ToastItem[]>([]);

  readonly orderedQuestions = computed(() => {
    const questionMap = this.questionsById();
    return (this.session()?.questionIds ?? [])
      .map((questionId) => questionMap[questionId])
      .filter((question): question is QuestionDto => Boolean(question));
  });
  readonly currentIndex = computed(() => this.session()?.currentIndex ?? 0);
  readonly currentQuestion = computed(() => this.orderedQuestions()[this.currentIndex()] ?? null);
  readonly answersMap = computed(() => this.session()?.answers ?? {});
  readonly currentQuestionAnswered = computed(() => {
    const question = this.currentQuestion();
    return question ? (this.answersMap()[question.id] ?? []).length > 0 : false;
  });
  readonly allQuestionsAnswered = computed(() =>
    this.orderedQuestions().every((question) => (this.answersMap()[question.id] ?? []).length > 0)
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

  isSelected(questionId: number, answerId: number): boolean {
    return (this.answersMap()[questionId] ?? []).includes(answerId);
  }

  toggleAnswer(answerId: number): void {
    const question = this.currentQuestion();
    const session = this.session();

    if (!question || !session) {
      return;
    }

    const answers = { ...session.answers };
    const current = new Set(answers[question.id] ?? []);

    if (current.has(answerId)) {
      current.delete(answerId);
    } else {
      current.add(answerId);
    }

    if (current.size) {
      answers[question.id] = [...current];
    } else {
      delete answers[question.id];
    }

    this.persistSession(session.currentIndex, answers);
  }

  openQuestion(index: number): void {
    const session = this.session();

    if (!session) {
      return;
    }

    this.persistSession(index, { ...session.answers });
  }

  goToPreviousQuestion(): void {
    this.openQuestion(Math.max(this.currentIndex() - 1, 0));
  }

  goToNextQuestion(): void {
    this.openQuestion(Math.min(this.currentIndex() + 1, this.orderedQuestions().length - 1));
  }

  finishAttempt(): void {
    const session = this.session();

    if (!session || !this.allQuestionsAnswered()) {
      this.pushToast('Incomplete quiz', 'Answer every quiz question before finishing the attempt.', 'error');
      return;
    }

    this.isSubmitting.set(true);

    this.attemptService.createAttempt(this.courseId, this.quizId, this.toAnswerRequests(session.answers))
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (attempt) => {
          this.completedAttempt.set(attempt);
          this.pushToast('Attempt finished', 'Your quiz result has been saved.', 'success');
        },
        error: (error) => {
          this.pushToast('Finish failed', extractApiMessage(error) ?? 'Unable to finish this quiz right now.', 'error');
        }
      });
  }

  playAgain(): void {
    void this.router.navigate(['/courses', this.courseId, 'quizzes']);
  }

  openReview(): void {
    const attempt = this.completedAttempt();

    if (!attempt) {
      return;
    }

    void this.router.navigate(['/courses', this.courseId, 'attempts', attempt.id]);
  }

  answerLabel(index: number): string {
    return String.fromCharCode(65 + index);
  }

  currentOptions(): QuestionDto['answers'] {
    const question = this.currentQuestion();
    const quiz = this.quiz();

    if (!question) {
      return [];
    }

    if (quiz?.answerOrder !== 'random') {
      return question.answers;
    }

    return [...question.answers].sort((left, right) => this.hashSeed(`${question.id}:${left.id}`) - this.hashSeed(`${question.id}:${right.id}`));
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
      questions: this.questionService.fetchQuestions(this.courseId),
      session: this.attemptService.createOrResumeSession(this.courseId, this.quizId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, quiz, questions, session }) => {
          this.course.set(course);
          this.quiz.set(quiz);
          this.questionsById.set(Object.fromEntries(questions.map((question) => [question.id, question])));
          this.session.set(session);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to open this quiz right now.', 'error');
        }
      });
  }

  private persistSession(currentIndex: number, answers: Record<string, number[]>): void {
    const session = this.session();

    if (!session) {
      return;
    }

    this.isSaving.set(true);
    const optimisticSession: QuizSessionDto = {
      ...session,
      currentIndex,
      answers
    };
    this.session.set(optimisticSession);

    this.attemptService.updateSession(this.courseId, this.quizId, {
      currentIndex,
      answers: this.toAnswerRequests(answers)
    })
      .pipe(
        finalize(() => this.isSaving.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (updatedSession) => this.session.set(updatedSession),
        error: (error) => {
          this.pushToast('Save failed', extractApiMessage(error) ?? 'Unable to save quiz progress right now.', 'error');
        }
      });
  }

  private toAnswerRequests(answers: Record<string, number[]>): QuizAttemptAnswerRequest[] {
    return Object.entries(answers).map(([questionId, answerIds]) => ({
      questionId: Number.parseInt(questionId, 10),
      answerIds
    }));
  }

  private hashSeed(seed: string): number {
    let hash = 0;

    for (let index = 0; index < seed.length; index++) {
      hash = (hash * 31 + seed.charCodeAt(index)) >>> 0;
    }

    return hash;
  }

  private pushToast(title: string, message: string, tone: ToastItem['tone']): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

    this.toastTimeouts.set(id, timeout);
    this.toasts.update((toasts) => [...toasts, { id, title, message, tone }]);
  }
}
