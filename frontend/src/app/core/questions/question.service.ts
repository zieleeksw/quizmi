import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/app-runtime-config';
import { QuestionDto, QuestionPageDto, QuestionVersionDto, SaveQuestionRequest } from './question.models';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  fetchQuestions(courseId: number): Observable<QuestionDto[]> {
    return this.http.get<QuestionDto[]>(`${this.apiBaseUrl}/courses/${courseId}/questions`);
  }

  fetchQuestionPreview(
    courseId: number,
    page = 0,
    size = 5,
    search = '',
    categoryId: number | null = null
  ): Observable<QuestionPageDto> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (search.trim()) {
      params = params.set('search', search.trim());
    }

    if (categoryId !== null) {
      params = params.set('categoryId', categoryId);
    }

    return this.http.get<QuestionPageDto>(`${this.apiBaseUrl}/courses/${courseId}/questions/preview`, { params });
  }

  fetchQuestion(courseId: number, questionId: number): Observable<QuestionDto> {
    return this.http.get<QuestionDto>(`${this.apiBaseUrl}/courses/${courseId}/questions/${questionId}`);
  }

  createQuestion(courseId: number, payload: SaveQuestionRequest): Observable<QuestionDto> {
    return this.http.post<QuestionDto>(`${this.apiBaseUrl}/courses/${courseId}/questions`, payload);
  }

  updateQuestion(courseId: number, questionId: number, payload: SaveQuestionRequest): Observable<QuestionDto> {
    return this.http.put<QuestionDto>(`${this.apiBaseUrl}/courses/${courseId}/questions/${questionId}`, payload);
  }

  fetchQuestionVersions(courseId: number, questionId: number): Observable<QuestionVersionDto[]> {
    return this.http.get<QuestionVersionDto[]>(`${this.apiBaseUrl}/courses/${courseId}/questions/${questionId}/versions`);
  }
}
