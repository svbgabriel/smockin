import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { HttpClient } from '@angular/common/http';

import { JwtPayload, SimpleMessageResponse } from './models';
import { environment } from '../../environments/environment';

export interface AuthState {
  token: string;
  payload: JwtPayload | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly tokenKey = 'SMOCKIN_AUTH_TOKEN';
  private readonly baseUrl = environment.apiBaseUrl ?? '';
  private readonly stateSubject = new BehaviorSubject<AuthState | null>(this.loadState());

  readonly state$ = this.stateSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  login(username: string, password: string): Observable<SimpleMessageResponse> {
    return this.http.post<SimpleMessageResponse>(`${this.baseUrl}/auth`, { username, password }).pipe(
      tap((response) => {
        if (response?.message) {
          this.saveToken(response.message);
        }
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    this.stateSubject.next(null);
  }

  isLoggedIn(): boolean {
    return this.stateSubject.value !== null;
  }

  isAdmin(): boolean {
    const role = this.stateSubject.value?.payload?.role;
    return role === 'SYS_ADMIN' || role === 'ADMIN';
  }

  isSysAdmin(): boolean {
    return this.stateSubject.value?.payload?.role === 'SYS_ADMIN';
  }

  getToken(): string | null {
    return this.stateSubject.value?.token ?? null;
  }

  getUserName(): string | null {
    return this.stateSubject.value?.payload?.username ?? null;
  }

  getFullName(): string | null {
    return this.stateSubject.value?.payload?.name ?? null;
  }

  clearToken(): void {
    this.logout();
  }

  private saveToken(token: string): void {
    localStorage.setItem(this.tokenKey, token);
    this.stateSubject.next({ token, payload: this.parseJwt(token) });
  }

  private loadState(): AuthState | null {
    const token = localStorage.getItem(this.tokenKey);
    if (!token) {
      return null;
    }

    return {
      token,
      payload: this.parseJwt(token)
    };
  }

  private parseJwt(token: string): JwtPayload | null {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    try {
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
      const payload = JSON.parse(atob(padded));
      return payload as JwtPayload;
    } catch {
      return null;
    }
  }
}
