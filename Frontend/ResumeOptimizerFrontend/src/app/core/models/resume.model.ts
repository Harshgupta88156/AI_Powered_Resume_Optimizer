export interface ResumeResponse {
  resumeId: number;
  displayName: string;
  notes: string | null;
  fileName: string;
  contentType: string;
  cloudinaryUrl: string;
  latestVersionNumber: number;
  latestVersionId: number;
  totalVersions: number;
  createdAt: string;
  updatedAt: string;
}

export interface ResumeUpdateRequest {
  displayName: string;
  notes?: string | null;
}

export interface ResumeVersionResponse {
  resumeVersionId: number;
  resumeId: number;
  versionNumber: number;
  fileName: string;
  contentType: string;
  fileSizeBytes: number;
  cloudinaryUrl: string;
  createdAt: string;
}

