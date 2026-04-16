import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../config/app-runtime-config';
import {
  CourseDto,
  CourseMemberDto,
  CourseMembersDto,
  CreateCourseRequest,
  UpdateCourseMemberRoleRequest,
  UpdateCourseRequest
} from './course.models';

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

  requestToJoinCourse(courseId: number): Observable<CourseDto> {
    return this.http.post<CourseDto>(`${this.apiBaseUrl}/courses/${courseId}/join-requests`, {});
  }

  fetchCourseMembers(courseId: number): Observable<CourseMembersDto> {
    return this.http.get<CourseMembersDto>(`${this.apiBaseUrl}/courses/${courseId}/members`);
  }

  approveCourseMember(courseId: number, memberUserId: number): Observable<CourseMemberDto> {
    return this.http.post<CourseMemberDto>(`${this.apiBaseUrl}/courses/${courseId}/members/${memberUserId}/approve`, {});
  }

  declineCourseJoinRequest(courseId: number, memberUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/courses/${courseId}/members/${memberUserId}/request`);
  }

  removeCourseMember(courseId: number, memberUserId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/courses/${courseId}/members/${memberUserId}`);
  }

  updateCourseMemberRole(
    courseId: number,
    memberUserId: number,
    payload: UpdateCourseMemberRoleRequest
  ): Observable<CourseMemberDto> {
    return this.http.put<CourseMemberDto>(`${this.apiBaseUrl}/courses/${courseId}/members/${memberUserId}/role`, payload);
  }

  createCourse(payload: CreateCourseRequest): Observable<CourseDto> {
    return this.http.post<CourseDto>(`${this.apiBaseUrl}/courses`, payload);
  }

  updateCourse(courseId: number, payload: UpdateCourseRequest): Observable<CourseDto> {
    return this.http.put<CourseDto>(`${this.apiBaseUrl}/courses/${courseId}`, payload);
  }
}
