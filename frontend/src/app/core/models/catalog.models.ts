export interface ArtifactSummary {
  artifactType: string;
  currentVersion: number | null;
  deleted: boolean;
}

export interface CallSummary {
  callId: string;
  agentId: string;
  templateId: string;
  timestamp: string;
  artifacts: ArtifactSummary[];
}

export interface ArtifactVersion {
  version: number;
  path: string;
  modelUsed: string;
  generatedAt: string;
}

export interface ArtifactDetail {
  artifactType: string;
  currentVersion: number | null;
  deleted: boolean;
  versions: ArtifactVersion[];
  currentContent: unknown;
}

export interface CallDetail {
  callId: string;
  agentId: string;
  templateId: string;
  mediaPath: string;
  timestamp: string;
  artifacts: ArtifactDetail[];
}

export interface AuditEntry {
  callId: string;
  artifactType: string | null;
  action: string;
  version: number | null;
  modelUsed: string | null;
  actor: string;
  timestamp: string;
}
