import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/app-runtime-config';
import {
  AdminCourseOptionDto,
  AdminQuizAttemptFilters,
  AdminQuizAttemptPageDto,
  AdminQuizOptionDto,
  AdminUserOptionDto
} from './admin-stats.models';

@Injectable({ providedIn: 'root' })
export class AdminStatsService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  fetchQuizAttempts(page: number, size: number, filters: AdminQuizAttemptFilters): Observable<AdminQuizAttemptPageDto> {
    let params = new HttpParams().set('page', page).set('size', size);

    if (filters.courseId != null) {
      params = params.set('courseId', filters.courseId);
    }

    if (filters.quizId != null) {
      params = params.set('quizId', filters.quizId);
    }

    if (filters.userId != null) {
      params = params.set('userId', filters.userId);
    }

    return this.http.get<AdminQuizAttemptPageDto>(`${this.apiBaseUrl}/admin/quiz-attempts`, { params });
  }

  fetchCourseOptions(userId: number | null): Observable<AdminCourseOptionDto[]> {
    let params = new HttpParams();

    if (userId != null) {
      params = params.set('userId', userId);
    }

    return this.http.get<AdminCourseOptionDto[]>(`${this.apiBaseUrl}/admin/courses`, { params });
  }

  fetchQuizOptions(userId: number | null, courseId: number | null): Observable<AdminQuizOptionDto[]> {
    let params = new HttpParams();

    if (userId != null) {
      params = params.set('userId', userId);
    }

    if (courseId != null) {
      params = params.set('courseId', courseId);
    }

    return this.http.get<AdminQuizOptionDto[]>(`${this.apiBaseUrl}/admin/quizzes`, { params });
  }

  searchPlayers(search: string): Observable<AdminUserOptionDto[]> {
    const params = new HttpParams().set('search', search);

    return this.http.get<AdminUserOptionDto[]>(`${this.apiBaseUrl}/admin/users`, { params });
  }
}
