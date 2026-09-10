import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChartGranularity, DashboardResponse, RecentActivity, SummaryStatistics } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/api/dashboard`;

  getDashboard(from?: string, to?: string, granularity: ChartGranularity = 'DAY'): Observable<DashboardResponse> {
    let params = new HttpParams().set('granularity', granularity);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<DashboardResponse>(this.baseUrl, { params });
  }

  getSummary(): Observable<SummaryStatistics> {
    return this.http.get<SummaryStatistics>(`${this.baseUrl}/summary`);
  }

  getRecentActivity(limit = 5): Observable<RecentActivity> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<RecentActivity>(`${this.baseUrl}/recent-activity`, { params });
  }
}

