import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { extractApiMessage, extractFieldErrors } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-course-edit-page',
  imports: [RouterLink, ReactiveFormsModule, ActionButtonComponent, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './course-edit-page.component.html',
  styleUrl: './course-edit-page.component.scss'
})
export class CourseEditPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly courseService = inject(CourseService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly hasSubmitted = signal(false);
  readonly serverFieldErrors = signal<Record<string, string>>({});
  readonly errorToasts = signal<ToastItem[]>([]);

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(120)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
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

    this.loadCourse();
  }

  submit(): void {
    this.hasSubmitted.set(true);

    if (this.form.invalid || !Number.isFinite(this.courseId)) {
      return;
    }

    const { name, description } = this.form.getRawValue();

    this.isSubmitting.set(true);
    this.serverFieldErrors.set({});

    this.courseService
      .updateCourse(this.courseId, { name, description })
      .pipe(
        finalize(() => this.isSubmitting.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          void this.router.navigate(['/courses', this.courseId]);
        },
        error: (error) => {
          this.serverFieldErrors.set(extractFieldErrors(error));
          this.pushErrorToast(extractApiMessage(error) ?? 'Unable to save your course right now.');
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

  hasError(controlName: 'name' | 'description'): boolean {
    const control = this.form.controls[controlName];

    if (this.serverFieldErrors()[controlName]) {
      return true;
    }

    return this.hasSubmitted() && control.invalid;
  }

  getErrorMessage(controlName: 'name' | 'description'): string | null {
    const serverError = this.serverFieldErrors()[controlName];

    if (serverError) {
      return serverError;
    }

    const control = this.form.controls[controlName];

    if (!control.errors || !this.hasSubmitted()) {
      return null;
    }

    if (control.errors['required']) {
      return controlName === 'name' ? 'Course name is required.' : 'Course description is required.';
    }

    if (control.errors['minlength']) {
      return controlName === 'name'
        ? 'Course name must be at least 3 characters long.'
        : 'Course description must be at least 10 characters long.';
    }

    if (control.errors['maxlength']) {
      return controlName === 'name'
        ? 'Course name cannot be longer than 120 characters.'
        : 'Course description cannot be longer than 1000 characters.';
    }

    return null;
  }

  private loadCourse(): void {
    if (!Number.isFinite(this.courseId)) {
      this.isLoading.set(false);
      this.pushErrorToast('This course link is invalid.');
      return;
    }

    this.isLoading.set(true);

    this.courseService
      .fetchCourse(this.courseId)
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (course) => {
          this.course.set(course);
          this.form.reset(
            {
              name: course.name,
              description: course.description
            },
            { emitEvent: false }
          );
        },
        error: (error) => {
          this.pushErrorToast(extractApiMessage(error) ?? 'Unable to load this course right now.');
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
