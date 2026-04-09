import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { AuthSession } from './auth.models';
import { AuthService } from './auth.service';

const AUTH_STORAGE_KEY = 'quizmi.auth-session';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });

    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should refresh session and store new tokens', () => {
    seedSession();

    let result: AuthSession | undefined;

    service.refreshSession().subscribe((session) => {
      result = session;
    });

    const refreshRequest = httpMock.expectOne('http://localhost:8080/auth/refresh-token');
    expect(refreshRequest.request.method).toBe('POST');
    expect(refreshRequest.request.body).toEqual({ token: 'refresh-token-1' });

    refreshRequest.flush(authenticationResponse('access-token-2', 'refresh-token-2'));

    expect(result).toEqual(jasmine.objectContaining({ accessToken: 'access-token-2', refreshToken: 'refresh-token-2' }));
    expect(service.getAccessToken()).toBe('access-token-2');
    expect(service.getRefreshToken()).toBe('refresh-token-2');
    expect(readStoredSession()?.accessToken).toBe('access-token-2');
  });

  it('should reuse one refresh request while refresh is already in progress', () => {
    seedSession();

    const results: string[] = [];

    service.refreshSession().subscribe((session) => results.push(`first:${session.accessToken}`));
    service.refreshSession().subscribe((session) => results.push(`second:${session.accessToken}`));

    const refreshRequest = httpMock.expectOne('http://localhost:8080/auth/refresh-token');
    expect(refreshRequest.request.body).toEqual({ token: 'refresh-token-1' });

    refreshRequest.flush(authenticationResponse('access-token-2', 'refresh-token-2'));

    expect(results).toEqual(['first:access-token-2', 'second:access-token-2']);
  });

  it('should logout when refresh fails', () => {
    seedSession();

    let thrownStatus: number | undefined;

    service.refreshSession().subscribe({
      error: (error) => {
        thrownStatus = error.status;
      }
    });

    const refreshRequest = httpMock.expectOne('http://localhost:8080/auth/refresh-token');
    refreshRequest.flush({ message: 'Refresh token is invalid or expired.' }, { status: 401, statusText: 'Unauthorized' });

    expect(thrownStatus).toBe(401);
    expect(service.getAccessToken()).toBeNull();
    expect(readStoredSession()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  function seedSession() {
    localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({
        user: {
          id: 7,
          email: 'player@quizmi.app',
          role: 'USER'
        },
        accessToken: 'access-token-1',
        refreshToken: 'refresh-token-1'
      } satisfies AuthSession)
    );

    service = TestBed.inject(AuthService);
  }

  function readStoredSession(): AuthSession | null {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as AuthSession) : null;
  }

  function authenticationResponse(accessToken: string, refreshToken: string) {
    return {
      user: {
        id: 7,
        email: 'player@quizmi.app',
        role: 'USER'
      },
      accessToken: {
        value: accessToken
      },
      refreshToken: {
        value: refreshToken
      }
    };
  }
});
