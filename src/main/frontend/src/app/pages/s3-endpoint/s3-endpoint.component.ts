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
import { Alert, S3BucketDetail, S3BucketResponse, S3File, S3Node, ServerStatusResponse } from '../../core/models';
import { downloadBase64File } from '../../core/file-utils';
import { ACTIVE_STATUS, INACTIVE_STATUS, SYNC_MODES } from '../../core/constants';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

interface S3BucketForm {
  extId: string | null;
  bucket: string | null;
  status: string;
  syncMode: string;
  createdBy?: string | null;
  children: S3Node[];
  files: S3File[];
}

interface SelectedNode {
  type: 'BUCKET' | 'DIR' | 'FILE';
  extId: string;
  name: string;
}

@Component({
  selector: 'app-s3-endpoint',
  standalone: true,
  imports: [AlertsComponent, DatePipe, FormsModule, I18nPipe, ModalComponent, NgFor, NgIf],
  templateUrl: './s3-endpoint.component.html',
  styleUrls: ['./s3-endpoint.component.css']
})
export class S3EndpointComponent implements OnInit {
  alerts: Alert[] = [];
  mockServerStatus = 'Stopped';
  s3Services: S3BucketResponse[] = [];
  allS3Services: S3BucketResponse[] = [];
  mockSelection = new Set<string>();
  searchFilter = '';
  isBusy = false;
  showImport = false;
  keepExisting = false;
  importFile: File | null = null;

  viewMode: 'list' | 'editor' = 'list';
  isNew = false;
  currentExtId: string | null = null;
  endpoint: S3BucketForm = this.defaultEndpoint();
  rootExpanded = true;
  selectedNode: SelectedNode | null = null;

  nodeModalOpen = false;
  nodeModalTitleKey = '';
  nodeModalName = '';
  nodeModalParentName: string | null = null;
  nodeModalMode: 'add' | 'rename' = 'add';
  nodeModalTarget: SelectedNode | null = null;

  uploadModalOpen = false;
  uploadFile: File | null = null;
  uploadTarget: SelectedNode | null = null;

  readonly statusRunning = 'Running';
  readonly statusStopped = 'Stopped';
  readonly activeStatus = ACTIVE_STATUS;
  readonly inactiveStatus = INACTIVE_STATUS;
  readonly syncModeOptions = [
    { value: SYNC_MODES.none, labelKey: 's3.sync.none', descriptionKey: 's3.sync.noneDescription' },
    { value: SYNC_MODES.oneWay, labelKey: 's3.sync.oneWay', descriptionKey: 's3.sync.oneWayDescription' },
    { value: SYNC_MODES.biDirectional, labelKey: 's3.sync.biDirectional', descriptionKey: 's3.sync.biDirectionalDescription' }
  ];

  private readonly s3BucketNameRegex = /(?=^.{3,63}$)(?!^(\d+\.)+\d+$)(^(([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])\.)*([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])$)/;

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

  get canAddDir(): boolean {
    return !!this.selectedNode && (this.selectedNode.type === 'BUCKET' || this.selectedNode.type === 'DIR');
  }

  get canRenameNode(): boolean {
    return !!this.selectedNode && (this.selectedNode.type === 'BUCKET' || this.selectedNode.type === 'DIR');
  }

  get canRemoveNode(): boolean {
    return !!this.selectedNode && this.selectedNode.type !== 'BUCKET';
  }

  get canUploadFile(): boolean {
    return !!this.selectedNode && (this.selectedNode.type === 'BUCKET' || this.selectedNode.type === 'DIR');
  }

  translateSyncMode(syncMode: string): string {
    const option = this.syncModeOptions.find((item) => item.value === syncMode);
    return option ? this.i18n.t(option.labelKey) : syncMode;
  }

  toggleSelection(item: S3BucketResponse): void {
    if (this.mockSelection.has(item.extId)) {
      this.mockSelection.delete(item.extId);
      return;
    }
    this.mockSelection.add(item.extId);
  }

  isSelected(extId: string): boolean {
    return this.mockSelection.has(extId);
  }

  selectAll(): void {
    this.mockSelection.clear();
    this.s3Services.forEach((item) => this.mockSelection.add(item.extId));
  }

  clearSelection(): void {
    this.mockSelection.clear();
  }

  filterMocks(): void {
    const filter = this.searchFilter.trim();
    if (!filter) {
      this.s3Services = [...this.allS3Services];
      return;
    }

    this.s3Services = this.allS3Services.filter((item) => item.bucket.includes(filter));
    this.clearSelection();
  }

  startServer(): void {
    if (this.isReadOnly) {
      return;
    }
    this.isBusy = true;
    this.api.post<{ port: number }>('/mockedserver/s3/start', {}).subscribe({
      next: (data) => {
        this.isBusy = false;
        this.mockServerStatus = this.statusRunning;
        this.showAlert(this.i18n.t('s3.alerts.serverStarted', { port: data.port }), 'success');
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
    this.api.post<void>('/mockedserver/s3/stop', {}).subscribe({
      next: () => {
        this.isBusy = false;
        this.mockServerStatus = this.statusStopped;
        this.showAlert(this.i18n.t('s3.alerts.serverStopped'), 'success');
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
      this.showAlert(this.i18n.t('s3.alerts.exportNoneSelected'));
      return;
    }
    if (!confirm(this.i18n.t('s3.confirm.exportSelected'))) {
      return;
    }

    const ids = Array.from(this.mockSelection);
    this.api.post<string>('/mock/export/S3', ids).subscribe({
      next: (data) => {
        downloadBase64File(data, `smockin_S3_export_${ids.length}_mocks.zip`, 'application/zip');
        this.clearSelection();
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  deleteSelection(): void {
    if (this.mockSelection.size === 0) {
      this.showAlert(this.i18n.t('s3.alerts.deleteNoneSelected'));
      return;
    }
    if (!confirm(this.i18n.t('s3.confirm.deleteSelected'))) {
      return;
    }

    this.isBusy = true;
    const deletes = Array.from(this.mockSelection).map((id) => this.api.delete<void>(`/s3mock/bucket/${id}`));
    forkJoin(deletes).subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('s3.alerts.deleteSuccess'), 'success');
        this.clearSelection();
        this.loadTableData();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('s3.alerts.deletePartial'));
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

  openNewBucket(): void {
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
      this.showAlert(this.i18n.t('s3.import.selectZip'));
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
          this.showAlert(data.message || this.i18n.t('s3.import.success'), 'success');
          this.importFile = null;
          this.showImport = false;
          this.loadTableData();
        },
        error: () => {
          this.isBusy = false;
          this.showAlert(this.i18n.t('s3.import.failed'));
        }
      });
  }

  selectBucket(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (!this.endpoint.extId) {
      return;
    }
    this.selectedNode = {
      type: 'BUCKET',
      extId: this.endpoint.extId,
      name: this.endpoint.bucket || ''
    };
  }

  selectDir(node: S3Node, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.selectedNode = {
      type: 'DIR',
      extId: node.extId,
      name: node.name
    };
  }

  selectFile(file: S3File, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.selectedNode = {
      type: 'FILE',
      extId: file.extId,
      name: file.name
    };
  }

  isSelectedNode(extId: string, type: SelectedNode['type']): boolean {
    return !!this.selectedNode && this.selectedNode.extId === extId && this.selectedNode.type === type;
  }

  toggleRoot(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.rootExpanded = !this.rootExpanded;
  }

  toggleNode(node: S3Node, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    node.expanded = !node.expanded;
  }

  getNodeItemCount(node: S3Node): number {
    return (node.children?.length || 0) + (node.files?.length || 0);
  }

  openAddDirModal(): void {
    if (!this.canAddDir) {
      this.showAlert(this.i18n.t('s3.alerts.selectDir'));
      return;
    }
    this.nodeModalMode = 'add';
    this.nodeModalTitleKey = 's3.modal.addDirTitle';
    this.nodeModalName = '';
    this.nodeModalParentName = this.selectedNode?.name ?? null;
    this.nodeModalTarget = this.selectedNode;
    this.nodeModalOpen = true;
  }

  openRenameModal(): void {
    if (!this.canRenameNode) {
      this.showAlert(this.i18n.t('s3.alerts.selectRenameTarget'));
      return;
    }
    const target = this.selectedNode;
    if (!target) {
      return;
    }
    this.nodeModalMode = 'rename';
    this.nodeModalTitleKey = target.type === 'BUCKET' ? 's3.modal.renameBucketTitle' : 's3.modal.renameDirTitle';
    this.nodeModalName = target.name;
    this.nodeModalParentName = null;
    this.nodeModalTarget = target;
    this.nodeModalOpen = true;
  }

  closeNodeModal(): void {
    this.nodeModalOpen = false;
    this.nodeModalTarget = null;
  }

  saveNodeModal(): void {
    if (this.isBlank(this.nodeModalName)) {
      this.showAlert(this.i18n.t('common.errors.nameRequired'));
      return;
    }

    const target = this.nodeModalTarget;
    if (!target) {
      return;
    }

    if (this.nodeModalMode === 'add') {
      const req = {
        name: this.nodeModalName,
        bucketExtId: target.type === 'BUCKET' ? target.extId : null,
        parentDirExtId: target.type === 'DIR' ? target.extId : null
      };
      this.api.post<{ message: string }>('/s3mock/dir', req).subscribe({
        next: (data) => {
          const newId = data.message;
          this.appendNodeChild(target.extId, this.endpoint, this.nodeModalName, newId, 'DIR');
          this.nodeModalOpen = false;
          this.showAlert(this.i18n.t('s3.alerts.dirCreated'), 'success');
        },
        error: (err) => {
          if (err?.status === 400) {
            this.showAlert(err.error?.message || this.i18n.t('s3.alerts.invalidDirName'));
            return;
          }
          this.showAlert(this.i18n.t('common.errors.generic'));
        }
      });
      return;
    }

    if (target.type === 'BUCKET') {
      if (!this.isValidBucketName(this.nodeModalName)) {
        this.showAlert(this.i18n.t('s3.alerts.invalidBucketName'));
        return;
      }
      const req = {
        bucket: this.nodeModalName,
        status: this.endpoint.status,
        syncMode: this.endpoint.syncMode
      };
      this.api.put<void>(`/s3mock/bucket/${target.extId}`, req).subscribe({
        next: () => {
          this.endpoint.bucket = this.nodeModalName;
          if (this.selectedNode?.extId === target.extId) {
            this.selectedNode = { ...this.selectedNode, name: this.nodeModalName };
          }
          this.nodeModalOpen = false;
          this.showAlert(this.i18n.t('s3.alerts.bucketUpdated'), 'success');
        },
        error: (err) => {
          if (err?.status === 409) {
            this.showAlert(this.i18n.t('s3.alerts.bucketExists', { name: this.nodeModalName }));
            return;
          }
          this.showAlert(this.i18n.t('common.errors.generic'));
        }
      });
      return;
    }

    const req = {
      name: this.nodeModalName,
      bucketExtId: null,
      parentDirExtId: null
    };
    this.api.put<void>(`/s3mock/dir/${target.extId}`, req).subscribe({
      next: () => {
        this.renameNodeChild(target.extId, this.endpoint, this.nodeModalName);
        if (this.selectedNode?.extId === target.extId) {
          this.selectedNode = { ...this.selectedNode, name: this.nodeModalName };
        }
        this.nodeModalOpen = false;
        this.showAlert(this.i18n.t('s3.alerts.dirUpdated'), 'success');
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  openUploadModal(): void {
    if (!this.canUploadFile) {
      this.showAlert(this.i18n.t('s3.alerts.selectUploadDir'));
      return;
    }
    this.uploadModalOpen = true;
    this.uploadFile = null;
    this.uploadTarget = this.selectedNode;
  }

  onUploadFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.uploadFile = input.files?.[0] ?? null;
  }

  closeUploadModal(): void {
    this.uploadModalOpen = false;
    this.uploadFile = null;
    this.uploadTarget = null;
  }

  uploadSelectedFile(): void {
    if (!this.uploadFile) {
      this.showAlert(this.i18n.t('s3.alerts.selectUploadFile'));
      return;
    }
    const target = this.uploadTarget;
    if (!target) {
      return;
    }

    const formData = new FormData();
    formData.append('file', this.uploadFile);

    const targetType = target.type === 'BUCKET' ? 'bucket' : 'dir';
    this.api.postFormData<{ message: string }>(`/s3mock/${targetType}/${target.extId}/upload`, formData).subscribe({
      next: (data) => {
        const newId = data.message;
        this.appendNodeChild(target.extId, this.endpoint, this.uploadFile?.name || 'file', newId, 'FILE');
        this.uploadModalOpen = false;
        this.uploadFile = null;
        this.showAlert(this.i18n.t('s3.alerts.fileUploaded'), 'success');
      },
      error: (err: any) => {
        if (err?.status === 400) {
          this.showAlert(this.i18n.t('s3.alerts.importFileIssue'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  removeSelectedNode(): void {
    if (!this.canRemoveNode || !this.selectedNode) {
      if (this.selectedNode?.type === 'BUCKET') {
        this.showAlert(this.i18n.t('s3.alerts.deleteUseMainButton'));
      }
      return;
    }

    const node = this.selectedNode;
    const label =
      node.type === 'DIR' ? this.i18n.t('s3.confirm.deleteNode.dir') : this.i18n.t('s3.confirm.deleteNode.file');
    if (!confirm(this.i18n.t('s3.confirm.deleteNode.message', { target: label }))) {
      return;
    }

    const nodeType = node.type.toLowerCase();
    this.api.delete<void>(`/s3mock/${nodeType}/${node.extId}`).subscribe({
      next: () => {
        this.removeNodeChild(node.extId, this.endpoint, node.type);
        this.selectedNode = null;
        this.showAlert(this.i18n.t('s3.alerts.nodeDeleted'), 'success');
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  resetBucketOnServer(): void {
    if (!this.endpoint.extId || this.mockServerStatus !== this.statusRunning) {
      return;
    }
    if (!confirm(this.i18n.t('s3.confirm.resetBucket'))) {
      return;
    }

    const req = { bucketExtId: this.endpoint.extId };
    this.api.post<void>(`/s3mock/bucket/${this.endpoint.extId}/resynchronize`, req).subscribe({
      next: () => this.showAlert(this.i18n.t('s3.alerts.bucketSynced'), 'success'),
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  saveBucket(): void {
    if (this.isBlank(this.endpoint.bucket)) {
      this.showAlert(this.i18n.t('s3.alerts.bucketRequired'));
      return;
    }
    if (!this.isValidBucketName(this.endpoint.bucket || '')) {
      this.showAlert(this.i18n.t('s3.alerts.invalidBucketName'));
      return;
    }

    const req = {
      bucket: this.endpoint.bucket,
      status: this.endpoint.status,
      syncMode: this.endpoint.syncMode
    };

    if (this.isNew) {
      this.api.post<{ message: string }>('/s3mock/bucket', req).subscribe({
        next: () => {
          this.showAlert(this.i18n.t('s3.alerts.bucketCreated'), 'success');
          this.closeEditor();
        },
        error: (err: any) => {
          if (err?.status === 409) {
            this.showAlert(this.i18n.t('s3.alerts.bucketExists', { name: this.endpoint.bucket ?? '' }));
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

    this.api.put<void>(`/s3mock/bucket/${this.endpoint.extId}`, req).subscribe({
      next: () => {
        this.showAlert(this.i18n.t('s3.alerts.bucketUpdated'), 'success');
        this.closeEditor();
      },
      error: (err: any) => {
        if (err?.status === 409) {
          this.showAlert(this.i18n.t('s3.alerts.bucketExists', { name: this.endpoint.bucket ?? '' }));
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

  deleteBucket(): void {
    if (!this.endpoint.extId) {
      return;
    }
    if (!confirm(this.i18n.t('s3.confirm.deleteBucket'))) {
      return;
    }
    this.api.delete<void>(`/s3mock/bucket/${this.endpoint.extId}`).subscribe({
      next: () => {
        this.showAlert(this.i18n.t('s3.alerts.bucketDeleted'), 'success');
        this.closeEditor();
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private loadEndpoint(extId: string): void {
    this.api.get<S3BucketDetail>(`/s3mock/bucket/${extId}`).subscribe({
      next: (data) => {
        this.endpoint = this.mapEndpoint(data);
        this.currentExtId = data.extId;
        this.rootExpanded = true;
        this.selectBucket();
      },
      error: () => {
        this.showAlert(this.i18n.t('s3.alerts.loadBucketFailed'));
      }
    });
  }

  private mapEndpoint(data: S3BucketDetail): S3BucketForm {
    return {
      extId: data.extId,
      bucket: data.bucket,
      status: data.status,
      syncMode: data.syncMode,
      createdBy: data.createdBy,
      children: this.normalizeNodes(data.children || []),
      files: data.files || []
    };
  }

  private normalizeNodes(nodes: S3Node[]): S3Node[] {
    return (nodes || []).map((node) => ({
      ...node,
      expanded: node.expanded ?? false,
      children: this.normalizeNodes(node.children || []),
      files: node.files || []
    }));
  }

  private appendNodeChild(
    extIdToMatch: string,
    endpoint: S3BucketForm | S3Node,
    newNodeName: string,
    newNodeExtId: string,
    nodeType: 'DIR' | 'FILE'
  ): void {
    if (extIdToMatch === endpoint.extId) {
      if ('bucket' in endpoint) {
        this.rootExpanded = true;
      } else {
        endpoint.expanded = true;
      }
      if (nodeType === 'DIR') {
        endpoint.children.push({
          extId: newNodeExtId,
          name: newNodeName,
          children: [],
          files: [],
          expanded: false
        });
      } else {
        endpoint.files.push({
          extId: newNodeExtId,
          name: newNodeName
        });
      }
      return;
    }

    for (const child of endpoint.children) {
      this.appendNodeChild(extIdToMatch, child, newNodeName, newNodeExtId, nodeType);
    }
  }

  private removeNodeChild(extIdToMatch: string, endpoint: S3BucketForm | S3Node, nodeType: SelectedNode['type']): void {
    if (nodeType === 'DIR') {
      const idx = endpoint.children.findIndex((child) => child.extId === extIdToMatch);
      if (idx >= 0) {
        endpoint.children.splice(idx, 1);
        return;
      }
    }

    if (nodeType === 'FILE') {
      const idx = endpoint.files.findIndex((file) => file.extId === extIdToMatch);
      if (idx >= 0) {
        endpoint.files.splice(idx, 1);
        return;
      }
    }

    for (const child of endpoint.children) {
      this.removeNodeChild(extIdToMatch, child, nodeType);
    }
  }

  private renameNodeChild(extIdToMatch: string, endpoint: S3BucketForm | S3Node, newName: string): void {
    for (const child of endpoint.children) {
      if (child.extId === extIdToMatch) {
        child.name = newName;
        return;
      }
      this.renameNodeChild(extIdToMatch, child, newName);
    }
  }

  private defaultEndpoint(): S3BucketForm {
    return {
      extId: null,
      bucket: null,
      status: ACTIVE_STATUS,
      syncMode: SYNC_MODES.none,
      createdBy: null,
      children: [],
      files: []
    };
  }

  private resetEditor(): void {
    this.endpoint = this.defaultEndpoint();
    this.selectedNode = null;
    this.rootExpanded = true;
  }

  private isBlank(value: string | null | undefined): boolean {
    return !value || value.trim().length === 0;
  }

  private isValidBucketName(value: string): boolean {
    return this.s3BucketNameRegex.test(value);
  }

  private loadTableData(): void {
    this.api.get<S3BucketResponse[]>('/s3mock/bucket').subscribe({
      next: (data) => {
        this.allS3Services = data ?? [];
        this.s3Services = [...this.allS3Services];
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

  private loadServerStatus(): void {
    this.api.get<ServerStatusResponse>('/mockedserver/s3/status').subscribe({
      next: (data) => {
        this.mockServerStatus = data.running ? this.statusRunning : this.statusStopped;
      },
      error: () => this.showAlert(this.i18n.t('common.errors.generic'))
    });
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
