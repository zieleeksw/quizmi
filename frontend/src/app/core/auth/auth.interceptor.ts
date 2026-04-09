import { HttpErrorResponse, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const isPublicAuthEndpoint = isPublicAuthEndpointRequest(request);
  const accessToken = authService.getAccessToken();

  const authenticatedRequest = !isPublicAuthEndpoint && accessToken ? withAccessToken(request, accessToken) : request;

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isPublicAuthEndpoint || !authService.getRefreshToken()) {
        return throwError(() => error);
      }

      return authService.refreshSession().pipe(
        switchMap((session) =>
          retryWithFreshToken(request, next, session.accessToken).pipe(
            catchError((retryError) => {
              if (retryError instanceof HttpErrorResponse && retryError.status === 401) {
                authService.handleUnauthorized();
              }

              return throwError(() => retryError);
            })
          )
        )
      );
    })
  );
};

function retryWithFreshToken(request: HttpRequest<unknown>, next: HttpHandlerFn, accessToken: string) {
  return next(withAccessToken(request, accessToken));
}

function withAccessToken(request: HttpRequest<unknown>, accessToken: string): HttpRequest<unknown> {
  return request.clone({
    setHeaders: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

function isPublicAuthEndpointRequest(request: HttpRequest<unknown>): boolean {
  return (
    request.url.includes('/auth/register') ||
    request.url.includes('/auth/login') ||
    request.url.includes('/auth/refresh-token')
  );
}
