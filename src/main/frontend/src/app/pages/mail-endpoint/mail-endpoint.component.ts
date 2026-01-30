import { Component, OnInit } from '@angular/core';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpHeaders } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { ModalComponent } from '../../shared/modal/modal.component';
import {
  Alert,
  AttachmentContent,
  MailAttachment,
  MailMessage,
  MailMessagePage,
  MailMockDetail,
  MailMockResponse,
  ServerStatusResponse
} from '../../core/models';
import { downloadBase64File } from '../../core/file-utils';
import { ACTIVE_STATUS, INACTIVE_STATUS } from '../../core/constants';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

interface MailEndpointForm {
  extId: string | null;
  address: string | null;
  status: string;
  saveReceivedMail: boolean;
  retainCachedMail: boolean;
  createdBy?: string | null;
}

@Component({
  selector: 'app-mail-endpoint',
  standalone: true,
  imports: [AlertsComponent, DatePipe, FormsModule, I18nPipe, ModalComponent, NgFor, NgIf],
  templateUrl: './mail-endpoint.component.html',
  styleUrls: ['./mail-endpoint.component.css']
})
export class MailEndpointComponent implements OnInit {
  alerts: Alert[] = [];
  mockServerStatus = 'Stopped';
  mailServices: MailMockResponse[] = [];
  allMailServices: MailMockResponse[] = [];
  mockSelection = new Set<string>();
  searchFilter = '';
  isBusy = false;
  showImport = false;
  keepExisting = false;
  importFile: File | null = null;

  viewMode: 'list' | 'editor' = 'list';
  isNew = false;
  currentExtId: string | null = null;
  endpoint: MailEndpointForm = this.defaultEndpoint();

  showIncludeMailMessagesInSavePrompt = false;
  showPurgeSavedMailWarning = false;
  currentSaveReceivedMailState = false;

  currentPageIndex = 0;
  maxPageIndex = 0;
  recordsPerPage = 0;
  mailMessagesTotal = 0;
  mailMessages: MailMessage[] = [];
  messagesSelection: string[] = [];
  mailMessageSearch = '';

  messageModalOpen = false;
  messageModalData: MailMessage | null = null;
  messageModalAttachments: MailAttachment[] = [];
  messageModalLoading = false;

  readonly statusRunning = 'Running';
  readonly statusStopped = 'Stopped';
  readonly activeStatus = ACTIVE_STATUS;
  readonly inactiveStatus = INACTIVE_STATUS;

  constructor(
    private readonly api: ApiService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly i18n: I18nService
  ) {}

  ngOnInit(): void {
    this.loadTableData();
    this.loadServerStatus();

    this.route.queryParamMap.subscribe((params) => {
      const extId = params.get('eid');
      const isNew = params.get('new');

      if (extId) {
        this.openEditor(extId);
        return;
      }

      if (isNew) {
        this.openEditor();
        return;
      }

      this.viewMode = 'list';
      this.currentExtId = null;
    });
  }

  get isReadOnly(): boolean {
    return this.auth.isLoggedIn() && !this.auth.isAdmin();
  }

  get isOwnerReadOnly(): boolean {
    if (!this.endpoint.createdBy || !this.auth.isLoggedIn()) {
      return false;
    }
    return this.endpoint.createdBy !== this.auth.getUserName();
  }

  get isEditorReadOnly(): boolean {
    return this.isReadOnly || this.isOwnerReadOnly;
  }

  toggleSelection(item: MailMockResponse): void {
    if (this.mockSelection.has(item.externalId)) {
      this.mockSelection.delete(item.externalId);
      return;
    }
    this.mockSelection.add(item.externalId);
  }

  isSelected(extId: string): boolean {
    return this.mockSelection.has(extId);
  }

  selectAll(): void {
    this.mockSelection.clear();
    this.mailServices.forEach((item) => this.mockSelection.add(item.externalId));
  }

  clearSelection(): void {
    this.mockSelection.clear();
  }

  filterMocks(): void {
    const filter = this.searchFilter.trim();
    if (!filter) {
      this.mailServices = [...this.allMailServices];
      return;
    }

    this.mailServices = this.allMailServices.filter((item) => item.address.includes(filter));
    this.clearSelection();
  }

  startServer(): void {
    if (this.isReadOnly) {
      return;
    }
    this.isBusy = true;
    this.api.post<{ port: number }>('/mockedserver/mail/start', {}).subscribe({
      next: (data) => {
        this.isBusy = false;
        this.mockServerStatus = this.statusRunning;
        this.showAlert(this.i18n.t('mail.alerts.serverStarted', { port: data.port }), 'success');
        this.loadTableData();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  stopServer(): void {
    if (this.isReadOnly) {
      return;
    }
    this.isBusy = true;
    this.api.post<void>('/mockedserver/mail/stop', {}).subscribe({
      next: () => {
        this.isBusy = false;
        this.mockServerStatus = this.statusStopped;
        this.showAlert(this.i18n.t('mail.alerts.serverStopped'), 'success');
        this.loadTableData();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  exportSelection(): void {
    if (this.mockSelection.size === 0) {
      this.showAlert(this.i18n.t('mail.alerts.exportNoneSelected'));
      return;
    }
    if (!confirm(this.i18n.t('mail.confirm.exportSelected'))) {
      return;
    }

    const ids = Array.from(this.mockSelection);
    this.api.post<string>('/mock/export/MAIL', ids).subscribe({
      next: (data) => {
        downloadBase64File(data, `smockin_mail_export_${ids.length}_mocks.zip`, 'application/zip');
        this.clearSelection();
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  deleteSelection(): void {
    if (this.mockSelection.size === 0) {
      this.showAlert(this.i18n.t('mail.alerts.deleteNoneSelected'));
      return;
    }
    if (!confirm(this.i18n.t('mail.confirm.deleteSelected'))) {
      return;
    }

    this.isBusy = true;
    const deletes = Array.from(this.mockSelection).map((id) => this.api.delete<void>(`/mailmock/${id}`));
    forkJoin(deletes).subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('mail.alerts.deleteSuccess'), 'success');
        this.clearSelection();
        this.loadTableData();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('mail.alerts.deletePartial'));
        this.loadTableData();
      }
    });
  }

  openEditor(extId?: string): void {
    this.viewMode = 'editor';
    this.resetEditor();

    if (!extId) {
      this.isNew = true;
      this.currentExtId = null;
      return;
    }

    this.isNew = false;
    this.currentExtId = extId;
    this.loadEndpoint(extId);
  }

  openNewInbox(): void {
    this.router.navigate([], { queryParams: { new: 'true' }, queryParamsHandling: 'merge' });
  }

  closeEditor(): void {
    this.router.navigate([], { queryParams: { eid: null, new: null }, queryParamsHandling: 'merge' });
    this.viewMode = 'list';
    this.loadTableData();
  }

  toggleImport(): void {
    this.showImport = !this.showImport;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.importFile = input.files?.[0] ?? null;
  }

  importMocks(): void {
    if (!this.importFile) {
      this.showAlert(this.i18n.t('mail.import.selectZip'));
      return;
    }

    const formData = new FormData();
    formData.append('file', this.importFile);
    this.isBusy = true;
    this.api
      .postFormData<{ message: string }>(
        '/mock/import',
        formData,
        new HttpHeaders({ KeepExisting: String(this.keepExisting) })
      )
      .subscribe({
        next: (data) => {
          this.isBusy = false;
          this.showAlert(data.message || this.i18n.t('mail.import.success'), 'success');
          this.importFile = null;
          this.showImport = false;
          this.loadTableData();
        },
        error: () => {
          this.isBusy = false;
          this.showAlert(this.i18n.t('mail.import.failed'));
        }
      });
  }

  onSaveReceivedMailToggle(): void {
    this.showPurgeSavedMailWarning = false;

    if (this.isNew || this.mailMessagesTotal === 0) {
      return;
    }

    if (!this.endpoint.saveReceivedMail && this.currentSaveReceivedMailState) {
      this.showPurgeSavedMailWarning = true;
    }

    if (!this.endpoint.saveReceivedMail) {
      this.showIncludeMailMessagesInSavePrompt = false;
      this.endpoint.retainCachedMail = false;
      return;
    }

    if (!this.showIncludeMailMessagesInSavePrompt && !this.currentSaveReceivedMailState) {
      this.showIncludeMailMessagesInSavePrompt = true;
    }
  }

  saveInbox(): void {
    if (this.isBlank(this.endpoint.address)) {
      this.showAlert(this.i18n.t('mail.errors.addressRequired'));
      return;
    }
    if ((this.endpoint.address || '').length < 10) {
      this.showAlert(this.i18n.t('mail.errors.addressLength'));
      return;
    }
    if (!this.endpoint.address?.includes('@') || !this.endpoint.address?.includes('.')) {
      this.showAlert(this.i18n.t('mail.errors.addressFormat'));
      return;
    }

    const req = {
      address: this.endpoint.address,
      status: this.endpoint.status,
      saveReceivedMail: this.endpoint.saveReceivedMail
    };

    if (this.isNew) {
      this.api.post<void>('/mailmock', req).subscribe({
        next: () => {
          this.showAlert(this.i18n.t('mail.alerts.created'), 'success');
          this.closeEditor();
        },
        error: (err) => {
          if (err?.status === 409) {
            this.showAlert(this.i18n.t('mail.errors.addressExists', { address: this.endpoint.address ?? '' }));
            return;
          }
          if (err?.status === 400) {
            this.showAlert(err.error?.message || this.i18n.t('common.errors.invalidData'));
            return;
          }
          this.showAlert(this.i18n.t('common.errors.generic'));
        }
      });
      return;
    }

    const retainParam = `?retainCachedMail=${this.endpoint.retainCachedMail}`;
    this.api.put<void>(`/mailmock/${this.endpoint.extId}${retainParam}`, req).subscribe({
      next: () => {
        this.showAlert(this.i18n.t('mail.alerts.updated'), 'success');
        this.showIncludeMailMessagesInSavePrompt = false;
        this.currentPageIndex = 0;
        if (this.endpoint.extId) {
          this.loadEndpoint(this.endpoint.extId);
        }
      },
      error: (err) => {
        if (err?.status === 409) {
          this.showAlert(this.i18n.t('mail.errors.addressExists', { address: this.endpoint.address ?? '' }));
          return;
        }
        if (err?.status === 400) {
          this.showAlert(err.error?.message || this.i18n.t('common.errors.invalidData'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  deleteInbox(): void {
    if (!this.endpoint.extId) {
      return;
    }
    if (!confirm(this.i18n.t('mail.confirm.deleteInbox'))) {
      return;
    }
    this.api.delete<void>(`/mailmock/${this.endpoint.extId}`).subscribe({
      next: () => {
        this.showAlert(this.i18n.t('mail.alerts.deleted'), 'success');
        this.closeEditor();
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  reloadMessages(): void {
    if (!this.endpoint.extId) {
      return;
    }
    this.loadEndpoint(this.endpoint.extId);
  }

  filterMessages(): void {
    this.currentPageIndex = 0;
    if (this.endpoint.extId) {
      this.loadEndpoint(this.endpoint.extId);
    }
  }

  selectAllMessages(): void {
    this.messagesSelection = this.mailMessages.map((message) => this.getMessageId(message));
  }

  clearAllMessages(): void {
    this.messagesSelection = [];
  }

  toggleMessageSelection(message: MailMessage): void {
    const messageId = this.getMessageId(message);
    const pos = this.messagesSelection.indexOf(messageId);
    if (pos === -1) {
      this.messagesSelection.push(messageId);
      return;
    }
    this.messagesSelection.splice(pos, 1);
  }

  doesMessagesSelectionContain(message: MailMessage): boolean {
    const messageId = this.getMessageId(message);
    return this.messagesSelection.includes(messageId);
  }

  deleteAllMessages(): void {
    if (!this.endpoint.extId || this.mailMessages.length === 0) {
      return;
    }
    if (!confirm(this.i18n.t('mail.confirm.deleteAllMessages'))) {
      return;
    }

    this.api.delete<void>(`/mailmock/${this.endpoint.extId}/inbox`).subscribe({
      next: () => {
        if (this.mockServerStatus === this.statusRunning) {
          this.api.delete<void>(`/mailmock/${this.endpoint.extId}/server/inbox`).subscribe({
            next: () => this.loadEndpoint(this.endpoint.extId as string),
            error: () => this.showAlert(this.i18n.t('common.errors.generic'))
          });
        } else {
          this.loadEndpoint(this.endpoint.extId as string);
        }
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  deleteSelectedMessages(): void {
    if (!this.endpoint.extId || this.messagesSelection.length === 0) {
      return;
    }
    if (!confirm(this.i18n.t('mail.confirm.deleteSelectedMessages'))) {
      return;
    }

    const deletions = this.messagesSelection.map((id) =>
      this.api.delete<void>(`/mailmock/${this.endpoint.extId}/inbox/${id}`)
    );

    forkJoin(deletions).subscribe({
      next: () => {
        this.loadEndpoint(this.endpoint.extId as string);
        this.showAlert(this.i18n.t('mail.alerts.messagesDeleted'), 'success');
      },
      error: () => {
        this.loadEndpoint(this.endpoint.extId as string);
        this.showAlert(this.i18n.t('mail.alerts.messagesDeletePartial'));
      }
    });
  }

  openPreviousPage(): void {
    if (this.currentPageIndex === 0) {
      return;
    }
    this.currentPageIndex -= 1;
    if (this.endpoint.extId) {
      this.loadEndpoint(this.endpoint.extId);
    }
  }

  openNextPage(): void {
    if (this.currentPageIndex >= this.maxPageIndex) {
      return;
    }
    this.currentPageIndex += 1;
    if (this.endpoint.extId) {
      this.loadEndpoint(this.endpoint.extId);
    }
  }

  openMessage(message: MailMessage): void {
    this.messageModalData = message;
    this.messageModalAttachments = [];
    this.messageModalOpen = true;
    if (!this.endpoint.extId) {
      return;
    }
    this.loadMessageAttachments(this.endpoint.extId, message);
  }

  closeMessageModal(): void {
    this.messageModalOpen = false;
    this.messageModalData = null;
    this.messageModalAttachments = [];
  }

  downloadAttachment(attachment: MailAttachment): void {
    if (!this.endpoint.extId || !this.messageModalData) {
      return;
    }
    const messageId = this.getMessageId(this.messageModalData);
    const attachmentIdOrName = attachment.extId ?? attachment.name;

    this.api
      .get<AttachmentContent>(
        `/mailmock/${this.endpoint.extId}/message/${messageId}/attachment/${encodeURIComponent(attachmentIdOrName)}`
      )
      .subscribe({
        next: (data) => {
          downloadBase64File(data.base64Content, data.name, data.mimeType);
        },
        error: () => this.showAlert(this.i18n.t('common.errors.generic'))
      });
  }

  private loadEndpoint(extId: string): void {
    const query = this.buildSearchParams();
    this.api.get<MailMockDetail>(`/mailmock/${extId}${query}`).subscribe({
      next: (data) => {
        this.endpoint = {
          extId: data.externalId,
          address: data.address,
          status: data.status,
          saveReceivedMail: data.saveReceivedMail,
          retainCachedMail: false,
          createdBy: null
        };

        this.currentSaveReceivedMailState = data.saveReceivedMail;
        this.showIncludeMailMessagesInSavePrompt = false;
        this.showPurgeSavedMailWarning = false;
        this.messagesSelection = [];

        if (data.saveReceivedMail) {
          this.setMessagesFromPage(data.messages);
          this.loadServerStatus();
          return;
        }

        this.loadServerStatus(() => {
          this.loadInboxMessages(extId);
        });
      },
      error: () => {
        this.showAlert(this.i18n.t('mail.errors.loadInbox'));
      }
    });
  }

  private setMessagesFromPage(page?: MailMessagePage): void {
    if (!page) {
      this.mailMessages = [];
      this.mailMessagesTotal = 0;
      this.recordsPerPage = 0;
      this.maxPageIndex = 0;
      return;
    }

    this.mailMessages = page.pageData || [];
    this.mailMessagesTotal = page.totalRecords || 0;
    this.recordsPerPage = page.recordsPerPage || 0;
    this.maxPageIndex = this.recordsPerPage
      ? Math.max(0, Math.ceil(this.mailMessagesTotal / this.recordsPerPage) - 1)
      : 0;
  }

  private loadInboxMessages(extId: string): void {
    this.mailMessagesTotal = 0;
    this.mailMessages = [];

    if (this.isNew || this.mockServerStatus === this.statusStopped) {
      return;
    }

    const query = this.buildSearchParams();
    this.api.get<MailMessagePage>(`/mailmock/${extId}/inbox${query}`).subscribe({
      next: (data) => {
        this.setMessagesFromPage(data);
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private loadMessageAttachments(extId: string, message: MailMessage): void {
    const messageId = this.getMessageId(message);
    this.messageModalLoading = true;
    this.api.get<MailAttachment[]>(`/mailmock/${extId}/message/${messageId}/attachments`).subscribe({
      next: (data) => {
        this.messageModalLoading = false;
        this.messageModalAttachments = data ?? [];
      },
      error: () => {
        this.messageModalLoading = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  private buildSearchParams(): string {
    const base = `?pageStart=${this.currentPageIndex}`;
    if (!this.mailMessageSearch.trim()) {
      return base;
    }
    const search = JSON.stringify({ sender: null, subject: this.mailMessageSearch.trim(), dateReceived: null });
    return `${base}&search=${encodeURIComponent(search)}`;
  }

  private getMessageId(message: MailMessage): string {
    return message.extId ?? message.cacheID ?? '';
  }

  private defaultEndpoint(): MailEndpointForm {
    return {
      extId: null,
      address: null,
      status: ACTIVE_STATUS,
      saveReceivedMail: true,
      retainCachedMail: false,
      createdBy: null
    };
  }

  private resetEditor(): void {
    this.endpoint = this.defaultEndpoint();
    this.currentPageIndex = 0;
    this.maxPageIndex = 0;
    this.recordsPerPage = 0;
    this.mailMessagesTotal = 0;
    this.mailMessages = [];
    this.messagesSelection = [];
    this.mailMessageSearch = '';
    this.showIncludeMailMessagesInSavePrompt = false;
    this.showPurgeSavedMailWarning = false;
    this.currentSaveReceivedMailState = false;
  }

  private isBlank(value: string | null | undefined): boolean {
    return !value || value.trim().length === 0;
  }

  private loadTableData(): void {
    this.api.get<MailMockResponse[]>('/mailmock').subscribe({
      next: (data) => {
        this.allMailServices = data ?? [];
        this.mailServices = [...this.allMailServices];
      },
      error: (err) => {
        if (err?.status === 401) {
          this.showAlert(this.i18n.t('common.errors.loginRequired'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  private loadServerStatus(callback?: () => void): void {
    this.api.get<ServerStatusResponse>('/mockedserver/mail/status').subscribe({
      next: (data) => {
        this.mockServerStatus = data.running ? this.statusRunning : this.statusStopped;
        if (callback) {
          callback();
        }
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
