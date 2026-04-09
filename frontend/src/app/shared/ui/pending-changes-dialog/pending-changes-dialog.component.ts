import { Component, HostListener, inject } from '@angular/core';

import { PendingChangesDialogService } from '../../../core/navigation/pending-changes-dialog.service';
import { ActionButtonComponent } from '../action-button/action-button.component';

@Component({
  selector: 'app-pending-changes-dialog',
  imports: [ActionButtonComponent],
  templateUrl: './pending-changes-dialog.component.html',
  styleUrl: './pending-changes-dialog.component.scss'
})
export class PendingChangesDialogComponent {
  readonly dialog = inject(PendingChangesDialogService);

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    if (this.dialog.state().open) {
      this.dialog.cancel();
    }
  }

  cancel(): void {
    this.dialog.cancel();
  }

  accept(): void {
    this.dialog.accept();
  }
}
