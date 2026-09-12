import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/api-response.model';
import { JobDescriptionResponse } from '../models/job-description.model';

@Injectable({ providedIn: 'root' })
export class JobDescriptionService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/api/resumes/job-descriptions`;

  list(page = 0, size = 20): Observable<Page<JobDescriptionResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    return this.http.get<Page<JobDescriptionResponse>>(this.baseUrl, { params });
  }

  getById(id: number): Observable<JobDescriptionResponse> {
    return this.http.get<JobDescriptionResponse>(`${this.baseUrl}/${id}`);
  }

  getFile(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/file`, { responseType: 'blob' });
  }

  createFromFile(file: File, company?: string, jobTitle?: string): Observable<JobDescriptionResponse> {
    const formData = new FormData();
    formData.append('file', file);
    if (company) formData.append('company', company);
    if (jobTitle) formData.append('jobTitle', jobTitle);
    return this.http.post<JobDescriptionResponse>(this.baseUrl, formData);
  }

  createFromText(text: string, company?: string, jobTitle?: string): Observable<JobDescriptionResponse> {
    const formData = new FormData();
    formData.append('text', text);
    if (company) formData.append('company', company);
    if (jobTitle) formData.append('jobTitle', jobTitle);
    return this.http.post<JobDescriptionResponse>(this.baseUrl, formData);
  }

  /**
   * Renames / re-edits a job description. Omitted fields are left untouched,
   * so a rename can send only the title.
   */
  update(
    id: number,
    changes: { company?: string; jobTitle?: string; text?: string }
  ): Observable<JobDescriptionResponse> {
    return this.http.put<JobDescriptionResponse>(`${this.baseUrl}/${id}`, changes);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
