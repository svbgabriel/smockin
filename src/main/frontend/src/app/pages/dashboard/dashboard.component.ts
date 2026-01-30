import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../../core/api.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert, MailMockResponse, RestMockResponse, S3BucketResponse, ServerStatusResponse } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AlertsComponent, I18nPipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  alerts: Alert[] = [];
  httpCount = 0;
  s3Count = 0;
  mailCount = 0;
  httpStatus: ServerStatusResponse | null = null;
  s3Status: ServerStatusResponse | null = null;
  mailStatus: ServerStatusResponse | null = null;

  constructor(private readonly api: ApiService, private readonly i18n: I18nService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.alerts = [];
    this.loadCounts();
    this.loadStatuses();
  }

  private loadCounts(): void {
    this.api.get<RestMockResponse[]>('/restmock').subscribe({
      next: (data) => (this.httpCount = data?.length ?? 0),
      error: () => this.showAlert(this.i18n.t('dashboard.errors.loadHttpMocks'))
    });

    this.api.get<S3BucketResponse[]>('/s3mock/bucket').subscribe({
      next: (data) => (this.s3Count = data?.length ?? 0),
      error: () => this.showAlert(this.i18n.t('dashboard.errors.loadS3Buckets'))
    });

    this.api.get<MailMockResponse[]>('/mailmock').subscribe({
      next: (data) => (this.mailCount = data?.length ?? 0),
      error: () => this.showAlert(this.i18n.t('dashboard.errors.loadMailInboxes'))
    });
  }

  private loadStatuses(): void {
    this.api.get<ServerStatusResponse>('/mockedserver/rest/status').subscribe({
      next: (data) => (this.httpStatus = data),
      error: () => this.showAlert(this.i18n.t('dashboard.errors.loadHttpStatus'))
    });

    this.api.get<ServerStatusResponse>('/mockedserver/s3/status').subscribe({
      next: (data) => (this.s3Status = data),
      error: () => this.showAlert(this.i18n.t('dashboard.errors.loadS3Status'))
    });

    this.api.get<ServerStatusResponse>('/mockedserver/mail/status').subscribe({
      next: (data) => (this.mailStatus = data),
      error: () => this.showAlert(this.i18n.t('dashboard.errors.loadMailStatus'))
    });
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
