import { InjectionToken } from '@angular/core';

import { generatedAppConfig } from './generated-app-config';

function normalizeApiBaseUrl(value: string): string {
  return value.replace(/\/+$/, '');
}

export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => normalizeApiBaseUrl(generatedAppConfig.apiBaseUrl)
});
