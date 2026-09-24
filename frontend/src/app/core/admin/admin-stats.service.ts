import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/app-runtime-config';
import { AdminQuizAttemptDto } from './admin-stats.models';

@Injectable({ providedIn: 'root' })
export class AdminStatsService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  fetchAllQuizAttempts(): Observable<AdminQuizAttemptDto[]> {
    return this.http.get<AdminQuizAttemptDto[]>(`${this.apiBaseUrl}/admin/quiz-attempts`);
  }
}
