import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { CallService } from '../../core/services/call.service';
import { CallSummary } from '../../core/models/catalog.models';

@Component({
  selector: 'app-call-list',
  imports: [RouterLink],
  templateUrl: './call-list.html',
  styleUrl: './call-list.css',
})
export class CallList implements OnInit {
  readonly calls = signal<CallSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor(
    private callService: CallService,
    protected authService: AuthService,
  ) {}

  ngOnInit(): void {
    this.callService.listCalls().subscribe({
      next: (calls) => {
        this.calls.set(calls);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load calls.');
        this.loading.set(false);
      },
    });
  }
}
