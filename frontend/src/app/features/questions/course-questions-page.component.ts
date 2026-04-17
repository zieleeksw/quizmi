import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CategoryDto } from '../../core/categories/category.models';
import { CategoryService } from '../../core/categories/category.service';
import { CourseDto } from '../../core/courses/course.models';
import { CourseService } from '../../core/courses/course.service';
import { QuestionDto, QuestionPageDto } from '../../core/questions/question.models';
import { QuestionService } from '../../core/questions/question.service';
import { extractApiMessage } from '../../shared/api/api-error.utils';
import { RichTextHtmlPipe } from '../../shared/rich-text/rich-text-html.pipe';
import { CourseWorkspaceSectionComponent } from '../../shared/ui/course-workspace-section/course-workspace-section.component';
import { ToastStackComponent } from '../../shared/ui/toast-stack/toast-stack.component';
import { ToastItem } from '../../shared/ui/toast-stack/toast-stack.models';

@Component({
  selector: 'app-course-questions-page',
  imports: [DatePipe, RouterLink, CourseWorkspaceSectionComponent, RichTextHtmlPipe, ToastStackComponent],
  templateUrl: './course-questions-page.component.html',
  styleUrl: './course-questions-page.component.scss'
})
export class CourseQuestionsPageComponent {
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly courseService = inject(CourseService);
  private readonly categoryService = inject(CategoryService);
  private readonly questionService = inject(QuestionService);
  private readonly toastTimeouts = new Map<number, ReturnType<typeof setTimeout>>();
  private toastId = 0;

  readonly courseId = this.resolveCourseId();
  readonly course = signal<CourseDto | null>(null);
  readonly categories = signal<CategoryDto[]>([]);
  readonly preview = signal<QuestionPageDto | null>(null);
  readonly isLoadingPage = signal(true);
  readonly isLoadingPreview = signal(false);
  readonly pageLoadError = signal<string | null>(null);
  readonly previewLoadError = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly selectedCategoryId = signal<number | 'all'>('all');
  readonly requestedPage = signal(0);
  readonly errorToasts = signal<ToastItem[]>([]);
  readonly canManageCourse = computed(() => this.course()?.canManage ?? false);
  readonly canAccessCourse = computed(() => this.course()?.canAccess ?? false);
  readonly hasCategories = computed(() => this.categories().length > 0);
  readonly createQuestionHint = 'Add at least one category before creating questions.';
  readonly previewQuestions = computed(() => this.preview()?.items ?? []);
  readonly pageLabel = computed(() => {
    const preview = this.preview();

    if (!preview || preview.totalPages === 0) {
      return 'Page 0 of 0';
    }

    return `Page ${preview.pageNumber + 1} of ${preview.totalPages}`;
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
    if (!this.canAccessCourse()) {
      return;
    }

    this.searchTerm.set(value);
    this.requestedPage.set(0);
    this.loadPreview();
  }

  selectCategory(categoryId: number | 'all'): void {
    if (!this.canAccessCourse()) {
      return;
    }

    this.selectedCategoryId.set(categoryId);
    this.requestedPage.set(0);
    this.loadPreview();
  }

  goToPreviousPage(): void {
    if (!this.canAccessCourse()) {
      return;
    }

    const preview = this.preview();

    if (!preview?.hasPrevious) {
      return;
    }

    this.requestedPage.update((page) => Math.max(page - 1, 0));
    this.loadPreview();
  }

  goToNextPage(): void {
    if (!this.canAccessCourse()) {
      return;
    }

    const preview = this.preview();

    if (!preview?.hasNext) {
      return;
    }

    this.requestedPage.update((page) => page + 1);
    this.loadPreview();
  }

  trackByQuestionId(_index: number, question: QuestionDto): number {
    return question.id;
  }

  isQuestionUpdated(question: QuestionDto): boolean {
    return question.currentVersionNumber > 1;
  }

  questionEditorLink(question: QuestionDto): string[] | null {
    if (!this.canManageCourse()) {
      return null;
    }

    return ['/courses', String(question.courseId), 'questions', String(question.id), 'edit'];
  }

  private loadPage(): void {
    if (!Number.isFinite(this.courseId)) {
      this.isLoadingPage.set(false);
      this.pushErrorToast('This course link is invalid.');
      return;
    }

    this.isLoadingPage.set(true);
    this.pageLoadError.set(null);

    forkJoin({
      course: this.courseService.fetchCourse(this.courseId),
      categories: this.categoryService.fetchCategories(this.courseId)
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ course, categories }) => {
          this.course.set(course);
          this.categories.set(categories);
          this.isLoadingPage.set(false);
          this.loadPreview();
        },
        error: (error: unknown) => {
          this.isLoadingPage.set(false);
          const message = extractApiMessage(error) ?? 'Unable to load the question bank right now.';
          this.pageLoadError.set(message);
          this.pushErrorToast(message);
        }
      });
  }

  private loadPreview(): void {
    if (!Number.isFinite(this.courseId) || !this.course()) {
      return;
    }

    this.isLoadingPreview.set(true);
    this.previewLoadError.set(null);
    const selectedCategoryId = this.selectedCategoryId();

    this.questionService.fetchQuestionPreview(
      this.courseId,
      this.requestedPage(),
      5,
      this.searchTerm(),
      selectedCategoryId === 'all' ? null : selectedCategoryId
    )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (preview) => {
          this.preview.set(preview);
          this.requestedPage.set(preview.pageNumber);
          this.isLoadingPreview.set(false);
        },
        error: (error: unknown) => {
          this.isLoadingPreview.set(false);
          const message = extractApiMessage(error) ?? 'Unable to refresh question results right now.';
          this.previewLoadError.set(message);
          this.pushErrorToast(message);
        }
      });
  }

  private resolveCourseId(): number {
    return Number.parseInt(
      this.route.parent?.snapshot.paramMap.get('courseId') ?? this.route.snapshot.paramMap.get('courseId') ?? '',
      10
    );
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
