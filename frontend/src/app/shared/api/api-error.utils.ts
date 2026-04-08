import { HttpErrorResponse } from '@angular/common/http';

type FieldError = {
  field?: string;
  message?: string;
};

type ValidationPayload = {
  errors?: FieldError[];
};

type RuntimePayload = {
  message?: string;
};

export function extractFieldErrors(error: unknown): Record<string, string> {
  if (!(error instanceof HttpErrorResponse)) {
    return {};
  }

  const payload = error.error as ValidationPayload | null;

  if (!payload || !Array.isArray(payload.errors)) {
    return {};
  }

  return payload.errors.reduce<Record<string, string>>((accumulator, currentError) => {
    if (currentError.field && currentError.message) {
      accumulator[currentError.field] = currentError.message;
    }

    return accumulator;
  }, {});
}

export function extractApiMessage(error: unknown): string | null {
  if (!(error instanceof HttpErrorResponse)) {
    return null;
  }

  if (typeof error.error === 'string' && error.error.trim()) {
    return error.error;
  }

  const payload = error.error as RuntimePayload | null;

  if (payload?.message?.trim()) {
    return payload.message;
  }

  if (error.status === 0) {
    return 'Unable to reach the backend right now.';
  }

  return null;
}
