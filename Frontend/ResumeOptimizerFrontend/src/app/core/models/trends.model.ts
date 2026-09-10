export interface LabelCount {
  label: string;
  count: number;
}

export type TrendGranularity = 'DAY' | 'WEEK' | 'MONTH';

export interface TimeSeriesPoint {
  date: string;
  label: string;
  count: number;
}

export interface TrendsQueryParams {
  from?: string;
  to?: string;
  limit?: number;
  granularity?: TrendGranularity;
}

