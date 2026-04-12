import { of, forkJoin } from 'rxjs';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, HostListener, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { CategoryDto } from '../../core/categories/category.models';
import { CategoryService } from '../../core/categories/category.service';
import { PendingChangesDialogService } from '../../core/navigation/pending-changes-dialog.service';
import { PendingChangesAware } from '../../core/navigation/pending-changes.guard';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto, QuestionVersionDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

type AnswerDraft = {
  content: string;
  correct: boolean;
};

type AnswerFormGroup = FormGroup<{
  content: FormControl<string>;
  correct: FormControl<boolean>;
}>;

type QuestionDraftSnapshot = {
  prompt: string;
  answers: AnswerDraft[];
  categoryIds: number[];
};

@Component({
  selector: 'app-question-editor-page',
  imports: [DatePipe, ReactiveFormsModule, RouterLink, ActionButtonComponent, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './question-editor-page.component.html',
  styleUrl: './question-editor-page.component.scss'
})
export class QuestionEditorPageComponent implements PendingChangesAware {
  private static readonly MIN_ANSWERS = 2;
  private static readonly MAX_ANSWERS = 6;

  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly courseService = inject(CourseService);
  private readonly categoryService = inject(CategoryService);
  private readonly questionService = inject(QuestionService);
  private readonly pendingChangesDialog = inject(PendingChangesDialogService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly rawQuestionId = this.route.snapshot.paramMap.get('questionId');
  readonly questionId = this.rawQuestionId ? Number.parseInt(this.rawQuestionId, 10) : NaN;
  readonly isEditing = Number.isFinite(this.questionId);
  readonly course = signal<CourseDto | null>(null);
  readonly categories = signal<CategoryDto[]>([]);
  readonly question = signal<QuestionDto | null>(null);
  readonly existingQuestions = signal<QuestionDto[]>([]);
  readonly versions = signal<QuestionVersionDto[]>([]);
  readonly selectedCategoryIds = signal<number[]>([]);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly hasSubmitted = signal(false);
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly toasts = signal<ToastItem[]>([]);
  readonly promptDraft = signal('');
  readonly duplicatePromptHint = signal('');
  readonly canManageCourse = computed(() => {
    const currentCourse = this.course();
    const currentUserId = this.authService.user()?.id;

    return Boolean(currentCourse && currentUserId === currentCourse.ownerUserId);
  });

  readonly form = this.formBuilder.nonNullable.group({
    prompt: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(1000)]],
    answers: this.formBuilder.nonNullable.array([
      this.createAnswerGroup(),
      this.createAnswerGroup()
    ])
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }
    });

    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.hasSubmitted.set(false);
      this.serverFieldErrors.set({});
      this.promptDraft.set(this.form.controls.prompt.getRawValue());
      this.refreshDuplicatePromptHint();
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

  answersArray(): FormArray<AnswerFormGroup> {
    return this.form.controls.answers;
  }

  answerLabel(index: number): string {
    return String.fromCharCode(65 + index);
  }

  toggleCategory(categoryId: number): void {
    this.selectedCategoryIds.update((categoryIds) =>
      categoryIds.includes(categoryId) ? categoryIds.filter((entry) => entry !== categoryId) : [...categoryIds, categoryId]
    );
  }

  addAnswer(): void {
    if (this.answersArray().length >= QuestionEditorPageComponent.MAX_ANSWERS) {
      return;
    }

    this.answersArray().push(this.createAnswerGroup());
  }

  removeAnswer(index: number): void {
    if (this.answersArray().length <= QuestionEditorPageComponent.MIN_ANSWERS) {
      return;
    }

    this.answersArray().removeAt(index);
  }

  saveQuestion(): void {
    this.hasSubmitted.set(true);
    this.serverFieldErrors.set({});

    if (!Number.isFinite(this.courseId)) {
      this.pushToast('Invalid link', 'This course link is invalid.', 'error');
      return;
    }

    if (!this.canManageCourse()) {
      this.pushToast('Editing unavailable', 'Only the owner of this course can create or edit questions.', 'error');
      return;
    }

    const validationMessage = this.validateForm();

    if (validationMessage) {
      this.form.markAllAsTouched();
      this.pushToast('Cannot save question', validationMessage, 'error');
      return;
    }

    if (this.isEditing && !this.hasPendingChanges()) {
      this.pushToast('Nothing changed', 'Make at least one change before saving a new version.', 'info');
      return;
    }

    const payload = this.captureQuestionDraft();
    this.isSubmitting.set(true);

    const request$ = this.isEditing
      ? this.questionService.updateQuestion(this.courseId, this.questionId, payload)
      : this.questionService.createQuestion(this.courseId, payload);

    request$
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (question) => {
          this.question.set(question);
          this.existingQuestions.update((questions) => {
            const withoutCurrent = questions.filter((entry) => entry.id !== question.id);
            return [...withoutCurrent, question];
          });
          this.serverFieldErrors.set({});
          this.form.reset({ prompt: question.prompt }, { emitEvent: false });
          this.setAnswers(question.answers.map((answer) => ({
            content: answer.content,
            correct: answer.correct
          })));
          this.selectedCategoryIds.set(question.categories.map((category) => category.id));
          this.promptDraft.set(question.prompt);
          this.refreshDuplicatePromptHint();
          this.hasSubmitted.set(false);
          this.pushToast(
            this.isEditing ? 'Version saved' : 'Question created',
            this.isEditing ? 'A new question version has been saved.' : 'Question added to the course.',
            'success'
          );

          if (this.isEditing) {
            this.reloadVersions();
            return;
          }

          this.resetComposer();
        },
        error: (error) => {
          this.serverFieldErrors.set(extractFieldErrors(error));
          this.pushToast('Save failed', extractApiMessage(error) ?? 'Unable to save this question right now.', 'error');
        }
      });
  }

  hasPromptError(): boolean {
    if (this.serverFieldErrors()['prompt']) {
      return true;
    }

    return this.hasSubmitted() && this.form.controls.prompt.invalid;
  }

  getPromptError(): string | null {
    const serverError = this.serverFieldErrors()['prompt'];

    if (serverError) {
      return serverError;
    }

    const control = this.form.controls.prompt;

    if (!control.errors || !this.hasSubmitted()) {
      return null;
    }

    if (control.errors['required']) {
      return 'Question prompt is required.';
    }

    if (control.errors['minlength']) {
      return 'Question prompt must be at least 12 characters long.';
    }

    if (control.errors['maxlength']) {
      return 'Question prompt cannot be longer than 1000 characters.';
    }

    return null;
  }

  trackByVersionId(_index: number, version: QuestionVersionDto): number {
    return version.id;
  }

  hasPendingChanges(): boolean {
    const draft = this.captureQuestionDraft();

    if (!this.isEditing) {
      return !this.areQuestionDraftsEqual(draft, this.createEmptyQuestionSnapshot());
    }

    const question = this.question();

    if (!question) {
      return false;
    }

    return !this.areQuestionDraftsEqual(draft, this.createSnapshotFromQuestion(question));
  }

  confirmDiscardChanges(): Promise<boolean> {
    return this.pendingChangesDialog.confirm({
      title: 'Leave question editing?',
      message: 'Your latest question changes are not saved yet. Stay here to save them, or leave and discard them.',
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
    if (!Number.isFinite(this.courseId) || (this.rawQuestionId !== null && !Number.isFinite(this.questionId))) {
      this.isLoading.set(false);
      this.pushToast('Invalid link', 'This question link is invalid.', 'error');
      return;
    }

    this.isLoading.set(true);

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
            question: this.isEditing ? this.questionService.fetchQuestion(this.courseId, this.questionId) : of(null),
            versions: this.isEditing ? this.questionService.fetchQuestionVersions(this.courseId, this.questionId) : of([])
          })
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: ({ categories, questions, question, versions }) => {
                this.categories.set(categories);
                this.existingQuestions.set(questions);
                this.question.set(question);
                this.versions.set(versions);

                if (question) {
                  this.form.reset({ prompt: question.prompt }, { emitEvent: false });
                  this.setAnswers(question.answers.map((answer) => ({
                    content: answer.content,
                    correct: answer.correct
                  })));
                  this.selectedCategoryIds.set(question.categories.map((category) => category.id));
                }

                this.promptDraft.set(this.form.controls.prompt.getRawValue());
                this.refreshDuplicatePromptHint();

                this.isLoading.set(false);
              },
              error: (error) => {
                this.isLoading.set(false);
                this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load this question workspace right now.', 'error');
              }
            });
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushToast('Load failed', extractApiMessage(error) ?? 'Unable to load this course right now.', 'error');
        }
      });
  }

  private reloadVersions(): void {
    this.questionService.fetchQuestionVersions(this.courseId, this.questionId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (versions) => this.versions.set(versions),
        error: (error) => {
          this.pushToast('History unavailable', extractApiMessage(error) ?? 'Unable to load question history right now.', 'error');
        }
      });
  }

  private validateForm(): string | null {
    if (!this.selectedCategoryIds().length) {
      return 'Choose at least one category.';
    }

    if (this.answersArray().length < QuestionEditorPageComponent.MIN_ANSWERS) {
      return 'Question must contain at least two answers.';
    }

    const rawAnswers = this.answersArray().getRawValue();

    if (rawAnswers.some((answer) => !answer.content.trim())) {
      return 'Fill in every visible answer option.';
    }

    const normalizedAnswers = rawAnswers.map((answer) => answer.content.trim().toLocaleLowerCase());

    if (new Set(normalizedAnswers).size !== normalizedAnswers.length) {
      return 'Each answer must be unique.';
    }

    if (!rawAnswers.some((answer) => answer.correct)) {
      return 'Choose at least one correct answer.';
    }

    return null;
  }

  private createAnswerGroup(value: AnswerDraft = { content: '', correct: false }): AnswerFormGroup {
    return this.formBuilder.nonNullable.group({
      content: this.formBuilder.nonNullable.control(value.content, Validators.required),
      correct: this.formBuilder.nonNullable.control(value.correct)
    });
  }

  private setAnswers(answers: AnswerDraft[]): void {
    const nextArray = this.formBuilder.nonNullable.array(
      answers.map((answer) => this.createAnswerGroup(answer))
    );

    this.form.setControl('answers', nextArray);
  }

  private captureQuestionDraft(): QuestionDraftSnapshot {
    const value = this.form.getRawValue();

    return {
      prompt: value.prompt.trim(),
      answers: value.answers.map((answer) => ({
        content: answer.content.trim(),
        correct: answer.correct
      })),
      categoryIds: this.selectedCategoryIds().slice().sort((left, right) => left - right)
    };
  }

  private createEmptyQuestionSnapshot(): QuestionDraftSnapshot {
    return {
      prompt: '',
      answers: [
        { content: '', correct: false },
        { content: '', correct: false }
      ],
      categoryIds: []
    };
  }

  private createSnapshotFromQuestion(question: QuestionDto): QuestionDraftSnapshot {
    return {
      prompt: question.prompt.trim(),
      answers: question.answers
        .slice()
        .sort((left, right) => left.displayOrder - right.displayOrder)
        .map((answer) => ({
          content: answer.content.trim(),
          correct: answer.correct
        })),
      categoryIds: question.categories.map((category) => category.id).slice().sort((left, right) => left - right)
    };
  }

  private areQuestionDraftsEqual(left: QuestionDraftSnapshot, right: QuestionDraftSnapshot): boolean {
    return (
      left.prompt === right.prompt &&
      JSON.stringify(left.answers) === JSON.stringify(right.answers) &&
      JSON.stringify(left.categoryIds) === JSON.stringify(right.categoryIds)
    );
  }

  private resetComposer(): void {
    this.question.set(null);
    this.form.reset({ prompt: '' }, { emitEvent: false });
    this.setAnswers([
      { content: '', correct: false },
      { content: '', correct: false }
    ]);
    this.selectedCategoryIds.set([]);
    this.promptDraft.set('');
    this.refreshDuplicatePromptHint();
  }

  private pushToast(title: string, message: string, tone: ToastItem['tone']): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

    this.toastTimeouts.set(id, timeout);
    this.toasts.update((toasts) => [
      ...toasts,
      {
        id,
        title,
        message,
        tone
      }
    ]);
  }

  private refreshDuplicatePromptHint(): void {
    const normalizedPrompt = this.normalizePrompt(this.promptDraft());

    if (!normalizedPrompt) {
      this.duplicatePromptHint.set('');
      return;
    }

    const matches = this.existingQuestions().filter((question) =>
      question.id !== this.questionId &&
      this.normalizePrompt(question.prompt) === normalizedPrompt
    );

    if (!matches.length) {
      this.duplicatePromptHint.set('');
      return;
    }

    this.duplicatePromptHint.set(
      matches.length === 1
        ? 'A question with this exact prompt already exists in this course. You can still save if the answers or learning goal are different.'
        : `${matches.length} questions with this exact prompt already exist in this course. You can still save if this version serves a different set of answers or learning goal.`
    );
  }

  private normalizePrompt(prompt: string): string {
    return prompt.trim().replace(/\s+/g, ' ').toLocaleLowerCase();
  }
}
