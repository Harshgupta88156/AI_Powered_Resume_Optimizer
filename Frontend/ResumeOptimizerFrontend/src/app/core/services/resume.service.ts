import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/api-response.model';
import { ResumeResponse, ResumeUpdateRequest, ResumeVersionResponse } from '../models/resume.model';

@Injectable({ providedIn: 'root' })
export class ResumeService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/api/resumes`;

  list(page = 0, size = 20): Observable<Page<ResumeResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<ResumeResponse>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<ResumeResponse> {
    return this.http.get<ResumeResponse>(`${this.baseUrl}/${id}`);
  }

  upload(file: File, displayName?: string): Observable<ResumeResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (displayName) {
      formData.append('displayName', displayName);
    }
    return this.http.post<ResumeResponse>(this.baseUrl, formData);
  }

  update(id: number, payload: ResumeUpdateRequest): Observable<ResumeResponse> {
    return this.http.put<ResumeResponse>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  addVersion(resumeId: number, file: File): Observable<ResumeVersionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ResumeVersionResponse>(`${this.baseUrl}/${resumeId}/versions`, formData);
  }

  listVersions(resumeId: number): Observable<ResumeVersionResponse[]> {
    return this.http.get<ResumeVersionResponse[]>(`${this.baseUrl}/${resumeId}/versions`);
  }
}

