import { JsonPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CallService } from '../../core/services/call.service';
import { ArtifactDetail, AuditEntry, CallDetail } from '../../core/models/catalog.models';

const DEFAULT_MODEL_BY_ARTIFACT_TYPE: Record<string, string> = {
  transcript: 'claude-opus-5',
  summary: 'claude-opus-5',
};

const POLL_INTERVAL_MS = 2000;
const MAX_POLL_ATTEMPTS = 30; // ~60s - generous enough for a real Anthropic call plus Kafka round trip

@Component({
  selector: 'app-call-detail',
  imports: [FormsModule, RouterLink, JsonPipe],
  templateUrl: './call-detail.html',
  styleUrl: './call-detail.css',
})
export class CallDetailPage implements OnInit {
  private callId!: string;

  readonly call = signal<CallDetail | null>(null);
  readonly auditLog = signal<AuditEntry[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionInFlight = signal<string | null>(null);
  readonly actionTimedOut = signal<string | null>(null);
  readonly modelByArtifactType: Record<string, string> = { ...DEFAULT_MODEL_BY_ARTIFACT_TYPE };

  constructor(
    private route: ActivatedRoute,
    private callService: CallService,
  ) {}

  ngOnInit(): void {
    this.callId = this.route.snapshot.paramMap.get('callId')!;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.callService.getCallDetail(this.callId).subscribe({
      next: (call) => {
        this.call.set(call);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load this call.');
        this.loading.set(false);
      },
    });
    this.loadAuditLog();
  }

  private loadAuditLog(): void {
    this.callService.getAuditLog(this.callId).subscribe({
      next: (entries) => this.auditLog.set(entries),
    });
  }

  regenerate(artifactType: string): void {
    const model = this.modelByArtifactType[artifactType] ?? null;
    const previousVersion = this.call()?.artifacts.find((a) => a.artifactType === artifactType)?.currentVersion ?? null;

    this.actionInFlight.set(artifactType);
    this.actionTimedOut.set(null);
    this.callService.regenerateArtifact(this.callId, artifactType, model).subscribe({
      next: () =>
        this.pollUntil(artifactType, (a) => (a?.currentVersion ?? null) !== previousVersion, MAX_POLL_ATTEMPTS),
      error: () => this.actionInFlight.set(null),
    });
  }

  delete(artifactType: string): void {
    if (!confirm(`Delete the current ${artifactType} version? This removes the file and cannot be undone.`)) {
      return;
    }
    this.actionInFlight.set(artifactType);
    this.actionTimedOut.set(null);
    this.callService.deleteArtifact(this.callId, artifactType).subscribe({
      next: () => this.pollUntil(artifactType, (a) => a?.deleted === true, MAX_POLL_ATTEMPTS),
      error: () => this.actionInFlight.set(null),
    });
  }

  /**
   * Both regenerate and delete complete on the backend via an async Kafka round trip (regenerate
   * additionally waits on a real LLM call) - there's no fixed delay that's reliably long enough
   * for either, so poll until the artifact reaches the expected state (or we give up).
   */
  private pollUntil(
    artifactType: string,
    isDone: (artifact: ArtifactDetail | undefined) => boolean,
    attemptsLeft: number,
  ): void {
    if (attemptsLeft <= 0) {
      this.actionInFlight.set(null);
      this.actionTimedOut.set(artifactType);
      return;
    }
    setTimeout(() => {
      this.callService.getCallDetail(this.callId).subscribe({
        next: (call) => {
          this.call.set(call);
          const artifact = call.artifacts.find((a) => a.artifactType === artifactType);
          if (isDone(artifact)) {
            this.actionInFlight.set(null);
            this.loadAuditLog();
          } else {
            this.pollUntil(artifactType, isDone, attemptsLeft - 1);
          }
        },
        error: () => this.actionInFlight.set(null),
      });
    }, POLL_INTERVAL_MS);
  }
}
