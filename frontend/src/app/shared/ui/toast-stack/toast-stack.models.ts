export type ToastTone = 'error' | 'success' | 'info';

export type ToastItem = {
  id: number;
  title?: string;
  message: string;
  tone?: ToastTone;
};
