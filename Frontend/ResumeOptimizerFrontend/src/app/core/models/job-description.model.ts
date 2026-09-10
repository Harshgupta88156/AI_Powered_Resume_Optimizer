export type JobDescriptionSource = 'FILE' | 'TEXT';

export interface JobDescriptionResponse {
  jobDescriptionId: number;
  company: string | null;
  jobTitle: string | null;
  source: JobDescriptionSource;
  fileName: string | null;
  contentType: string | null;
  cloudinaryUrl: string | null;
  createdAt: string;

  /** Full text. Only sent by the single-item endpoint (GET /job-descriptions/:id). */
  extractedText?: string | null;

  /** Short excerpt, sent with every list row. */
  textPreview?: string | null;

  /** Character count of the full text. */
  textLength?: number | null;
}

