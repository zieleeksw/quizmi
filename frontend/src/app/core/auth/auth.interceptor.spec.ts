import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';

import { AuthSession } from './auth.models';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

const AUTH_STORAGE_KEY = 'quizmi.auth-session';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.returnValue(Promise.resolve(true));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should attach authorization header to protected requests', () => {
    seedSession();

    httpClient.get('/api/protected').subscribe();

    const protectedRequest = httpMock.expectOne('/api/protected');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer access-token-1');
    protectedRequest.flush({ ok: true });
  });

  it('should refresh expired token and retry the original request', () => {
    seedSession();

    let responseBody: unknown;

    httpClient.get('/api/protected').subscribe((response) => {
      responseBody = response;
    });

    const protectedRequest = httpMock.expectOne('/api/protected');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer access-token-1');
    protectedRequest.flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });

    const refreshRequest = httpMock.expectOne('http://localhost:8080/auth/refresh-token');
    expect(refreshRequest.request.body).toEqual({ token: 'refresh-token-1' });
    refreshRequest.flush(authenticationResponse('access-token-2', 'refresh-token-2'));

    const retriedRequest = httpMock.expectOne('/api/protected');
    expect(retriedRequest.request.headers.get('Authorization')).toBe('Bearer access-token-2');
    retriedRequest.flush({ ok: true });

    expect(responseBody).toEqual({ ok: true });
    expect(readStoredSession()?.accessToken).toBe('access-token-2');
  });

  it('should logout when refresh also fails', () => {
    seedSession();

    let thrownStatus: number | undefined;

    httpClient.get('/api/protected').subscribe({
      error: (error) => {
        thrownStatus = error.status;
      }
    });

    const protectedRequest = httpMock.expectOne('/api/protected');
    protectedRequest.flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });

    const refreshRequest = httpMock.expectOne('http://localhost:8080/auth/refresh-token');
    refreshRequest.flush({ message: 'invalid refresh token' }, { status: 401, statusText: 'Unauthorized' });

    expect(thrownStatus).toBe(401);
    expect(readStoredSession()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('should use one refresh request for concurrent unauthorized calls', () => {
    seedSession();

    const results: unknown[] = [];

    httpClient.get('/api/alpha').subscribe((response) => results.push(response));
    httpClient.get('/api/beta').subscribe((response) => results.push(response));

    const alphaRequest = httpMock.expectOne('/api/alpha');
    const betaRequest = httpMock.expectOne('/api/beta');

    alphaRequest.flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });
    betaRequest.flush({ message: 'expired' }, { status: 401, statusText: 'Unauthorized' });

    const refreshRequest = httpMock.expectOne('http://localhost:8080/auth/refresh-token');
    refreshRequest.flush(authenticationResponse('access-token-2', 'refresh-token-2'));

    const retriedAlpha = httpMock.expectOne('/api/alpha');
    const retriedBeta = httpMock.expectOne('/api/beta');

    expect(retriedAlpha.request.headers.get('Authorization')).toBe('Bearer access-token-2');
    expect(retriedBeta.request.headers.get('Authorization')).toBe('Bearer access-token-2');

    retriedAlpha.flush({ ok: 'alpha' });
    retriedBeta.flush({ ok: 'beta' });

    expect(results).toEqual([{ ok: 'alpha' }, { ok: 'beta' }]);
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

    TestBed.inject(AuthService);
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
