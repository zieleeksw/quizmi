import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { CategoryDto } from '../../core/categories/category.models';
import { CategoryService } from '../../core/categories/category.service';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';
import { WorkspaceTopbarComponent } from '../../shared/ui/workspace-topbar/workspace-topbar.component';

@Component({
  selector: 'app-course-categories-page',
  imports: [DatePipe, RouterLink, ToastStackComponent, WorkspaceTopbarComponent],
  templateUrl: './course-categories-page.component.html',
  styleUrl: './course-categories-page.component.scss'
})
export class CourseCategoriesPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);
  private readonly courseService = inject(CourseService);
  private readonly categoryService = inject(CategoryService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = Number.parseInt(this.route.snapshot.paramMap.get('courseId') ?? '', 10);
  readonly course = signal<CourseDto | null>(null);
  readonly categories = signal<CategoryDto[]>([]);
  readonly isLoading = signal(true);
  readonly searchTerm = signal('');
  readonly errorToasts = signal<ToastItem[]>([]);
  readonly canManageCourse = computed(() => {
    const currentCourse = this.course();
    const currentUserId = this.authService.user()?.id;

    return Boolean(currentCourse && currentUserId === currentCourse.ownerUserId);
  });
  readonly filteredCategories = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();

    if (!query) {
      return this.categories();
    }

    return this.categories().filter((category) => category.name.toLowerCase().includes(query));
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

    this.errorToasts.update((toasts) => toasts.filter((toast) => toast.id !== id));
  }

  updateSearchTerm(value: string): void {
    this.searchTerm.set(value);
  }

  trackByCategoryId(_index: number, category: CategoryDto): number {
    return category.id;
  }

  isCategoryUpdated(category: CategoryDto): boolean {
    return category.updatedAt !== category.createdAt;
  }

  categoryCardLink(category: CategoryDto): string[] | null {
    if (!this.canManageCourse()) {
      return null;
    }

    return ['/courses', String(category.courseId), 'categories', String(category.id), 'edit'];
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId)) {
      this.isLoading.set(false);
      this.pushErrorToast('This course link is invalid.');
      return;
    }

    this.isLoading.set(true);

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      categories: this.categoryService.fetchCategories(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, categories }) => {
          this.course.set(course);
          this.categories.set(categories);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.pushErrorToast(extractApiMessage(error) ?? 'Unable to load course categories right now.');
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
