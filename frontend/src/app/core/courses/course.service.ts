import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/app-runtime-config';
import { CourseDto, CreateCourseRequest, UpdateCourseRequest } from './course.models';

@Injectable({ providedIn: 'root' })
export class CourseService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  fetchCourses(): Observable<CourseDto[]> {
    return this.http.get<CourseDto[]>(`${this.apiBaseUrl}/courses`);
  }

  fetchCourse(courseId: number): Observable<CourseDto> {
    return this.http.get<CourseDto>(`${this.apiBaseUrl}/courses/${courseId}`);
  }

  createCourse(payload: CreateCourseRequest): Observable<CourseDto> {
    return this.http.post<CourseDto>(`${this.apiBaseUrl}/courses`, payload);
  }

  updateCourse(courseId: number, payload: UpdateCourseRequest): Observable<CourseDto> {
    return this.http.put<CourseDto>(`${this.apiBaseUrl}/courses/${courseId}`, payload);
  }
}
