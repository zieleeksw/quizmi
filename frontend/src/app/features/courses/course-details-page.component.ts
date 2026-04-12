import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-course-details-page',
  imports: [DatePipe, RouterLink, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './course-details-page.component.html',
  styleUrl: './course-details-page.component.scss'
})
export class CourseDetailsPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly courseService = inject(CourseService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly isLoading = signal(true);
  readonly errorToasts = signal<ToastItem[]>([]);
  readonly pageTitle = computed(() => this.course()?.name ?? 'Course details');
  readonly canManageCourse = computed(() => {
    const currentCourse = this.course();
    const currentUserId = this.authService.user()?.id;

    return Boolean(currentCourse && currentUserId === currentCourse.ownerUserId);
  });

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
      }
    });

    this.loadCourse();
  }

  dismissToast(id: number): void {
    const timeout = this.toastTimeouts.get(id);

    if (timeout) {
      clearTimeout(timeout);
      this.toastTimeouts.delete(id);
    }

    this.errorToasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  private loadCourse(): void {
    if (!Number.isFinite(this.courseId)) {
      this.isLoading.set(false);
      this.pushToast('This course link is invalid.');
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
        },
        error: (error) => {
          this.pushToast(extractApiMessage(error) ?? 'Unable to load this course right now.');
        }
      });
  }

  private pushToast(message: string): void {
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
