import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { PendingChangesDialogService } from './pending-changes-dialog.service';

export interface PendingChangesAware {
  hasPendingChanges(): boolean;
  confirmDiscardChanges?(): boolean | Promise<boolean>;
}

export const pendingChangesGuard: CanDeactivateFn<PendingChangesAware> = (component) => {
  const pendingChangesDialog = inject(PendingChangesDialogService);

  if (!component.hasPendingChanges()) {
    return true;
  }

  return component.confirmDiscardChanges?.() ?? pendingChangesDialog.confirm({
    title: 'Leave this page?',
    message: 'You have unsaved changes. Stay here to save them first, or leave now and discard this edit.',
    confirmLabel: 'Leave without saving',
    cancelLabel: 'Stay here'
  });
};
