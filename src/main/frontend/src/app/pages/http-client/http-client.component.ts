import { Component } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert } from '../../core/models';
import { HTTP_METHODS } from '../../core/constants';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

interface HeaderRow {
  name: string | null;
  value: string | null;
}

interface ClientRequest {
  url: string | null;
  method: string | null;
  body: string | null;
  headers: HeaderRow[];
}

interface HttpClientResponse {
  status: number;
  contentType: string;
  headers: Record<string, string>;
  body: string;
}

@Component({
  selector: 'app-http-client',
  standalone: true,
  imports: [AlertsComponent, FormsModule, I18nPipe, NgFor, NgIf],
  templateUrl: './http-client.component.html',
  styleUrls: ['./http-client.component.css']
})
export class HttpClientComponent {
  alerts: Alert[] = [];
  httpMethods = HTTP_METHODS;
  isBusy = false;
  clientResponse = '';

  clientRequest: ClientRequest = {
    url: null,
    method: null,
    body: null,
    headers: []
  };

  constructor(private readonly api: ApiService, private readonly i18n: I18nService) {}

  addHeaderRow(): void {
    this.clientRequest.headers.push({ name: null, value: null });
  }

  removeHeaderRow(index: number): void {
    this.clientRequest.headers.splice(index, 1);
  }

  clear(): void {
    this.clientRequest = {
      url: null,
      method: null,
      body: null,
      headers: []
    };
    this.clientResponse = '';
    this.alerts = [];
  }

  send(): void {
    this.clientResponse = '';
    this.alerts = [];

    if (this.isBlank(this.clientRequest.url)) {
      this.showAlert(this.i18n.t('httpClient.errors.pathRequired'));
      return;
    }

    if (!this.clientRequest.url?.startsWith('/')) {
      this.showAlert(this.i18n.t('httpClient.errors.pathInvalid'));
      return;
    }

    if (this.isBlank(this.clientRequest.method)) {
      this.showAlert(this.i18n.t('httpClient.errors.methodRequired'));
      return;
    }

    if (['POST', 'PUT', 'PATCH'].includes(this.clientRequest.method ?? '') && this.clientRequest.body == null) {
      this.showAlert(this.i18n.t('httpClient.errors.bodyRequired'));
      return;
    }

    for (const header of this.clientRequest.headers) {
      if (this.isBlank(header.name) || this.isBlank(header.value)) {
        this.showAlert(this.i18n.t('httpClient.errors.headersIncomplete'));
        return;
      }
    }

    const headers: Record<string, string> = {};
    for (const header of this.clientRequest.headers) {
      if (header.name && header.value) {
        headers[header.name] = header.value;
      }
    }

    const req = {
      method: this.clientRequest.method,
      headers,
      url: this.clientRequest.url,
      body: this.clientRequest.body
    };

    this.isBusy = true;
    this.api.post<HttpClientResponse>('/httpclientcall', req).subscribe({
      next: (data) => {
        this.isBusy = false;
        this.clientResponse = this.formatResponse(data, req.url ?? '');
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  private formatResponse(data: HttpClientResponse, url: string): string {
    if (data.status === 404) {
      return (
        this.i18n.t('httpClient.response.statusLine', { status: data.status }) +
        '\n\n' +
        this.i18n.t('httpClient.response.errorIntro') +
        '\n' +
        ` ${url}\n\n` +
        this.i18n.t('httpClient.response.errorHintEndpoint', { url }) +
        '\n' +
        this.i18n.t('httpClient.response.errorHintServer')
      );
    }

    const headersText = this.headersToString(data.headers ?? {});
    return (
      this.i18n.t('httpClient.response.statusLine', { status: data.status }) +
      '\n\n' +
      this.i18n.t('httpClient.response.headersLabel') +
      `\n${headersText}\n${data.body ?? ''}`
    );
  }

  private headersToString(headers: Record<string, string>): string {
    const lines: string[] = [];
    for (const key of Object.keys(headers)) {
      lines.push(`   ${key}: ${headers[key]}`);
    }
    return lines.join('\n');
  }

  private isBlank(value: string | null | undefined): boolean {
    return value == null || String(value).trim().length === 0;
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
