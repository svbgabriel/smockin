import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIf } from '@angular/common';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AlertsComponent } from '../alerts/alerts.component';
import { Alert } from '../../core/models';
import { ModalComponent } from '../modal/modal.component';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

interface ServerConfig {
  serverType: string;
  port: number;
  maxThreads: number;
  minThreads: number;
  timeOutMillis: number;
  autoStart: boolean;
  enableCors: boolean;
  autoGenInboxes: boolean;
  ngrokAuthToken: string | null;
}

@Component({
  selector: 'app-server-config-panel',
  standalone: true,
  imports: [AlertsComponent, FormsModule, I18nPipe, ModalComponent, NgIf],
  templateUrl: './server-config-panel.component.html',
  styleUrls: ['./server-config-panel.component.css']
})
export class ServerConfigPanelComponent implements OnInit {
  @Input() serverType = 'RESTFUL';
  @Output() close = new EventEmitter<{ restartReq?: boolean; reload?: boolean } | void>();

  alerts: Alert[] = [];
  config: ServerConfig = {
    serverType: '',
    port: 0,
    maxThreads: 0,
    minThreads: 0,
    timeOutMillis: 0,
    autoStart: false,
    enableCors: false,
    autoGenInboxes: false,
    ngrokAuthToken: null
  };

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.loadConfig();
  }

  get isReadOnly(): boolean {
    return this.auth.isLoggedIn() && !this.auth.isAdmin();
  }

  save(): void {
    if (this.isReadOnly) {
      return;
    }

    if (!this.isNumeric(this.config.port)) {
      this.showAlert(this.i18n.t('serverConfig.errors.portRequired'));
      return;
    }

    if (this.serverType === 'RESTFUL') {
      if (!this.isNumeric(this.config.maxThreads)) {
        this.showAlert(this.i18n.t('serverConfig.errors.maxThreadsRequired'));
        return;
      }
      if (!this.isNumeric(this.config.minThreads)) {
        this.showAlert(this.i18n.t('serverConfig.errors.minThreadsRequired'));
        return;
      }
      if (!this.isNumeric(this.config.timeOutMillis)) {
        this.showAlert(this.i18n.t('serverConfig.errors.idleTimeoutRequired'));
        return;
      }
    }

    const req = {
      serverType: this.serverType,
      port: this.config.port,
      maxThreads: this.config.maxThreads,
      minThreads: this.config.minThreads,
      timeOutMillis: this.config.timeOutMillis,
      autoStart: this.config.autoStart,
      nativeProperties: this.buildNativeProperties()
    };

    this.api.put<void>(`/mockedserver/config/${this.serverType}`, req).subscribe({
      next: () => {
        this.close.emit({ restartReq: true });
      },
      error: () => {
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  purgeMailMessages(store: 'CACHE' | 'DB'): void {
    if (!confirm(this.i18n.t('serverConfig.confirm.purgeMail', { store }))) {
      return;
    }

    this.api.delete<void>(`/mockedserver/mail/clear/${store}`).subscribe({
      next: () => {
        this.close.emit({ reload: true });
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private loadConfig(): void {
    this.api.get<any>(`/mockedserver/config/${this.serverType}`).subscribe({
      next: (data) => {
        this.config = {
          serverType: data.serverType,
          port: data.port,
          maxThreads: data.maxThreads,
          minThreads: data.minThreads,
          timeOutMillis: data.timeOutMillis,
          autoStart: data.autoStart,
          enableCors: data.nativeProperties?.['ENABLE_CORS']?.toUpperCase() === 'TRUE',
          autoGenInboxes: data.nativeProperties?.['AUTO_GEN_INBOXES']?.toUpperCase() === 'TRUE',
          ngrokAuthToken: data.nativeProperties?.['NGROK_AUTH_TOKEN'] ?? null
        };
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private buildNativeProperties(): Record<string, string> {
    const nativeProps: Record<string, string> = {};

    if (this.serverType === 'RESTFUL') {
      nativeProps['ENABLE_CORS'] = this.config.enableCors ? 'TRUE' : 'FALSE';
      if (this.config.ngrokAuthToken) {
        nativeProps['NGROK_AUTH_TOKEN'] = this.config.ngrokAuthToken;
      }
    }

    if (this.serverType === 'MAIL') {
      nativeProps['AUTO_GEN_INBOXES'] = this.config.autoGenInboxes ? 'TRUE' : 'FALSE';
    }

    return nativeProps;
  }

  private isNumeric(value: number | null): boolean {
    return value !== null && !Number.isNaN(Number(value));
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
