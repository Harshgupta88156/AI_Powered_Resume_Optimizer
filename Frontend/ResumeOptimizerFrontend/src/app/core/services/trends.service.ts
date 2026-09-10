import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LabelCount, TimeSeriesPoint, TrendsQueryParams } from '../models/trends.model';

@Injectable({ providedIn: 'root' })
export class TrendsService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/api/trends`;

  private buildParams(query: TrendsQueryParams): HttpParams {
    let params = new HttpParams();
    if (query.from) params = params.set('from', query.from);
    if (query.to) params = params.set('to', query.to);
    if (query.limit != null) params = params.set('limit', query.limit);
    if (query.granularity) params = params.set('granularity', query.granularity);
    return params;
  }

  missingSkills(query: TrendsQueryParams = {}): Observable<LabelCount[]> {
    return this.http.get<LabelCount[]>(`${this.baseUrl}/missing-skills`, { params: this.buildParams(query) });
  }

  suggestedSkills(query: TrendsQueryParams = {}): Observable<LabelCount[]> {
    return this.http.get<LabelCount[]>(`${this.baseUrl}/suggested-skills`, { params: this.buildParams(query) });
  }

  technologies(query: TrendsQueryParams = {}): Observable<LabelCount[]> {
    return this.http.get<LabelCount[]>(`${this.baseUrl}/technologies`, { params: this.buildParams(query) });
  }

  companies(query: TrendsQueryParams = {}): Observable<LabelCount[]> {
    return this.http.get<LabelCount[]>(`${this.baseUrl}/companies`, { params: this.buildParams(query) });
  }

  jobTitles(query: TrendsQueryParams = {}): Observable<LabelCount[]> {
    return this.http.get<LabelCount[]>(`${this.baseUrl}/job-titles`, { params: this.buildParams(query) });
  }

  uploadActivity(query: TrendsQueryParams = {}): Observable<TimeSeriesPoint[]> {
    return this.http.get<TimeSeriesPoint[]>(`${this.baseUrl}/upload-activity`, { params: this.buildParams(query) });
  }
}

