import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditEntry, CallDetail, CallSummary } from '../models/catalog.models';

@Injectable({ providedIn: 'root' })
export class CallService {
  constructor(private http: HttpClient) {}

  listCalls(): Observable<CallSummary[]> {
    return this.http.get<CallSummary[]>(`${environment.apiBaseUrl}/calls`);
  }

  getCallDetail(callId: string): Observable<CallDetail> {
    return this.http.get<CallDetail>(`${environment.apiBaseUrl}/calls/${callId}`);
  }

  getAuditLog(callId: string): Observable<AuditEntry[]> {
    return this.http.get<AuditEntry[]>(`${environment.apiBaseUrl}/calls/${callId}/audit`);
  }

  regenerateArtifact(callId: string, artifactType: string, model: string | null): Observable<unknown> {
    return this.http.post(`${environment.apiBaseUrl}/calls/${callId}/artifacts/${artifactType}/regenerate`, {
      model,
    });
  }

  deleteArtifact(callId: string, artifactType: string): Observable<unknown> {
    return this.http.delete(`${environment.apiBaseUrl}/calls/${callId}/artifacts/${artifactType}`);
  }
}
