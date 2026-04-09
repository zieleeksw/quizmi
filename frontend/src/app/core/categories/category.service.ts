import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CategoryDto, CategoryVersionDto, CreateCategoryRequest, UpdateCategoryRequest } from './category.models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;

  fetchCategories(courseId: number): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>(`${this.apiBaseUrl}/courses/${courseId}/categories`);
  }

  fetchCategory(courseId: number, categoryId: number): Observable<CategoryDto> {
    return this.http.get<CategoryDto>(`${this.apiBaseUrl}/courses/${courseId}/categories/${categoryId}`);
  }

  createCategory(courseId: number, payload: CreateCategoryRequest): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${this.apiBaseUrl}/courses/${courseId}/categories`, payload);
  }

  updateCategory(courseId: number, categoryId: number, payload: UpdateCategoryRequest): Observable<CategoryDto> {
    return this.http.put<CategoryDto>(`${this.apiBaseUrl}/courses/${courseId}/categories/${categoryId}`, payload);
  }

  fetchCategoryVersions(courseId: number, categoryId: number): Observable<CategoryVersionDto[]> {
    return this.http.get<CategoryVersionDto[]>(`${this.apiBaseUrl}/courses/${courseId}/categories/${categoryId}/versions`);
  }
}
