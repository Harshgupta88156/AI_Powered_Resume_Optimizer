export type ResourceType = 'DOCS' | 'COURSE' | 'VIDEO' | 'PRACTICE' | 'BOOK' | 'ROADMAP';
export type ResourceLevel = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

/** Served by GET /api/resumes/analyses/:id/learning-resources. */
export interface LearningResourceResponse {
  skill: string;
  title: string;
  description: string;
  url: string;
  provider: string;
  resourceType: ResourceType;
  level: ResourceLevel;
  category: string;
  estimatedHours: number | null;
  free: boolean;
}
