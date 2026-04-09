import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { CourseDto, CreateCourseRequest, UpdateCourseRequest } from './course.models';

@Injectable({ providedIn: 'root' })
export class CourseService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;

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
