import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map } from 'rxjs';

import { AuthenticationDto, AuthSession, LoginRequest, RegisterRequest, UserDto } from './auth.models';

const AUTH_STORAGE_KEY = 'quizmi.auth-session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiBaseUrl = `${window.location.protocol}//${window.location.hostname}:8080`;
  private readonly sessionState = signal<AuthSession | null>(this.restoreSession());

  readonly session = this.sessionState.asReadonly();
  readonly user = computed(() => this.sessionState()?.user ?? null);
  readonly isAuthenticated = computed(() => Boolean(this.sessionState()?.accessToken));

  register(payload: RegisterRequest): Observable<UserDto> {
    return this.http.post<UserDto>(`${this.apiBaseUrl}/auth/register`, payload);
  }

  login(payload: LoginRequest): Observable<AuthSession> {
    return this.http
      .post<AuthenticationDto>(`${this.apiBaseUrl}/auth/login`, payload)
      .pipe(map((response) => this.storeSession(response)));
  }

  logout(redirect = true): void {
    this.clearSession();

    if (redirect) {
      void this.router.navigateByUrl('/login');
    }
  }

  getAccessToken(): string | null {
    return this.sessionState()?.accessToken ?? null;
  }

  handleUnauthorized(): void {
    this.logout();
  }

  private storeSession(response: AuthenticationDto): AuthSession {
    const session: AuthSession = {
      user: response.user,
      accessToken: response.accessToken.value,
      refreshToken: response.refreshToken.value
    };

    this.sessionState.set(session);
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
    return session;
  }

  private restoreSession(): AuthSession | null {
    const rawSession = localStorage.getItem(AUTH_STORAGE_KEY);

    if (!rawSession) {
      return null;
    }

    try {
      const parsed = JSON.parse(rawSession) as Partial<AuthSession>;

      if (
        !parsed.accessToken ||
        !parsed.refreshToken ||
        !parsed.user?.email ||
        typeof parsed.user.id !== 'number' ||
        typeof parsed.user.role !== 'string'
      ) {
        localStorage.removeItem(AUTH_STORAGE_KEY);
        return null;
      }

      return parsed as AuthSession;
    } catch {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }
  }

  private clearSession(): void {
    this.sessionState.set(null);
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }
}
