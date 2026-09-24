import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  username = '';
  password = '';
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  // Set only when authErrorInterceptor sent us here after a 401 - distinct from `error` so a
  // wrong-password retry doesn't get stuck showing a stale "session expired" message.
  readonly sessionExpired = signal(false);

  constructor(
    private authService: AuthService,
    private router: Router,
    route: ActivatedRoute,
  ) {
    this.sessionExpired.set(route.snapshot.queryParamMap.get('reason') === 'expired');
  }

  submit(): void {
    this.error.set(null);
    this.sessionExpired.set(false);
    this.submitting.set(true);
    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/calls']);
      },
      error: () => {
        this.submitting.set(false);
        this.error.set('Invalid username or password.');
      },
    });
  }
}
