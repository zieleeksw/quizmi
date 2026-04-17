import { finalize, forkJoin, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, HostListener, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CategoryDto } from '../../core/categories/category.models';
import { CategoryService } from '../../core/categories/category.service';
import { PendingChangesDialogService } from '../../core/navigation/pending-changes-dialog.service';
import { PendingChangesAware } from '../../core/navigation/pending-changes.guard';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { QuizDto, QuizMode, QuizOrderMode, QuizVersionDto, SaveQuizRequest } from '../../core/quizzes/quiz.models';
import { QuizService } from '../../core/quizzes/quiz.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { RichTextHtmlPipe } from '../../shared/rich-text/rich-text-html.pipe';
import { extractRichTextPlainText } from '../../shared/rich-text/rich-text.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

type QuizDraftSnapshot = {
  title: string;
  mode: QuizMode;
  randomCount: number | null;
  questionOrder: QuizOrderMode;
  answerOrder: QuizOrderMode;
  questionIds: number[];
  categoryIds: number[];
};

@Component({
  selector: 'app-quiz-editor-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, ActionButtonComponent, RichTextHtmlPipe, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './quiz-editor-page.component.html',
  styleUrl: './quiz-editor-page.component.scss'
})
export class QuizEditorPageComponent implements PendingChangesAware {
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly categoryService = inject(CategoryService);
  private readonly questionService = inject(QuestionService);
  private readonly quizService = inject(QuizService);
  private readonly pendingChangesDialog = inject(PendingChangesDialogService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly rawQuizId = this.route.snapshot.paramMap.get('quizId');
  readonly quizId = this.rawQuizId ? Number.parseInt(this.rawQuizId, 10) : NaN;
  readonly isEditing = Number.isFinite(this.quizId);

  readonly course = signal<CourseDto | null>(null);
  readonly categories = signal<CategoryDto[]>([]);
  readonly questions = signal<QuestionDto[]>([]);
  readonly quiz = signal<QuizDto | null>(null);
  readonly versions = signal<QuizVersionDto[]>([]);
  readonly selectedQuestionIds = signal<number[]>([]);
  readonly selectedCategoryIds = signal<number[]>([]);
  readonly draggedQuestionId = signal<number | null>(null);
  readonly draggedQuestionSource = signal<'bank' | 'selected' | null>(null);
  readonly dropTargetIndex = signal<number | null>(null);
  readonly searchTerm = signal('');
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly toasts = signal<ToastItem[]>([]);
  readonly canManageCourse = computed(() => this.course()?.canManage ?? false);

  readonly quizModes: QuizMode[] = ['manual', 'random', 'category'];
  readonly orderModes: QuizOrderMode[] = ['fixed', 'random'];

  readonly form = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(120)]],
    mode: ['manual' as QuizMode, Validators.required],
    randomCount: [10, [Validators.required, Validators.min(1)]],
    questionOrder: ['fixed' as QuizOrderMode, Validators.required],
    answerOrder: ['fixed' as QuizOrderMode, Validators.required]
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }
    });

    this.loadPage();
  }

  readonly filteredQuestions = signal<QuestionDto[]>([]);

  dismissToast(id: number): void {
    const timeout = this.toastTimeouts.get(id);

    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  currentMode(): QuizMode {
    return this.form.controls.mode.getRawValue();
  }

  bankQuestions(): QuestionDto[] {
    const normalizedSearch = this.searchTerm().trim().toLocaleLowerCase();
    const availableQuestions = this.questions().filter((question) => !this.selectedQuestionIds().includes(question.id));

    return normalizedSearch
      ? availableQuestions.filter((question) =>
        extractRichTextPlainText(question.prompt).toLocaleLowerCase().includes(normalizedSearch) ||
        question.categories.some((category) => category.name.toLocaleLowerCase().includes(normalizedSearch))
      )
      : availableQuestions;
  }

  updateSearchTerm(value: string): void {
    this.searchTerm.set(value);
  }

  shouldShowQuestionCount(): boolean {
    const mode = this.currentMode();
    return mode === 'random' || mode === 'category';
  }

  questionCountLabel(): string {
    return this.currentMode() === 'category'
      ? 'Question count from selected categories'
      : 'Random question count';
  }

  toggleQuestionSelection(questionId: number): void {
    this.selectedQuestionIds.update((selectedQuestionIds) =>
      selectedQuestionIds.includes(questionId)
        ? selectedQuestionIds.filter((id) => id !== questionId)
        : [...selectedQuestionIds, questionId]
    );
  }

  removeSelectedQuestion(questionId: number): void {
    this.selectedQuestionIds.update((selectedQuestionIds) => selectedQuestionIds.filter((id) => id !== questionId));
  }

  startDraggingQuestion(questionId: number, source: 'bank' | 'selected', event: DragEvent): void {
    this.draggedQuestionId.set(questionId);
    this.draggedQuestionSource.set(source);
    event.dataTransfer?.setData('text/plain', String(questionId));
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
    }
  }

  endDraggingQuestion(): void {
    this.draggedQuestionId.set(null);
    this.draggedQuestionSource.set(null);
    this.dropTargetIndex.set(null);
  }

  allowQuestionDrop(event: DragEvent): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
  }

  markDropIndex(index: number, event: DragEvent): void {
    event.preventDefault();
    this.dropTargetIndex.set(index);
  }

  clearDropMarker(): void {
    this.dropTargetIndex.set(null);
  }

  onSelectedItemDragOver(index: number, event: DragEvent): void {
    event.preventDefault();
    const element = event.currentTarget as HTMLElement;
    const rect = element.getBoundingClientRect();
    const midY = rect.top + rect.height / 2;
    this.dropTargetIndex.set(event.clientY < midY ? index : index + 1);
  }

  dropQuestionAtMarker(event: DragEvent): void {
    event.preventDefault();
    const index = this.dropTargetIndex();
    if (index !== null) {
      this.insertDraggedQuestionAt(index);
    }
  }

  dropQuestionAt(index: number, event: DragEvent): void {
    event.preventDefault();
    this.insertDraggedQuestionAt(index);
  }

  onQuestionBankKeydown(questionId: number, event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.toggleQuestionSelection(questionId);
    }
  }

  private insertDraggedQuestionAt(index: number): void {
    const draggedQuestionId = this.draggedQuestionId();

    if (!draggedQuestionId) {
      return;
    }

    this.selectedQuestionIds.update((selectedQuestionIds) => {
      const withoutDragged = selectedQuestionIds.filter((id) => id !== draggedQuestionId);
      const insertionIndex = Math.max(0, Math.min(index, withoutDragged.length));
      const next = [...withoutDragged];
      next.splice(insertionIndex, 0, draggedQuestionId);
      return next;
    });

    this.endDraggingQuestion();
  }

  questionCategoriesLabel(question: QuestionDto): string {
    return question.categories.map((category) => category.name).join(', ');
  }

  selectedQuestionPrompt(questionId: number): string {
    return this.questions().find((question) => question.id === questionId)?.prompt ?? `Question #${questionId}`;
  }

  toggleCategory(categoryId: number): void {
    this.selectedCategoryIds.update((selectedCategoryIds) =>
      selectedCategoryIds.includes(categoryId)
        ? selectedCategoryIds.filter((id) => id !== categoryId)
        : [...selectedCategoryIds, categoryId]
    );
  }

  saveQuiz(): void {
    if (!this.canManageCourse()) {
      this.pushToast('Editing unavailable', 'Only the course owner, moderator, or global admin can create or edit quizzes.', 'error');
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.pushToast('Cannot save quiz', 'Complete the title and ordering fields first.', 'error');
      return;
    }

    const payload = this.captureDraft();
    const validationMessage = this.validateDraft(payload);

    if (validationMessage) {
      this.pushToast('Cannot save quiz', validationMessage, 'error');
      return;
    }

    if (this.isEditing && !this.hasPendingChanges()) {
      this.pushToast('Nothing changed', 'Make at least one change before saving a new quiz version.', 'info');
      return;
    }

    this.isSubmitting.set(true);
    const request$ = this.isEditing
      ? this.quizService.updateQuiz(this.courseId, this.quizId, payload)
      : this.quizService.createQuiz(this.courseId, payload);

    request$
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (quiz) => {
          this.quiz.set(quiz);
          this.applyQuizToForm(quiz);
          this.pushToast(this.isEditing ? 'Version saved' : 'Quiz created', this.isEditing ? 'A new quiz version has been saved.' : 'Quiz added to the course.', 'success');

          if (this.isEditing) {
            this.reloadVersions();
            return;
          }

          void this.router.navigate(['/courses', this.courseId, 'quizzes', quiz.id]);
        },
        error: (error) => {
          this.pushToast('Save failed', extractApiMessage(error) ?? 'Unable to save this quiz right now.', 'error');
        }
      });
  }

  hasPendingChanges(): boolean {
    const current = this.captureDraft();

    if (!this.isEditing) {
      return !this.areDraftsEqual(current, this.emptyDraft());
    }

    const quiz = this.quiz();
    return quiz ? !this.areDraftsEqual(current, this.createDraftFromQuiz(quiz)) : false;
  }

  confirmDiscardChanges(): Promise<boolean> {
    return this.pendingChangesDialog.confirm({
      title: 'Leave quiz editing?',
      message: 'Your latest quiz changes are not saved yet. Stay here to save them, or leave and discard this edit.',
      confirmLabel: 'Leave without saving',
      cancelLabel: 'Stay here'
    });
  }

  @HostListener('window:beforeunload', ['$event'])
  handleBeforeUnload(event: BeforeUnloadEvent): void {
    if (!this.hasPendingChanges()) {
      return;
    }

    event.preventDefault();
    event.returnValue = true;
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId) || (this.rawQuizId !== null && !Number.isFinite(this.quizId))) {
      this.isLoading.set(false);
      this.pushToast('Invalid link', 'This quiz link is invalid.', 'error');
      return;
    }

    this.courseService.fetchCourse(this.courseId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (course) => {
          this.course.set(course);

          if (!this.canManageCourse()) {
            this.isLoading.set(false);
            return;
          }

          forkJoin({
            categories: this.categoryService.fetchCategories(this.courseId),
            questions: this.questionService.fetchQuestions(this.courseId),
            quiz: this.isEditing ? this.quizService.fetchQuiz(this.courseId, this.quizId) : of(null),
            versions: this.isEditing ? this.quizService.fetchQuizVersions(this.courseId, this.quizId) : of([])
          })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: ({ categories, questions, quiz, versions }) => {
                this.categories.set(categories);
                this.questions.set(questions);
                this.quiz.set(quiz);
                this.versions.set(versions);

                if (quiz) {
                  this.applyQuizToForm(quiz);
                }

                this.isLoading.set(false);
              },
              error: (error) => {
                this.isLoading.set(false);
                this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load this quiz workspace right now.', 'error');
              }
            });
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load this course right now.', 'error');
        }
      });
  }

  private applyQuizToForm(quiz: QuizDto): void {
    this.form.reset({
      title: quiz.title,
      mode: quiz.mode,
      randomCount: quiz.randomCount ?? Math.max(quiz.resolvedQuestionCount, 1),
      questionOrder: quiz.questionOrder,
      answerOrder: quiz.answerOrder
    });
    this.selectedQuestionIds.set([...quiz.questionIds]);
    this.selectedCategoryIds.set(quiz.categories.map((category) => category.id));
  }

  private reloadVersions(): void {
    this.quizService.fetchQuizVersions(this.courseId, this.quizId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (versions) => this.versions.set(versions),
        error: (error) => {
          this.pushToast('History unavailable', extractApiMessage(error) ?? 'Unable to load quiz history right now.', 'error');
        }
      });
  }

  private captureDraft(): SaveQuizRequest {
    const value = this.form.getRawValue();

    return {
      title: value.title.trim(),
      mode: value.mode,
      randomCount: value.mode === 'random' || value.mode === 'category' ? value.randomCount : null,
      questionOrder: value.questionOrder,
      answerOrder: value.answerOrder,
      questionIds: value.mode === 'manual' ? [...this.selectedQuestionIds()] : [],
      categoryIds: value.mode === 'category' ? [...this.selectedCategoryIds()].sort((a, b) => a - b) : []
    };
  }

  private validateDraft(draft: SaveQuizRequest): string | null {
    if (draft.mode === 'manual' && !draft.questionIds.length) {
      return 'Select at least one question for a manual quiz.';
    }

    if (draft.mode === 'random' && (!draft.randomCount || draft.randomCount < 1)) {
      return 'Choose a random question count greater than zero.';
    }

    if (draft.mode === 'category' && !draft.categoryIds.length) {
      return 'Select at least one category for a category quiz.';
    }

    if (draft.mode === 'category' && (!draft.randomCount || draft.randomCount < 1)) {
      return 'Choose how many questions should be pulled from the selected categories.';
    }

    return null;
  }

  private emptyDraft(): QuizDraftSnapshot {
    return {
      title: '',
      mode: 'manual',
      randomCount: null,
      questionOrder: 'fixed',
      answerOrder: 'fixed',
      questionIds: [],
      categoryIds: []
    };
  }

  private createDraftFromQuiz(quiz: QuizDto): QuizDraftSnapshot {
    return {
      title: quiz.title,
      mode: quiz.mode,
      randomCount: quiz.mode === 'random' ? quiz.randomCount : null,
      questionOrder: quiz.questionOrder,
      answerOrder: quiz.answerOrder,
      questionIds: quiz.mode === 'manual' ? [...quiz.questionIds] : [],
      categoryIds: quiz.mode === 'category' ? quiz.categories.map((category) => category.id).slice().sort((a, b) => a - b) : []
    };
  }

  private areDraftsEqual(left: QuizDraftSnapshot, right: QuizDraftSnapshot): boolean {
    return JSON.stringify(left) === JSON.stringify(right);
  }

  private pushToast(title: string, message: string, tone: ToastItem['tone']): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

    this.toastTimeouts.set(id, timeout);
    this.toasts.update((toasts) => [...toasts, { id, title, message, tone }]);
  }
}
