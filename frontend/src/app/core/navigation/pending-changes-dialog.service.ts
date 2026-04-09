import { Injectable, signal } from '@angular/core';

export type PendingChangesDialogOptions = {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
};

type PendingChangesDialogState = PendingChangesDialogOptions & {
  open: boolean;
};

@Injectable({
  providedIn: 'root'
})
export class PendingChangesDialogService {
  private resolver: ((value: boolean) => void) | null = null;
  private pendingPromise: Promise<boolean> | null = null;

  readonly state = signal<PendingChangesDialogState>({
    open: false,
    title: '',
    message: '',
    confirmLabel: 'Leave without saving',
    cancelLabel: 'Stay here'
  });

  confirm(options: PendingChangesDialogOptions): Promise<boolean> {
    if (this.pendingPromise) {
      return this.pendingPromise;
    }

    this.state.set({
      open: true,
      title: options.title,
      message: options.message,
      confirmLabel: options.confirmLabel ?? 'Leave without saving',
      cancelLabel: options.cancelLabel ?? 'Stay here'
    });

    this.pendingPromise = new Promise<boolean>((resolve) => {
      this.resolver = resolve;
    });

    return this.pendingPromise;
  }

  accept(): void {
    this.resolve(true);
  }

  cancel(): void {
    this.resolve(false);
  }

  private resolve(result: boolean): void {
    const resolver = this.resolver;

    this.resolver = null;
    this.pendingPromise = null;
    this.state.update((state) => ({ ...state, open: false }));
    resolver?.(result);
  }
}
