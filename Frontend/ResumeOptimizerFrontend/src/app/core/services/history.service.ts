import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/api-response.model';
import { AnalysisComparisonResponse, HistoryFilterParams, HistoryTimelineItemResponse } from '../models/history.model';

@Injectable({ providedIn: 'root' })
export class HistoryService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/api/history`;

  search(filters: HistoryFilterParams): Observable<Page<HistoryTimelineItemResponse>> {
    let params = new HttpParams()
      .set('page', filters.page ?? 0)
      .set('size', filters.size ?? 20)
      // .set('sort', filters.sort ?? 'analysisDate,desc');

    const optionalKeys: (keyof HistoryFilterParams)[] = [
      'resumeId', 'resumeVersionId', 'jobDescriptionId', 'company', 'jobTitle',
      'atsMin', 'atsMax', 'matchMin', 'matchMax', 'from', 'to', 'status'
    ];
    for (const key of optionalKeys) {
      const value = filters[key];
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }

    return this.http.get<Page<HistoryTimelineItemResponse>>(this.baseUrl, { params });
  }

  byResume(resumeId: number, page = 0, size = 20): Observable<Page<HistoryTimelineItemResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<HistoryTimelineItemResponse>>(`${this.baseUrl}/resumes/${resumeId}`, { params });
  }

  byJobDescription(jobDescriptionId: number, page = 0, size = 20): Observable<Page<HistoryTimelineItemResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<HistoryTimelineItemResponse>>(`${this.baseUrl}/job-descriptions/${jobDescriptionId}`, { params });
  }

  compare(leftAnalysisId: number, rightAnalysisId: number): Observable<AnalysisComparisonResponse> {
    const params = new HttpParams().set('leftAnalysisId', leftAnalysisId).set('rightAnalysisId', rightAnalysisId);
    return this.http.get<AnalysisComparisonResponse>(`${this.baseUrl}/compare`, { params });
  }
}

