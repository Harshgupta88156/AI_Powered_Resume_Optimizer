import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  UserProfileResponse,
  UserProfileUpdateRequest,
  UserResponse,
  UserUpdateRequest
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {

  private http = inject(HttpClient);
  private baseUrl = `${environment.apiBaseUrl}/api/users`;

  /** Lightweight account record — used on the OAuth callback path. */
  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/me`);
  }

  updateMe(payload: UserUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/me`, payload);
  }

  /** Full profile: account fields plus location, experience, education, links. */
  getMyProfile(): Observable<UserProfileResponse> {
    return this.http.get<UserProfileResponse>(`${this.baseUrl}/me/profile`);
  }

  updateMyProfile(payload: UserProfileUpdateRequest): Observable<UserProfileResponse> {
    return this.http.put<UserProfileResponse>(`${this.baseUrl}/me/profile`, payload);
  }
}
