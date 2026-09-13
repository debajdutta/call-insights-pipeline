import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  submit(): void {
    this.error.set(null);
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
