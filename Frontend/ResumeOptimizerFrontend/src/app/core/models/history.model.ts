import { AnalysisStatus } from './analysis.model';

export interface ResumeRef {
  resumeId: number;
  displayName: string;
}

export interface ResumeVersionRef {
  resumeVersionId: number;
  versionNumber: number;
}

export interface JobDescriptionRef {
  jobDescriptionId: number;
  company: string | null;
  jobTitle: string | null;
}

export interface HistoryTimelineItemResponse {
  analysisId: number;
  resume: ResumeRef;
  resumeVersion: ResumeVersionRef;
  jobDescription: JobDescriptionRef;
  status: AnalysisStatus;
  atsScore: number | null;
  matchScore: number | null;
  overallSummary: string | null;
  missingSkills: string[];
  suggestedSkills: string[];
  suggestions: string[];
  errorMessage: string | null;
  engineVersion: string | null;
  analysisDate: string;
  completedAt: string | null;
}

export interface HistoryFilterParams {
  resumeId?: number;
  resumeVersionId?: number;
  jobDescriptionId?: number;
  company?: string;
  jobTitle?: string;
  atsMin?: number;
  atsMax?: number;
  matchMin?: number;
  matchMax?: number;
  from?: string;
  to?: string;
  status?: AnalysisStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ScoreDelta {
  atsScoreDelta?: number;
  matchScoreDelta?: number;
}

export interface SkillDelta {
  added: string[];
  removed: string[];
}

// export interface AnalysisComparisonResponse {
//   left: HistoryTimelineItemResponse;
//   right: HistoryTimelineItemResponse;
//   delta: ScoreDelta;
//   missingSkills: SkillDelta;
//   suggestedSkills: SkillDelta;
// }
export interface AnalysisComparisonResponse {
  left: HistoryTimelineItemResponse;
  right: HistoryTimelineItemResponse;

  delta: {
    atsDelta: number | null;
    matchDelta: number | null;
  };

  missingSkills: {
    added: string[];
    removed: string[];
  };

  suggestedSkills: {
    added: string[];
    removed: string[];
  };
}
