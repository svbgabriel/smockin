import { Component, OnDestroy } from '@angular/core';
import { NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert, ServerStatusResponse } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

interface WsClientRequest {
  url: string | null;
  body: string | null;
}

@Component({
  selector: 'app-ws-client',
  standalone: true,
  imports: [AlertsComponent, FormsModule, I18nPipe, NgIf],
  templateUrl: './ws-client.component.html',
  styleUrls: ['./ws-client.component.css']
})
export class WsClientComponent implements OnDestroy {
  alerts: Alert[] = [];
  remoteResponse = '';
  isBusy = false;

  clientRequest: WsClientRequest = {
    url: null,
    body: null
  };

  private wsSocket: WebSocket | null = null;

  constructor(private readonly api: ApiService, private readonly i18n: I18nService) {}

  ngOnDestroy(): void {
    this.terminate();
  }

  connect(): void {
    if (this.isConnected) {
      return;
    }

    this.alerts = [];

    if (this.isBlank(this.clientRequest.url)) {
      this.showAlert(this.i18n.t('wsClient.errors.pathRequired'));
      return;
    }

    if (!this.clientRequest.url?.startsWith('/')) {
      this.showAlert(this.i18n.t('wsClient.errors.pathInvalid'));
      return;
    }

    this.isBusy = true;
    this.api.get<ServerStatusResponse>('/mockedserver/rest/status').subscribe({
      next: (data) => {
        this.isBusy = false;
        if (!data?.running) {
          this.showAlert(this.i18n.t('wsClient.errors.serverNotRunning'));
          return;
        }
        this.openSocket(data.port);
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  send(): void {
    this.alerts = [];
    if (this.isBlank(this.clientRequest.body)) {
      this.showAlert(this.i18n.t('wsClient.errors.messageRequired'));
      return;
    }
    if (!this.wsSocket) {
      this.showAlert(this.i18n.t('wsClient.errors.noConnection'));
      return;
    }

    this.wsSocket.send(this.clientRequest.body ?? '');
    this.clientRequest.body = '';
  }

  terminate(): void {
    if (this.wsSocket) {
      this.wsSocket.close();
      this.wsSocket = null;
    }
  }

  clearResponse(): void {
    this.remoteResponse = '';
  }

  get isConnected(): boolean {
    return this.wsSocket !== null;
  }

  private openSocket(port: number): void {
    const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const host = window.location.hostname || 'localhost';
    const wsUrl = `${protocol}://${host}:${port}${this.clientRequest.url}`;

    try {
      this.wsSocket = new WebSocket(wsUrl);
      this.applyWsListeners();
    } catch (err) {
      this.appendResponse(this.i18n.t('wsClient.errors.unableToConnect', { url: this.clientRequest.url ?? '' }));
      this.wsSocket = null;
    }
  }

  private applyWsListeners(): void {
    if (!this.wsSocket) {
      return;
    }

    this.wsSocket.onopen = () => {
      this.clearResponse();
      this.appendResponse(this.i18n.t('wsClient.status.connectionEstablished'));
    };

    this.wsSocket.onmessage = (event) => {
      this.appendResponse(event.data);
    };

    this.wsSocket.onerror = () => {
      this.clearResponse();
      this.appendResponse(this.i18n.t('wsClient.errors.unableToConnect', { url: this.clientRequest.url ?? '' }));
      this.wsSocket = null;
    };

    this.wsSocket.onclose = () => {
      this.appendResponse(this.i18n.t('wsClient.status.connectionClosed'));
      this.wsSocket = null;
    };
  }

  private appendResponse(message: string): void {
    const time = this.formatTime(new Date());
    const entry = `${time}  -  ${message}\n\n`;
    this.remoteResponse = (this.remoteResponse ?? '').concat(entry);
  }

  private formatTime(date: Date): string {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${hours}:${minutes}:${seconds}`;
  }

  private isBlank(value: string | null | undefined): boolean {
    return value == null || String(value).trim().length === 0;
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
