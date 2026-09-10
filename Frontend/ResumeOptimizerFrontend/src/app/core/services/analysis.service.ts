import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/api-response.model';
import { AnalysisResponse, AnalysisTriggerRequest, MarkdownResumeResponse } from '../models/analysis.model';
import { LearningResourceResponse } from '../models/learning-resource.model';

@Injectable({ providedIn: 'root' })
export class AnalysisService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/api/resumes/analyses`;

  trigger(payload: AnalysisTriggerRequest): Observable<AnalysisResponse> {
    return this.http.post<AnalysisResponse>(this.baseUrl, payload);
  }

  list(params: { resumeId?: number; resumeVersionId?: number; jobDescriptionId?: number; page?: number; size?: number } = {}): Observable<Page<AnalysisResponse>> {
    let httpParams = new HttpParams()
      .set('page', params.page ?? 0)
      .set('size', params.size ?? 20)
      .set('sort', 'createdAt,desc');
    if (params.resumeId != null) httpParams = httpParams.set('resumeId', params.resumeId);
    if (params.resumeVersionId != null) httpParams = httpParams.set('resumeVersionId', params.resumeVersionId);
    if (params.jobDescriptionId != null) httpParams = httpParams.set('jobDescriptionId', params.jobDescriptionId);
    return this.http.get<Page<AnalysisResponse>>(this.baseUrl, { params: httpParams });
  }

  getById(id: number): Observable<AnalysisResponse> {
    return this.http.get<AnalysisResponse>(`${this.baseUrl}/${id}`);
  }

  generateMarkdown(id: number, regenerate = false): Observable<MarkdownResumeResponse> {
    const params = new HttpParams().set('regenerate', regenerate);
    return this.http.post<MarkdownResumeResponse>(`${this.baseUrl}/${id}/markdown`, null, { params });
  }

  /**
   * Returns a previously generated resume without asking the AI for a new one.
   * Backed by GET /markdown, which 404s when nothing has been generated yet -
   * callers treat that as "no cached copy", not as an error worth showing.
   */
  getMarkdown(id: number): Observable<MarkdownResumeResponse> {
    return this.http.get<MarkdownResumeResponse>(`${this.baseUrl}/${id}/markdown`);
  }

  /**
   * Curated learning resources for this analysis's skill gaps.
   * Served from the backend catalog, so links can be corrected without a
   * frontend release.
   */
  getLearningResources(id: number, limit = 12): Observable<LearningResourceResponse[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<LearningResourceResponse[]>(
      `${this.baseUrl}/${id}/learning-resources`, { params });
  }
}

