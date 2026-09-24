import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/** A 401 here always means "no longer authenticated" - login is the only public endpoint,
 * so any other 401 means the token expired or was rejected. Clear the stale session and send
 * the user back to a login form that explains why, rather than leaving them on a page that
 * will just keep failing with no explanation. The `reason=expired` query param is how the
 * login page knows to show that explanation instead of rendering as a blank form. */
export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        authService.logout();
        router.navigate(['/login'], { queryParams: { reason: 'expired' } });
      }
      return throwError(() => error);
    }),
  );
};
