import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

interface LoginResponse {
  token: string;
  username: string;
  expiresInSeconds: number;
}

const TOKEN_STORAGE_KEY = 'call-insights.token';
const USERNAME_STORAGE_KEY = 'call-insights.username';

/**
 * localStorage, not a secure httpOnly cookie - fine for this project's local-dev-only,
 * no-production-hardening scope (SPEC.md non-goals), not something to carry into a real deployment.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly username = signal<string | null>(localStorage.getItem(USERNAME_STORAGE_KEY));

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, { username, password })
      .pipe(
        tap((response) => {
          localStorage.setItem(TOKEN_STORAGE_KEY, response.token);
          localStorage.setItem(USERNAME_STORAGE_KEY, response.username);
          this.username.set(response.username);
        }),
      );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USERNAME_STORAGE_KEY);
    this.username.set(null);
  }

  getToken(): string | null {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  }

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  }
}
