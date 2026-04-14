import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/app-runtime-config';
import { QuizDto, QuizVersionDto, SaveQuizRequest } from './quiz.models';

@Injectable({ providedIn: 'root' })
export class QuizService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  fetchQuizzes(courseId: number): Observable<QuizDto[]> {
    return this.http.get<QuizDto[]>(`${this.apiBaseUrl}/courses/${courseId}/quizzes`);
  }

  fetchQuiz(courseId: number, quizId: number): Observable<QuizDto> {
    return this.http.get<QuizDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}`);
  }

  fetchQuizVersions(courseId: number, quizId: number): Observable<QuizVersionDto[]> {
    return this.http.get<QuizVersionDto[]>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}/versions`);
  }

  createQuiz(courseId: number, payload: SaveQuizRequest): Observable<QuizDto> {
    return this.http.post<QuizDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes`, payload);
  }

  updateQuiz(courseId: number, quizId: number, payload: SaveQuizRequest): Observable<QuizDto> {
    return this.http.put<QuizDto>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}`, payload);
  }

  deleteQuiz(courseId: number, quizId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/courses/${courseId}/quizzes/${quizId}`);
  }
}
