import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RegisterRequest, UserDto } from './registration.models';

@Injectable({ providedIn: 'root' })
export class RegistrationService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;

  register(payload: RegisterRequest): Observable<UserDto> {
    return this.http.post<UserDto>(`${this.apiBaseUrl}/auth/register`, payload);
  }
}
