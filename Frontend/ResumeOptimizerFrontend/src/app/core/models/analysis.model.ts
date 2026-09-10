export type AnalysisStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AnalysisTriggerRequest {
  resumeId: number;
  jobDescriptionId: number;
  resumeVersionId?: number | null;
}

export interface AnalysisResponse {
  analysisId: number;
  resumeId: number;
  resumeDisplayName: string;
  resumeVersionId: number;
  resumeVersionNumber: number;
  jobDescriptionId: number;
  company: string | null;
  jobTitle: string | null;
  status: AnalysisStatus;
  atsScore: number | null;
  matchScore: number | null;
  overallSummary: string | null;
  matchingSkills: string[];
  missingSkills: string[];
  strengths: string[];
  weaknesses: string[];
  suggestedSkills: string[];
  suggestions: string[];
  errorMessage: string | null;
  engineVersion: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface MarkdownResumeResponse {
  analysisId: number;
  markdownCode: string;
  engineVersion: string | null;
}

