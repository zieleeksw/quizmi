import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ActionButtonComponent } from '../../shared/ui/action-button/action-button.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-courses-page',
  imports: [DatePipe, RouterLink, ActionButtonComponent, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './courses-page.component.html',
  styleUrl: './courses-page.component.scss'
})
export class CoursesPageComponent {
  private static readonly PAGE_SIZE = 10;

  private readonly destroyRef = inject(DestroyRef);
  private readonly courseService = inject(CourseService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courses = signal<CourseDto[]>([]);
  readonly isLoading = signal(true);
  readonly errorToasts = signal<ToastItem[]>([]);
  readonly searchTerm = signal('');
  readonly currentPage = signal(1);
  readonly filteredCourses = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();

    if (!query) {
      return this.courses();
    }

    return this.courses().filter((course) => {
      return (
        course.name.toLowerCase().includes(query) ||
        course.description.toLowerCase().includes(query) ||
        course.ownerEmail.toLowerCase().includes(query)
      );
    });
  });
  readonly totalPages = computed(() => {
    return Math.max(1, Math.ceil(this.filteredCourses().length / CoursesPageComponent.PAGE_SIZE));
  });
  readonly paginatedCourses = computed(() => {
    const startIndex = (this.currentPage() - 1) * CoursesPageComponent.PAGE_SIZE;
    return this.filteredCourses().slice(startIndex, startIndex + CoursesPageComponent.PAGE_SIZE);
  });
  readonly pageNumbers = computed(() => {
    return Array.from({ length: this.totalPages() }, (_, index) => index + 1);
  });

  constructor() {
    this.loadCourses();

    effect(() => {
      const currentPage = this.currentPage();
      const totalPages = this.totalPages();

      if (currentPage > totalPages) {
        this.currentPage.set(totalPages);
      }
    });

    this.destroyRef.onDestroy(() => {
      for (const timeout of this.toastTimeouts.values()) {
        clearTimeout(timeout);
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

  trackByCourseId(_index: number, course: CourseDto): number {
    return course.id;
  }

  updateSearchTerm(value: string): void {
    this.searchTerm.set(value);
    this.currentPage.set(1);
  }

  setPage(page: number): void {
    if (page < 1 || page > this.totalPages() || page === this.currentPage()) {
      return;
    }

    this.currentPage.set(page);
  }

  nextPage(): void {
    this.setPage(this.currentPage() + 1);
  }

  previousPage(): void {
    this.setPage(this.currentPage() - 1);
  }

  private loadCourses(): void {
    this.isLoading.set(true);

    this.courseService
      .fetchCourses()
      .pipe(
        finalize(() => this.isLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (courses) => {
          this.courses.set(courses);
        },
        error: (error) => {
          this.pushErrorToast(extractApiMessage(error) ?? 'Unable to load your courses right now.');
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
