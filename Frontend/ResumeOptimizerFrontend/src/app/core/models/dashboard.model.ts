export interface SummaryStatistics {
  totalResumes: number;
  totalResumeVersions: number;
  totalAnalyses: number;
  totalJobDescriptions: number;
  totalUnreadNotifications: number;
  completedAnalyses: number;
  averageAtsScore: number | null;
  averageMatchScore: number | null;
}

export interface RecentResumeItem {
  resumeId: number;
  displayName: string;
  createdAt: string;
}

export interface RecentAnalysisItem {
  analysisId: number;
  resumeDisplayName: string;
  jobTitle: string | null;
  company: string | null;
  status: string;
  atsScore: number | null;
  matchScore: number | null;
  createdAt: string;
}

export interface RecentActivity {
  recentResumes: RecentResumeItem[];
  recentAnalyses: RecentAnalysisItem[];
  recentNotifications: unknown[];
}

export type ChartGranularity = 'DAY' | 'WEEK' | 'MONTH';

export interface ChartPoint {
  date: string;
  label?: string;
  count: number;
}

export interface DashboardCharts {
  [key: string]: ChartPoint[] | undefined;
}

export interface DashboardInsights {
  insights?: string[];
}

export interface DashboardResponse {
  summary: SummaryStatistics;
  recentActivity: RecentActivity;
  charts: DashboardCharts;
  insights: DashboardInsights;
  generatedAt: string;
  degradedSources: string[];
}

