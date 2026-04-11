import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  QuizAttemptDetailDto,
  QuizAttemptDto,
  QuizAttemptAnswerRequest,
  QuizSessionDto,
  SaveQuizSessionRequest
} from './attempt.models';

@Injectable({ providedIn: 'root' })
export class AttemptService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;

  fetchSessions(courseId: number): Observable<QuizSessionDto[]> {
    return this.http.get<QuizSessionDto[]>(`${this.apiBaseUrl}/courses/${courseId}/sessions`);
  }

  createOrResumeSession(courseId: number, quizId: number): Observable<QuizSessionDto> {
    return this.http.post<QuizSessionDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}/session`, {});
  }

  updateSession(courseId: number, quizId: number, payload: SaveQuizSessionRequest): Observable<QuizSessionDto> {
    return this.http.put<QuizSessionDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}/session`, payload);
  }

  fetchAttempts(courseId: number): Observable<QuizAttemptDto[]> {
    return this.http.get<QuizAttemptDto[]>(`${this.apiBaseUrl}/courses/${courseId}/attempts`);
  }

  fetchAttemptDetail(courseId: number, attemptId: number): Observable<QuizAttemptDetailDto> {
    return this.http.get<QuizAttemptDetailDto>(`${this.apiBaseUrl}/courses/${courseId}/attempts/${attemptId}`);
  }

  fetchAttemptReviews(courseId: number): Observable<QuizAttemptDetailDto[]> {
    return this.http.get<QuizAttemptDetailDto[]>(`${this.apiBaseUrl}/courses/${courseId}/attempts/reviews`);
  }

  createAttempt(courseId: number, quizId: number, answers: QuizAttemptAnswerRequest[]): Observable<QuizAttemptDto> {
    return this.http.post<QuizAttemptDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}/attempts`, { answers });
  }
}
