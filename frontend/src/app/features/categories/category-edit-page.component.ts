import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CategoryDto, CategoryVersionDto } from '../../core/categories/category.models';
import { CategoryService } from '../../core/categories/category.service';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { PendingChangesDialogService } from '../../core/navigation/pending-changes-dialog.service';
import { PendingChangesAware } from '../../core/navigation/pending-changes.guard';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-category-edit-page',
  imports: [DatePipe, RouterLink, ReactiveFormsModule, ActionButtonComponent, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './category-edit-page.component.html',
  styleUrl: './category-edit-page.component.scss'
})
export class CategoryEditPageComponent implements PendingChangesAware {
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);
  private readonly categoryService = inject(CategoryService);
  private readonly pendingChangesDialog = inject(PendingChangesDialogService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly categoryId = Number.parseInt(this.route.snapshot.paramMap.get('categoryId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly category = signal<CategoryDto | null>(null);
  readonly versions = signal<CategoryVersionDto[]>([]);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly hasSubmitted = signal(false);
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly toasts = signal<ToastItem[]>([]);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]]
  });

  constructor() {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.hasSubmitted.set(false);
      this.serverFieldErrors.set({});
    });

    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }
    });

    this.loadPage();
  }

  submit(): void {
    this.hasSubmitted.set(true);

    if (this.form.invalid || !Number.isFinite(this.courseId) || !Number.isFinite(this.categoryId)) {
      return;
    }

    const { name } = this.form.getRawValue();

    this.isSubmitting.set(true);
    this.serverFieldErrors.set({});

    this.categoryService
      .updateCategory(this.courseId, this.categoryId, { name })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (category) => {
          this.category.set(category);
          this.form.reset({ name: category.name }, { emitEvent: false });
          this.hasSubmitted.set(false);
          this.pushToast('Category updated and version history saved.', 'success');
          this.reloadVersions();
        },
        error: (error) => {
          this.serverFieldErrors.set(extractFieldErrors(error));
          this.pushToast(extractApiMessage(error) ?? 'Unable to update this category right now.', 'error');
        }
      });
  }

  dismissToast(id: number): void {
    const timeout = this.toastTimeouts.get(id);

    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    this.toasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  hasError(): boolean {
    if (this.serverFieldErrors()['name']) {
      return true;
    }

    return this.hasSubmitted() && this.form.controls.name.invalid;
  }

  getErrorMessage(): string | null {
    const serverError = this.serverFieldErrors()['name'];

    if (serverError) {
      return serverError;
    }

    const control = this.form.controls.name;

    if (!control.errors || !this.hasSubmitted()) {
      return null;
    }

    if (control.errors['required']) {
      return 'Category name is required.';
    }

    if (control.errors['minlength']) {
      return 'Category name must be at least 2 characters long.';
    }

    if (control.errors['maxlength']) {
      return 'Category name cannot be longer than 120 characters.';
    }

    return null;
  }

  trackByVersionId(_index: number, version: CategoryVersionDto): number {
    return version.id;
  }

  hasPendingChanges(): boolean {
    const category = this.category();

    if (!category) {
      return false;
    }

    return this.form.getRawValue().name.trim() !== category.name.trim();
  }

  confirmDiscardChanges(): Promise<boolean> {
    return this.pendingChangesDialog.confirm({
      title: 'Leave category editing?',
      message: 'Your latest category name changes are not saved yet. Stay here to save them, or leave and discard them.',
      confirmLabel: 'Leave without saving',
      cancelLabel: 'Stay here'
    });
  }

  isCategoryUpdated(): boolean {
    const category = this.category();

    if (!category) {
      return false;
    }

    return category.updatedAt !== category.createdAt;
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
    if (!Number.isFinite(this.courseId) || !Number.isFinite(this.categoryId)) {
      this.isLoading.set(false);
      this.pushToast('This category link is invalid.', 'error');
      return;
    }

    this.isLoading.set(true);

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      category: this.categoryService.fetchCategory(this.courseId, this.categoryId),
      versions: this.categoryService.fetchCategoryVersions(this.courseId, this.categoryId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, category, versions }) => {
          this.course.set(course);
          this.category.set(category);
          this.versions.set(versions);
          this.form.reset({ name: category.name }, { emitEvent: false });
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushToast(extractApiMessage(error) ?? 'Unable to load this category right now.', 'error');
        }
      });
  }

  private reloadVersions(): void {
    this.categoryService
      .fetchCategoryVersions(this.courseId, this.categoryId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (versions) => {
          this.versions.set(versions);
        }
      });
  }

  private pushToast(message: string, tone: 'error' | 'success'): void {
    const id = ++this.toastId;
    const timeout = setTimeout(() => this.dismissToast(id), 5000);

    this.toastTimeouts.set(id, timeout);
    this.toasts.update((toasts) => [
      ...toasts,
      {
        id,
        message,
        tone
      }
    ]);
  }
}
