import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, shareReplay, throwError } from 'rxjs';

import { API_BASE_URL } from '../config/app-runtime-config';
import { AuthenticationDto, AuthSession, LoginRequest, RegisterRequest, UserDto } from './auth.models';

const AUTH_STORAGE_KEY = 'quizmi.auth-session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly apiBaseUrl = inject(API_BASE_URL);
  private readonly sessionState = signal<AuthSession | null>(this.restoreSession());
  private refreshRequest$: Observable<AuthSession> | null = null;

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

  refreshSession(): Observable<AuthSession> {
    const session = this.sessionState();

    if (!session?.refreshToken) {
      this.logout();
      return throwError(() => new Error('Refresh token is missing.'));
    }

    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    const refreshRequest = this.http
      .post<AuthenticationDto>(`${this.apiBaseUrl}/auth/refresh-token`, {
        token: session.refreshToken
      })
      .pipe(
        map((response) => this.storeSession(response)),
        catchError((error) => {
          this.logout();
          return throwError(() => error);
        }),
        finalize(() => {
          this.refreshRequest$ = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );

    this.refreshRequest$ = refreshRequest;
    return refreshRequest;
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

  getRefreshToken(): string | null {
    return this.sessionState()?.refreshToken ?? null;
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
