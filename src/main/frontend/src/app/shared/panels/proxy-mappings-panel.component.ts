import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { NgFor, NgIf } from '@angular/common';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AlertsComponent } from '../alerts/alerts.component';
import { Alert } from '../../core/models';
import { ModalComponent } from '../modal/modal.component';
import { downloadBase64File } from '../../core/file-utils';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

interface ProxyForwardMapping {
  path: string | null;
  proxyForwardUrl: string | null;
  disabled: boolean;
}

interface ProxyMappingConfig {
  proxyMode: boolean;
  proxyModeType: string;
  doNotForwardWhen404Mock: boolean;
  proxyForwardMappings: ProxyForwardMapping[];
}

@Component({
  selector: 'app-proxy-mappings-panel',
  standalone: true,
  imports: [AlertsComponent, FormsModule, I18nPipe, ModalComponent, NgFor, NgIf],
  templateUrl: './proxy-mappings-panel.component.html',
  styleUrls: ['./proxy-mappings-panel.component.css']
})
export class ProxyMappingsPanelComponent implements OnInit {
  @Input() serverType = 'RESTFUL';
  @Output() close = new EventEmitter<void>();

  alerts: Alert[] = [];
  currentProxyMode = false;
  config: ProxyMappingConfig = {
    proxyMode: false,
    proxyModeType: 'ACTIVE',
    doNotForwardWhen404Mock: false,
    proxyForwardMappings: []
  };

  isBusy = false;
  keepExisting = true;
  importFile: File | null = null;

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly http: HttpClient,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.loadConfig();
  }

  get isReadOnly(): boolean {
    return this.auth.isLoggedIn() && !this.auth.isAdmin();
  }

  addRow(): void {
    this.config.proxyForwardMappings.push({ path: null, proxyForwardUrl: null, disabled: false });
  }

  removeRow(index: number): void {
    this.config.proxyForwardMappings.splice(index, 1);
  }

  toggleMapping(path: string | null): void {
    if (!path) {
      return;
    }
    const match = this.config.proxyForwardMappings.find((item) => item.path === path);
    if (match) {
      match.disabled = !match.disabled;
    }
  }

  save(): void {
    if (this.config.proxyMode && this.config.proxyForwardMappings.length > 0) {
      const seen = new Set<string>();
      for (const mapping of this.config.proxyForwardMappings) {
        if (!mapping.path) {
          this.showAlert(this.i18n.t('proxyMappings.errors.pathMissing'));
          return;
        }
        if (mapping.path !== '*' && !mapping.path.startsWith('/')) {
          this.showAlert(this.i18n.t('proxyMappings.errors.pathSlash', { path: mapping.path }));
          return;
        }
        if (!mapping.proxyForwardUrl) {
          this.showAlert(this.i18n.t('proxyMappings.errors.urlMissing'));
          return;
        }
        if (!mapping.proxyForwardUrl.startsWith('http://') && !mapping.proxyForwardUrl.startsWith('https://')) {
          this.showAlert(this.i18n.t('proxyMappings.errors.urlInvalid', { url: mapping.proxyForwardUrl }));
          return;
        }
        if (seen.has(mapping.path)) {
          this.showAlert(this.i18n.t('proxyMappings.errors.pathDuplicate', { path: mapping.path }));
          return;
        }
        seen.add(mapping.path);
      }
    }

    const req = {
      proxyModeType: this.config.proxyModeType,
      doNotForwardWhen404Mock: this.config.doNotForwardWhen404Mock,
      proxyForwardMappings: this.config.proxyForwardMappings
    };

    this.isBusy = true;
    this.api.post<any>(`/mockedserver/config/${this.serverType}/user/proxy`, req).subscribe({
      next: () => {
        if (!this.isReadOnly && this.currentProxyMode !== this.config.proxyMode) {
          this.toggleProxyMode(this.config.proxyMode);
          return;
        }
        this.isBusy = false;
        this.close.emit();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  toggleProxyMode(enabled: boolean): void {
    this.api
      .put<void>(`/mockedserver/config/${this.serverType}/proxy/mode?enableProxyMode=${enabled}`, {})
      .subscribe({
        next: () => {
          this.isBusy = false;
          this.close.emit();
        },
        error: () => {
          this.isBusy = false;
          this.showAlert(this.i18n.t('common.errors.generic'));
        }
      });
  }

  exportMappings(): void {
    if (this.config.proxyForwardMappings.length === 0) {
      return;
    }
    if (!confirm(this.i18n.t('proxyMappings.confirm.export'))) {
      return;
    }

    this.http
      .get(`/mockedserver/config/${this.serverType}/proxy/mappings/export`, {
        observe: 'response',
        responseType: 'text'
      })
      .subscribe({
        next: (resp) => {
          if (resp.status === 202) {
            this.showAlert(this.i18n.t('proxyMappings.errors.exportEmpty'));
            return;
          }
          if (resp.status !== 200 || !resp.body) {
            this.showAlert(this.i18n.t('common.errors.generic'));
            return;
          }
          downloadBase64File(resp.body, 'smockin_proxy_mappings_export.json', 'application/json');
        },
        error: () => this.showAlert(this.i18n.t('common.errors.generic'))
      });
  }

  selectImportFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.importFile = input.files?.[0] ?? null;
  }

  importMappings(): void {
    if (!this.importFile || !this.importFile.name.toLowerCase().endsWith('.json')) {
      this.showAlert(this.i18n.t('proxyMappings.errors.importFileType'));
      return;
    }

    const formData = new FormData();
    formData.append('file', this.importFile);

    this.isBusy = true;
    const headers = new HttpHeaders({ KeepExisting: String(this.keepExisting) });
    this.http
      .post(`/mockedserver/config/${this.serverType}/proxy/mappings/import`, formData, {
        observe: 'response',
        headers
      })
      .subscribe({
        next: (resp) => {
          this.isBusy = false;
          if (resp.status !== 204) {
            this.showAlert(this.i18n.t('proxyMappings.errors.importFailed'));
            return;
          }
          this.close.emit();
        },
        error: (err) => {
          this.isBusy = false;
          const message = err?.error?.message || this.i18n.t('proxyMappings.errors.importFailed');
          this.showAlert(message);
        }
      });
  }

  private loadConfig(): void {
    this.api.get<ProxyMappingConfig>(`/mockedserver/config/${this.serverType}/user/proxy`).subscribe({
      next: (data) => {
        this.currentProxyMode = data.proxyMode;
        this.config = {
          proxyMode: data.proxyMode,
          proxyModeType: data.proxyModeType,
          doNotForwardWhen404Mock: data.doNotForwardWhen404Mock,
          proxyForwardMappings: data.proxyForwardMappings ?? []
        };
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
