import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-course-workspace-section',
  templateUrl: './course-workspace-section.component.html',
  styleUrl: './course-workspace-section.component.scss'
})
export class CourseWorkspaceSectionComponent {
  readonly eyebrow = input.required<string>();
  readonly title = input.required<string>();
  readonly searchPlaceholder = input.required<string>();
  readonly searchValue = input('');
  readonly showSearch = input(true);
  readonly searchDisabled = input(false);
  readonly showPagination = input(false);
  readonly pageLabel = input('');
  readonly previousDisabled = input(false);
  readonly nextDisabled = input(false);
  readonly previousLabel = input('Previous');
  readonly nextLabel = input('Next');
  readonly searchValueChange = output<string>();
  readonly previousClicked = output<void>();
  readonly nextClicked = output<void>();

  handleSearchInput(value: string): void {
    this.searchValueChange.emit(value);
  }

  handlePreviousClick(): void {
    this.previousClicked.emit();
  }

  handleNextClick(): void {
    this.nextClicked.emit();
  }
}
