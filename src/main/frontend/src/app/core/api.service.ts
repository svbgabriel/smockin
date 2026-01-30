import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = environment.apiBaseUrl ?? '';

  constructor(private readonly http: HttpClient) {}

  get<T>(path: string): Observable<T> {
    return this.http.get<T>(this.resolveUrl(path));
  }

  post<T>(path: string, body: unknown, headers?: HttpHeaders): Observable<T> {
    return this.http.post<T>(this.resolveUrl(path), body, headers ? { headers } : undefined);
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(this.resolveUrl(path), body);
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http.patch<T>(this.resolveUrl(path), body);
  }

  delete<T>(path: string): Observable<T> {
    return this.http.delete<T>(this.resolveUrl(path));
  }

  postFormData<T>(path: string, formData: FormData, headers?: HttpHeaders): Observable<T> {
    return this.http.post<T>(this.resolveUrl(path), formData, headers ? { headers } : undefined);
  }

  private resolveUrl(path: string): string {
    if (path.startsWith('http://') || path.startsWith('https://')) {
      return path;
    }

    return `${this.baseUrl}${path}`;
  }
}
